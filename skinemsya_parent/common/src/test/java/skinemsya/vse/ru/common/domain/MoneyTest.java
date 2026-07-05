package skinemsya.vse.ru.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void shouldCreateMoneyFromKopecks() {
        var money = Money.ofKopecks(15050);

        assertThat(money.kopecks()).isEqualTo(15050);
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> Money.ofKopecks(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Money amount cannot be negative");
    }

    @Test
    void shouldAddMoney() {
        var result = Money.ofKopecks(100).add(Money.ofKopecks(250));

        assertThat(result.kopecks()).isEqualTo(350);
    }

    @Test
    void shouldSubtractMoney() {
        var result = Money.ofKopecks(500).subtract(Money.ofKopecks(200));

        assertThat(result.kopecks()).isEqualTo(300);
    }

    @Test
    void shouldRejectSubtractionBelowZero() {
        assertThatThrownBy(() -> Money.ofKopecks(100).subtract(Money.ofKopecks(200)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Money subtraction would result in negative amount");
    }

    @Test
    void shouldReturnZero() {
        assertThat(Money.zero().kopecks()).isZero();
    }
}
