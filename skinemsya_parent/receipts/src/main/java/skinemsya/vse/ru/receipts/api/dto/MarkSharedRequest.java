package skinemsya.vse.ru.receipts.api.dto;

import jakarta.validation.constraints.NotNull;

public record MarkSharedRequest(@NotNull boolean forAll) {}
