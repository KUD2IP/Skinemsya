package skinemsya.vse.ru.integrations.infrastructure.ml;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import skinemsya.vse.ru.integrations.domain.MlReceiptItem;
import skinemsya.vse.ru.integrations.domain.MlReceiptResponse;

class OpenRouterReceiptParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesFencedJsonAndStringNumbers() {
        String content =
                """
                ```json
                {"merchant":"Кафе","date":"2026-11-08","total":"1 250,50","currency":"RUB",
                 "items":[{"name":"Борщ","quantity":"1","unit_price":"450","total_price":"450,00","confidence":"0.8"}],
                 "confidence":0.9,"errors":[]}
                ```
                """;

        MlReceiptResponse response = OpenRouterReceiptParser.parseContent(content, objectMapper);

        assertThat(response.merchant()).isEqualTo("Кафе");
        assertThat(response.total()).isEqualTo(1250.50);
        assertThat(response.items()).hasSize(1);
        MlReceiptItem item = response.items().getFirst();
        assertThat(item.name()).isEqualTo("Борщ");
        assertThat(item.totalPrice()).isEqualTo(450.0);
        assertThat(OpenRouterReceiptParser.shouldRetry(response)).isTrue();
    }

    @Test
    void doesNotRetryWhenSumMatches() {
        MlReceiptResponse response = OpenRouterReceiptParser.parseContent(
                """
                {"total":100,"items":[{"name":"Чай","quantity":1,"total_price":100,"confidence":0.95}],"confidence":0.95}
                """,
                objectMapper);

        assertThat(OpenRouterReceiptParser.shouldRetry(response)).isFalse();
    }

    @Test
    void retriesEmptyAndUnreadableContent() {
        assertThat(OpenRouterReceiptParser.shouldRetry(OpenRouterReceiptParser.parseContent("не json", objectMapper)))
                .isTrue();
        assertThat(OpenRouterReceiptParser.shouldRetry(OpenRouterReceiptParser.parseContent("", objectMapper)))
                .isTrue();
    }

    @Test
    void convertsDraftLitersIntoGlassesWithoutChangingTheLineTotal() {
        MlReceiptResponse response = OpenRouterReceiptParser.parseContent(
                """
                {"total":3440,"items":[
                  {"name":"Пилснер 400 мл розлив","quantity":3.2,"unit_price":1075,"total_price":3440,"confidence":0.9}
                ],"confidence":0.9}
                """,
                objectMapper);

        MlReceiptItem beer = response.items().getFirst();
        assertThat(beer.quantity()).isEqualTo(8);
        assertThat(beer.totalPrice()).isEqualTo(3440);
        assertThat(OpenRouterReceiptParser.shouldRetry(response)).isFalse();
    }

    @Test
    void keepsDraftQuantityWhenTheModelAlreadyCountedGlasses() {
        MlReceiptResponse response = OpenRouterReceiptParser.parseContent(
                """
                {"total":3440,"items":[
                  {"name":"Пилснер 400 мл розлив","quantity":8,"unit_price":430,"total_price":3440,"confidence":0.9}
                ],"confidence":0.9}
                """,
                objectMapper);

        assertThat(response.items().getFirst().quantity()).isEqualTo(8);
    }

    @Test
    void subtractsDiscountPrintedUnderTheDishFromThatDish() {
        MlReceiptResponse response = OpenRouterReceiptParser.parseContent(
                """
                {"total":244,"items":[
                  {"name":"Белый Русский","quantity":1,"unit_price":305,"total_price":305,"confidence":0.9},
                  {"name":"Скидка в День Рождения 20%","quantity":1,"total_price":-61,"confidence":0.8}
                ],"confidence":0.9}
                """,
                objectMapper);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().name()).isEqualTo("Белый Русский");
        assertThat(response.items().getFirst().totalPrice()).isEqualTo(244);
        assertThat(OpenRouterReceiptParser.shouldRetry(response)).isFalse();
    }

    @Test
    void doesNotSubtractDiscountThatIsAlreadyInsideTheDishPrice() {
        MlReceiptResponse response = OpenRouterReceiptParser.parseContent(
                """
                {"total":244,"items":[
                  {"name":"Белый Русский","quantity":1,"unit_price":305,"total_price":244,"confidence":0.9},
                  {"name":"Скидка в День Рождения 20%","quantity":1,"total_price":-61,"confidence":0.8}
                ],"confidence":0.9}
                """,
                objectMapper);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().totalPrice()).isEqualTo(244);
    }

    @Test
    void dropsNearbyDiscountWhenLineTotalsAlreadyMatchTheAmountDue() {
        MlReceiptResponse response = OpenRouterReceiptParser.parseContent(
                """
                {"total":651,"items":[
                  {"name":"Белый Русский","quantity":1,"total_price":244,"confidence":0.9},
                  {"name":"Скидка в День Рождения 20%","quantity":1,"total_price":-61,"confidence":0.8},
                  {"name":"Мохито","quantity":1,"total_price":407,"confidence":0.9},
                  {"name":"Скидка в День Рождения 20%","quantity":1,"total_price":-102,"confidence":0.8}
                ],"confidence":0.9}
                """,
                objectMapper);

        assertThat(response.items()).extracting(MlReceiptItem::totalPrice).containsExactly(244.0, 407.0);
    }

    @Test
    void readsTextPartsFromArrayContent() {
        ObjectNode message = objectMapper.createObjectNode();
        ArrayNode content = message.putArray("content");
        content.add("{\"total\":10,");
        content.addObject()
                .put("type", "text")
                .put("text", "\"items\":[{\"name\":\"Хлеб\",\"quantity\":1,\"total_price\":10}]}");

        MlReceiptResponse response =
                OpenRouterReceiptParser.parseContent(OpenRouterReceiptParser.messageContent(message), objectMapper);

        assertThat(response.items()).extracting(MlReceiptItem::name).containsExactly("Хлеб");
    }
}
