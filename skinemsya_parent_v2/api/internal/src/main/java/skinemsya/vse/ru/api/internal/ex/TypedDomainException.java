package skinemsya.vse.ru.api.internal.ex;

import skinemsya.vse.ru.api.internal.enums.ErrorCode;

/** Исключение, которое может быть преобразовано в машиночитаемый ответ API. */
public interface TypedDomainException {

    ErrorCode errorCode();

    String getMessage();
}
