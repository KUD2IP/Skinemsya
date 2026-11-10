package skinemsya.vse.ru.integrations.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Поправляет разбор чека там, где арифметика надёжнее модели:
 * скидка под блюдом вычитается из него, литры розлива становятся числом стаканов.
 */
public final class ReceiptLineReconciler {

    private static final Pattern SERVING_ML = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*мл", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISCOUNT_NAME =
            Pattern.compile("скидк", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NOISE_NAME = Pattern.compile("^[\\s%(),.0-9]+$");
    private static final BigDecimal PIECE_TOLERANCE = new BigDecimal("0.05");
    private static final double LITER_PRICE_HINT = 900;
    private static final double MONEY_TOLERANCE = 0.5;

    private ReceiptLineReconciler() {}

    public static MlReceiptResponse reconcile(MlReceiptResponse response) {
        if (response == null || response.items() == null || response.items().isEmpty()) {
            return response;
        }
        List<PricedLine> lines = new ArrayList<>();
        for (MlReceiptItem item : response.items()) {
            if (item.name() == null || item.name().isBlank() || isNoise(item)) {
                continue;
            }
            if (isDiscount(item)) {
                if (!lines.isEmpty()) {
                    lines.get(lines.size() - 1).discounts().add(Math.abs(item.totalPrice()));
                }
                continue;
            }
            lines.add(new PricedLine(item, new ArrayList<>()));
        }
        boolean subtractDiscounts = discountsImproveTotal(response.total(), lines);
        List<MlReceiptItem> items = new ArrayList<>();
        for (PricedLine line : lines) {
            MlReceiptItem item = line.item();
            if (subtractDiscounts) {
                item = withTotal(item, item.totalPrice() - discountStillDue(item, line.discounts()));
            }
            items.add(toServings(item));
        }
        return new MlReceiptResponse(
                response.merchant(),
                response.date(),
                response.total(),
                response.currency(),
                List.copyOf(items),
                response.confidence(),
                response.errors());
    }

    private record PricedLine(MlReceiptItem item, List<Double> discounts) {}

    /**
     * Скидку вычитаем, только если так сумма строк ближе к итогу к оплате.
     * Если цены уже после скидки, а строка скидки просто стоит рядом, вычитание их только портит.
     */
    private static boolean discountsImproveTotal(Double payable, List<PricedLine> lines) {
        if (lines.stream().allMatch(line -> line.discounts().isEmpty())) {
            return false;
        }
        if (payable == null || payable <= 0) {
            return true;
        }
        double gross = 0;
        double net = 0;
        for (PricedLine line : lines) {
            gross += line.item().totalPrice();
            net += Math.max(0, line.item().totalPrice() - discountStillDue(line.item(), line.discounts()));
        }
        return Math.abs(net - payable) + MONEY_TOLERANCE < Math.abs(gross - payable);
    }

    private static double discountStillDue(MlReceiptItem item, List<Double> discounts) {
        if (discounts.isEmpty()) {
            return 0;
        }
        double discount = discounts.stream().mapToDouble(Double::doubleValue).sum();
        if (item.unitPrice() == null || item.quantity() <= 0) {
            return discount;
        }
        double alreadyOff = Math.max(0, item.unitPrice() * item.quantity() - item.totalPrice());
        return Math.max(0, discount - alreadyOff);
    }

    private static boolean isDiscount(MlReceiptItem item) {
        return item.totalPrice() < 0 || DISCOUNT_NAME.matcher(item.name()).find();
    }

    private static boolean isNoise(MlReceiptItem item) {
        return NOISE_NAME.matcher(item.name().trim()).matches() && Math.abs(item.totalPrice()) < 0.01;
    }

    private static void applyDiscount(List<MlReceiptItem> items, double discount) {
        if (items.isEmpty() || discount <= 0) {
            return;
        }
        MlReceiptItem previous = items.get(items.size() - 1);
        items.set(items.size() - 1, withTotal(previous, previous.totalPrice() - discount));
    }

    private static MlReceiptItem toServings(MlReceiptItem item) {
        String name = item.name().toLowerCase();
        if (!name.contains("розлив")) {
            return item;
        }
        Matcher volume = SERVING_ML.matcher(item.name());
        if (!volume.find()) {
            return item;
        }
        BigDecimal milliliters = new BigDecimal(volume.group(1).replace(',', '.'));
        if (milliliters.compareTo(BigDecimal.valueOf(50)) < 0 || milliliters.compareTo(BigDecimal.valueOf(2000)) > 0) {
            return item;
        }
        BigDecimal servingLiters = milliliters.movePointLeft(3);
        BigDecimal quantity = BigDecimal.valueOf(item.quantity());
        BigDecimal pieces = quantity.divide(servingLiters, 4, RoundingMode.HALF_UP);
        BigDecimal nearest = pieces.setScale(0, RoundingMode.HALF_UP);
        boolean wholeServings =
                pieces.subtract(nearest).abs().compareTo(PIECE_TOLERANCE) <= 0 && nearest.intValue() >= 1;
        boolean printedAsLiters = quantity.stripTrailingZeros().scale() > 0
                || (item.unitPrice() != null && item.unitPrice() >= LITER_PRICE_HINT);
        if (wholeServings && printedAsLiters && nearest.compareTo(quantity) > 0) {
            return withQuantity(item, nearest.doubleValue());
        }
        if (quantity.compareTo(BigDecimal.ONE) < 0 && quantity.compareTo(BigDecimal.ZERO) > 0) {
            return withQuantity(item, 1);
        }
        return item;
    }

    private static MlReceiptItem withQuantity(MlReceiptItem item, double quantity) {
        Double unit = item.unitPrice();
        if (quantity > 0) {
            unit = pricePerUnit(item.totalPrice(), quantity);
        }
        return new MlReceiptItem(item.name(), quantity, unit, item.totalPrice(), item.confidence());
    }

    private static MlReceiptItem withTotal(MlReceiptItem item, double total) {
        double safeTotal = Math.max(0, total);
        Double unit = item.unitPrice();
        if (item.quantity() > 0) {
            unit = pricePerUnit(safeTotal, item.quantity());
        }
        return new MlReceiptItem(item.name(), item.quantity(), unit, safeTotal, item.confidence());
    }

    private static Double pricePerUnit(double total, double quantity) {
        return Double.valueOf(total / quantity);
    }
}
