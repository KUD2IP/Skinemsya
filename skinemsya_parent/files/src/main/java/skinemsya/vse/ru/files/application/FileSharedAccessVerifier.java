package skinemsya.vse.ru.files.application;

public interface FileSharedAccessVerifier {

    boolean canAccess(long fileId, long requesterId);
}
