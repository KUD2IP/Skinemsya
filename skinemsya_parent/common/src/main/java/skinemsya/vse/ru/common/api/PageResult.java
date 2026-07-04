package skinemsya.vse.ru.common.api;

import java.util.List;

/**
 * Paginated result wrapper.
 */
public record PageResult<T>(List<T> items, int page, int size, long totalElements) {

    public PageResult {
        items = items == null ? List.of() : List.copyOf(items);
        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must be positive");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements must be non-negative");
        }
    }

    public static <T> PageResult<T> of(List<T> items, PageRequest request, long totalElements) {
        return new PageResult<>(items, request.page(), request.size(), totalElements);
    }

    public int totalPages() {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
}
