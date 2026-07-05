package skinemsya.vse.ru.receipts.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record UpdateSelectionsRequest(@NotEmpty @Valid List<SelectionItem> selections) {

    public record SelectionItem(@NotNull long positionId, @NotNull @DecimalMin("0.01") BigDecimal quantity) {}
}
