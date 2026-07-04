package skinemsya.vse.ru.receipts.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdatePositionRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.01") BigDecimal quantity,
        @NotNull long totalPriceKopecks
) {
}
