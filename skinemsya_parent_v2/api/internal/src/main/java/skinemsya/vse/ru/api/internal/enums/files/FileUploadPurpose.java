package skinemsya.vse.ru.api.internal.enums.files;

/** Назначение загружаемого файла. */
public enum FileUploadPurpose {
    RECEIPT,
    PAYMENT_PROOF;

    /** Разбирает значение query-параметра. Неизвестное значение считается чеком. */
    public static FileUploadPurpose from(String value) {
        if (value == null || value.isBlank()) {
            return RECEIPT;
        }
        return switch (value.trim().toLowerCase()) {
            case "receipt" -> RECEIPT;
            case "payment-proof", "payment_proof" -> PAYMENT_PROOF;
            default -> RECEIPT;
        };
    }
}
