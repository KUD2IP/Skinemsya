package skinemsya.vse.ru.payments.api.dto;

import jakarta.validation.constraints.NotNull;

public record ConfirmDebtorRequest(@NotNull long screenshotFileId) {
}
