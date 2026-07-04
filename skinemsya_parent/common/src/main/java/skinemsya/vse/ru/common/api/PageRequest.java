package skinemsya.vse.ru.common.api;

/**
 * Pagination request parameters.
 */
public record PageRequest(int page, int size) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_SIZE);
        }
    }

    public static PageRequest defaults() {
        return new PageRequest(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }

    public long offset() {
        return (long) page * size;
    }
}
