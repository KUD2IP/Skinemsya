package skinemsya.vse.ru.common.domain;

/**
 * Domain exception with a stable API error code mapping.
 */
public interface TypedDomainException {

    ErrorCode errorCode();

    String getMessage();
}
