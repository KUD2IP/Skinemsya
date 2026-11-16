package skinemsya.vse.ru.impl.mapper.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMemberView;
import skinemsya.vse.ru.api.rest.groups.dto.GroupMemberViewResponse;
import skinemsya.vse.ru.api.rest.groups.dto.GroupResponse;
import skinemsya.vse.ru.api.rest.groups.dto.PageGroupMemberViewResponse;
import skinemsya.vse.ru.api.rest.groups.dto.PageGroupResponse;

/** MapStruct-маппер между внутренними POJO групп и REST DTO. */
@Mapper(componentModel = "spring")
public interface GroupRestMapper {

    /** Конвертирует группу в REST-ответ. */
    @Mapping(target = "type", expression = "java(toTypeEnum(group.getType()))")
    GroupResponse toResponse(Group group);

    /** Конвертирует участника группы в REST-ответ. */
    @Mapping(target = "role", expression = "java(toRoleEnum(member.getRole()))")
    GroupMemberViewResponse toResponse(GroupMemberView member);

    /** Конвертирует страницу групп в REST-ответ. */
    default PageGroupResponse toGroupPageResponse(PageResult<Group> page) {
        return new PageGroupResponse()
                .items(page.items().stream().map(this::toResponse).toList())
                .page(page.page())
                .size(page.size())
                .totalElements(page.totalElements())
                .totalPages(page.totalPages());
    }

    /** Конвертирует страницу участников в REST-ответ. */
    default PageGroupMemberViewResponse toMemberPageResponse(PageResult<GroupMemberView> page) {
        return new PageGroupMemberViewResponse()
                .items(page.items().stream().map(this::toResponse).toList())
                .page(page.page())
                .size(page.size())
                .totalElements(page.totalElements())
                .totalPages(page.totalPages());
    }

    /** Конвертирует тип группы в enum REST DTO. */
    default GroupResponse.TypeEnum toTypeEnum(skinemsya.vse.ru.api.internal.enums.groups.GroupType type) {
        return type == null ? null : GroupResponse.TypeEnum.valueOf(type.name());
    }

    /** Конвертирует роль участника в enum REST DTO. */
    default GroupMemberViewResponse.RoleEnum toRoleEnum(skinemsya.vse.ru.api.internal.enums.groups.GroupRole role) {
        return role == null ? null : GroupMemberViewResponse.RoleEnum.valueOf(role.name());
    }

    /** Конвертирует {@link Instant} в {@link OffsetDateTime} по UTC. */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    /** Конвертирует список групп в список REST-ответов. */
    List<GroupResponse> toGroupResponses(List<Group> groups);
}
