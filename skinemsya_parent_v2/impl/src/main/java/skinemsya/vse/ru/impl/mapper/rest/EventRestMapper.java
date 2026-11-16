package skinemsya.vse.ru.impl.mapper.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.events.Event;
import skinemsya.vse.ru.api.rest.events.dto.EventResponse;
import skinemsya.vse.ru.api.rest.events.dto.PageEventResponse;

/** MapStruct-маппер между внутренними POJO сборов и REST DTO. */
@Mapper(componentModel = "spring")
public interface EventRestMapper {

    /** Конвертирует сбор в REST-ответ. */
    @Mapping(target = "payerRequisitesReady", ignore = true)
    @Mapping(target = "joinedCount", ignore = true)
    @Mapping(target = "currentUserJoined", ignore = true)
    EventResponse toResponse(Event event);

    /** Конвертирует сбор в REST-ответ вместе с вычисленными полями текущего пользователя. */
    default EventResponse toResponse(
            Event event, boolean payerRequisitesReady, long joinedCount, boolean currentUserJoined) {
        return toResponse(event)
                .payerRequisitesReady(payerRequisitesReady)
                .joinedCount(joinedCount)
                .currentUserJoined(currentUserJoined);
    }

    /** Конвертирует страницу сборов в REST-ответ. */
    default PageEventResponse toEventPageResponse(PageResult<EventResponse> page) {
        return new PageEventResponse()
                .items(page.items())
                .page(page.page())
                .size(page.size())
                .totalElements(page.totalElements())
                .totalPages(page.totalPages());
    }

    /** Конвертирует статус сбора в enum REST DTO. */
    default EventResponse.StatusEnum toStatusEnum(EventStatus status) {
        return status == null ? null : EventResponse.StatusEnum.valueOf(status.name());
    }

    /** Конвертирует {@link Instant} в {@link OffsetDateTime} по UTC. */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
