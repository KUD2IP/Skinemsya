package skinemsya.vse.ru.groups.infrastructure.mapper;

import org.mapstruct.Mapper;
import skinemsya.vse.ru.groups.domain.Group;
import skinemsya.vse.ru.groups.domain.GroupMember;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupEntity;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupMemberEntity;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    Group toDomain(GroupEntity entity);

    GroupMember toDomain(GroupMemberEntity entity);
}
