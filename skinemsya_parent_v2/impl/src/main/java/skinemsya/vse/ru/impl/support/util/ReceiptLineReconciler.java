package skinemsya.vse.ru.impl.support.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import skinemsya.vse.ru.api.internal.pojo.ml.MlReceiptItem;
import skinemsya.vse.ru.api.internal.pojo.ml.MlReceiptResponse;
import lombok.experimental.UtilityClass;

/** Исправляет арифметику распознанных позиций чека. */
@UtilityClass
public class ReceiptLineReconciler {

    private static final Pattern SERVING_ML = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*мл", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISCOUNT_NAME =
            Pattern.compile("скидк", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NOISE_NAME = Pattern.compile("^[\\s%(),.0-9]+$");
    private static final BigDecimal PIECE_TOLERANCE = new BigDecimal("0.05");
    private static final double LITER_PRICE_HINT = 900;
    private static final double MONEY_TOLERANCE = 0.5;


    /** Убирает служебные строки, распределяет скидки и нормализует количество розлива. */
    public static MlReceiptResponse reconcile(MlReceiptResponse response) {
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            return response;
        }
        List<PricedLine> lines = new ArrayList<>();
        for (MlReceiptItem item : response.getItems()) {
            if (item.getName() == null || item.getName().isBlank() || isNoise(item)) {
                continue;
            }
            if (isDiscount(item)) {
                if (!lines.isEmpty()) {
                    lines.get(lines.size() - 1).discounts().add(Math.abs(item.getTotalPrice()));
                }
                continue;
            }
            lines.add(new PricedLine(item, new ArrayList<>()));
        }
        boolean subtractDiscounts = discountsImproveTotal(response.getTotal(), lines);
        List<MlReceiptItem> items = new ArrayList<>();
        for (PricedLine line : lines) {
            MlReceiptItem item = line.item();
            if (subtractDiscounts) {
                item = withTotal(item, item.getTotalPrice() - discountStillDue(item, line.discounts()));
            }
            items.add(toServings(item));
        }
        return new MlReceiptResponse(
                response.getMerchant(),
                response.getDate(),
                response.getTotal(),
                response.getCurrency(),
                List.copyOf(items),
                response.getConfidence(),
                response.getErrors());
    }

    private record PricedLine(MlReceiptItem item, List<Double> discounts) {}

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
            gross += line.item().getTotalPrice();
            net += Math.max(0, line.item().getTotalPrice() - discountStillDue(line.item(), line.discounts()));
        }
        return Math.abs(net - payable) + MONEY_TOLERANCE < Math.abs(gross - payable);
    }

    private static double discountStillDue(MlReceiptItem item, List<Double> discounts) {
        if (discounts.isEmpty()) {
            return 0;
        }
        double discount = discounts.stream().mapToDouble(Double::doubleValue).sum();
        if (item.getUnitPrice() == null || item.getQuantity() <= 0) {
            return discount;
        }
        double alreadyOff = Math.max(0, item.getUnitPrice() * item.getQuantity() - item.getTotalPrice());
        return Math.max(0, discount - alreadyOff);
    }

    private static boolean isDiscount(MlReceiptItem item) {
        return item.getTotalPrice() < 0 || DISCOUNT_NAME.matcher(item.getName()).find();
    }

    private static boolean isNoise(MlReceiptItem item) {
        return NOISE_NAME.matcher(item.getName().trim()).matches() && Math.abs(item.getTotalPrice()) < 0.01;
    }

    private static MlReceiptItem toServings(MlReceiptItem item) {
        String name = item.getName().toLowerCase();
        if (!name.contains("розлив")) {
            return item;
        }
        Matcher volume = SERVING_ML.matcher(item.getName());
        if (!volume.find()) {
            return item;
        }
        BigDecimal milliliters = new BigDecimal(volume.group(1).replace(',', '.'));
        if (milliliters.compareTo(BigDecimal.valueOf(50)) < 0 || milliliters.compareTo(BigDecimal.valueOf(2000)) > 0) {
            return item;
        }
        BigDecimal servingLiters = milliliters.movePointLeft(3);
        BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
        BigDecimal pieces = quantity.divide(servingLiters, 4, RoundingMode.HALF_UP);
        BigDecimal nearest = pieces.setScale(0, RoundingMode.HALF_UP);
        boolean wholeServings =
                pieces.subtract(nearest).abs().compareTo(PIECE_TOLERANCE) <= 0 && nearest.intValue() >= 1;
        boolean printedAsLiters = quantity.stripTrailingZeros().scale() > 0
                || (item.getUnitPrice() != null && item.getUnitPrice() >= LITER_PRICE_HINT);
        if (wholeServings && printedAsLiters && nearest.compareTo(quantity) > 0) {
            return withQuantity(item, nearest.doubleValue());
        }
        if (quantity.compareTo(BigDecimal.ONE) < 0 && quantity.compareTo(BigDecimal.ZERO) > 0) {
            return withQuantity(item, 1);
        }
        return item;
    }

    private static MlReceiptItem withQuantity(MlReceiptItem item, double quantity) {
        Double unit = item.getUnitPrice();
        if (quantity > 0) {
            unit = pricePerUnit(item.getTotalPrice(), quantity);
        }
        return new MlReceiptItem(item.getName(), quantity, unit, item.getTotalPrice(), item.getConfidence());
    }

    private static MlReceiptItem withTotal(MlReceiptItem item, double total) {
        double safeTotal = Math.max(0, total);
        Double unit = item.getUnitPrice();
        if (item.getQuantity() > 0) {
            unit = pricePerUnit(safeTotal, item.getQuantity());
        }
        return new MlReceiptItem(item.getName(), item.getQuantity(), unit, safeTotal, item.getConfidence());
    }

    private static Double pricePerUnit(double total, double quantity) {
        return Double.valueOf(total / quantity);
    }
}
