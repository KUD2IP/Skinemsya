package skinemsya.vse.ru.api.internal.service.files;

/** Дополнительная проверка общего доступа к файлу со стороны другого домена. */
public interface FileSharedAccessVerifier {

    /** Проверяет, может ли пользователь видеть файл как участник связанной операции. */
    boolean canAccess(long fileId, long requesterId);
}
