package skinemsya.vse.ru.integrations.infrastructure.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import skinemsya.vse.ru.integrations.domain.MlReceiptItem;
import skinemsya.vse.ru.integrations.domain.MlReceiptResponse;
import skinemsya.vse.ru.integrations.domain.ReceiptLineReconciler;

/** Достаёт JSON чека из ответа OpenRouter и решает, нужен ли повторный запрос. */
public final class OpenRouterReceiptParser {

    static final String PROMPT =
            """
            Ты читаешь фото российского чека: пречек ресторана или кассовый чек.
            Верни строго один JSON без markdown и пояснений.
            Схема:
            {"merchant": string|null, "date": "YYYY-MM-DD"|null, "total": number|null, "currency": "RUB",
             "items": [{"name": string, "quantity": number, "unit_price": number|null, "total_price": number, "confidence": number}],
             "confidence": number, "errors": []}
            Правила:
            - В items только блюда, напитки и товары. Чаевые и сервисный сбор — отдельная позиция.
            - Не включай НДС, фискальный подвал, способ оплаты, сдачу, «итого», «полная сумма» и «подытог».
            - total — сумма к оплате («ИТОГ» или «ИТОГО К ОПЛАТЕ»), не полная сумма до скидки и не зачёркнутый промежуточный итог.
            - Скидка строкой под блюдом относится к этому блюду. total_price — сумма строки уже после скидки. Саму скидку в items не клади.
            - quantity копируй как напечатано, не пересчитывай стаканы. Для розлива это часто литры: 3.2 при стакане 400 мл.
            - Если название перенесено на следующую строку, склей его в одно.
            - quantity > 0. Числа с точкой, без валюты и пробелов.
            - Не выдумывай позиции, которых не видно. confidence от 0 до 1.
            """;

    static final String RETRY_NOTE =
            """
            Предыдущий разбор был пустым или сумма строк сильно разошлась с итогом к оплате.
            Скидку под блюдом вычти из этого блюда и не добавляй её отдельной строкой.
            total — «итого к оплате», не полная сумма до скидки.
            Для розлива оставь напечатанное количество, даже если это литры вроде 3.2.
            """;

    private static final double RETRY_SUM_GAP = 0.15;

    private OpenRouterReceiptParser() {}

    public static boolean shouldRetry(MlReceiptResponse response) {
        if (response == null || response.items() == null || response.items().isEmpty()) {
            return true;
        }
        if (response.total() == null || response.total() <= 0) {
            return false;
        }
        double sum = 0;
        for (MlReceiptItem item : response.items()) {
            sum += item.totalPrice();
        }
        return Math.abs(sum - response.total()) / response.total() > RETRY_SUM_GAP;
    }

    public static MlReceiptResponse parseContent(String content, ObjectMapper objectMapper) {
        if (content == null || content.isBlank()) {
            return unreadable();
        }
        String json = unwrapJson(content);
        try {
            return ReceiptLineReconciler.reconcile(fromTree(objectMapper.readTree(json)));
        } catch (Exception ex) {
            return unreadable();
        }
    }

    public static String messageContent(JsonNode message) {
        JsonNode content = message.path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        if (!content.isArray()) {
            return "";
        }
        var text = new StringBuilder();
        for (JsonNode part : content) {
            if (part.isTextual()) {
                text.append(part.asText());
            } else {
                text.append(part.path("text").asText(""));
            }
        }
        return text.toString();
    }

    static MlReceiptResponse unreadable() {
        return new MlReceiptResponse(null, null, null, "RUB", List.of(), 0.0, List.of("unreadable"));
    }

    private static MlReceiptResponse fromTree(JsonNode root) {
        if (root == null || !root.isObject()) {
            return unreadable();
        }
        List<MlReceiptItem> items = new ArrayList<>();
        JsonNode rawItems = root.path("items");
        if (rawItems.isArray()) {
            for (JsonNode item : rawItems) {
                String name = text(item, "name");
                Double quantity = number(item, "quantity");
                Double totalPrice = number(item, "total_price");
                if (name == null || quantity == null || totalPrice == null) {
                    continue;
                }
                items.add(new MlReceiptItem(
                        name, quantity, number(item, "unit_price"), totalPrice, clamp(number(item, "confidence"))));
            }
        }
        List<String> errors = new ArrayList<>();
        JsonNode rawErrors = root.path("errors");
        if (rawErrors.isArray()) {
            for (JsonNode error : rawErrors) {
                if (error.isTextual() && !error.asText().isBlank()) {
                    errors.add(error.asText());
                }
            }
        }
        return new MlReceiptResponse(
                text(root, "merchant"),
                text(root, "date"),
                number(root, "total"),
                text(root, "currency") == null ? "RUB" : text(root, "currency"),
                items,
                clamp(number(root, "confidence")),
                errors);
    }

    private static String unwrapJson(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
            return trimmed;
        }
        int firstBreak = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstBreak >= 0 && lastFence > firstBreak) {
            return unwrapJson(trimmed.substring(firstBreak + 1, lastFence));
        }
        return trimmed;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText("").trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (!value.isTextual()) {
            return null;
        }
        String raw = value.asText().trim().replace(" ", "").replace(',', '.');
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Double clamp(Double value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            return 0.0;
        }
        if (value > 1) {
            return 1.0;
        }
        return value;
    }
}
