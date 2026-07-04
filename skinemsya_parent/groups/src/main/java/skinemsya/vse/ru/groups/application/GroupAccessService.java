package skinemsya.vse.ru.groups.application;

import java.util.List;

public interface GroupAccessService {

    void requireMember(long groupId, long userId);

    void requireOwner(long groupId, long userId);

    boolean isMember(long groupId, long userId);

    List<Long> memberUserIds(long groupId);
}
