package skinemsya.vse.ru.api.internal.service.auth;

/** Выдача доступа к событию по прямой ссылке. */
public interface EventDeepLinkAccessService {

    /** Обеспечивает пользователю доступ к событию. */
    void ensureAccess(long eventId, long userId);
}
