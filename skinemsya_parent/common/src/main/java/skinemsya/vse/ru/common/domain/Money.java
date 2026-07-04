package skinemsya.vse.ru.common.domain;

/**
 * Monetary amount stored in kopecks (minimal currency units).
 */
public record Money(long kopecks) {

    public Money {
        if (kopecks < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }
    }

    public static Money ofKopecks(long kopecks) {
        return new Money(kopecks);
    }

    public static Money zero() {
        return new Money(0);
    }

    public Money add(Money other) {
        return new Money(Math.addExact(kopecks, other.kopecks));
    }

    public Money subtract(Money other) {
        long result = kopecks - other.kopecks;
        if (result < 0) {
            throw new IllegalArgumentException("Money subtraction would result in negative amount");
        }
        return new Money(result);
    }
}
