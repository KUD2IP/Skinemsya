package skinemsya.vse.ru.api.internal.pojo.common;

/** Параметры постраничного запроса. */
public record PageRequest(int page, int size) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    /** Проверяет границы номера страницы и размера. */
    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_SIZE);
        }
    }

    /** Возвращает параметры страницы по умолчанию. */
    public static PageRequest defaults() {
        return new PageRequest(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    /** Создаёт параметры страницы. */
    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }

    /** Возвращает смещение первой записи страницы. */
    public long offset() {
        return (long) page * size;
    }
}
