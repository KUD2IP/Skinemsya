package skinemsya.vse.ru.impl.mapper.entity;

import org.mapstruct.Mapper;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMember;
import skinemsya.vse.ru.data.entity.groups.GroupEntity;
import skinemsya.vse.ru.data.entity.groups.GroupMemberEntity;

/** MapStruct-маппер между сущностями групп и внутренними POJO. */
@Mapper(componentModel = "spring")
public interface GroupEntityMapper {

    /** Конвертирует сущность группы во внутренний POJO. */
    Group toGroup(GroupEntity entity);

    /** Конвертирует внутренний POJO группы в сущность. */
    GroupEntity toEntity(Group group);

    /** Конвертирует сущность участника во внутренний POJO. */
    GroupMember toGroupMember(GroupMemberEntity entity);

    /** Конвертирует внутренний POJO участника в сущность. */
    GroupMemberEntity toEntity(GroupMember member);
}
