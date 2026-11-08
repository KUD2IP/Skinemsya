package skinemsya.vse.ru.subscriptions.infrastructure.yookassa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class YooKassaAmountTest {

    @Test
    void invoicePriceIsFortyNineRubles() {
        assertThat(HttpYooKassaClient.rubles(4900)).isEqualTo("49.00");
    }
}
