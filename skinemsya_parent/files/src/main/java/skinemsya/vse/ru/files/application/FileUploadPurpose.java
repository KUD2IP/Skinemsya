package skinemsya.vse.ru.files.application;

public enum FileUploadPurpose {
    RECEIPT,
    PAYMENT_PROOF;

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
