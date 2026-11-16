package skinemsya.vse.ru.impl.mapper.entity;

import org.mapstruct.Mapper;
import skinemsya.vse.ru.api.internal.pojo.events.Event;
import skinemsya.vse.ru.api.internal.pojo.events.EventParticipant;
import skinemsya.vse.ru.data.entity.events.EventEntity;
import skinemsya.vse.ru.data.entity.events.EventParticipantEntity;

/** MapStruct-маппер между сущностями сборов и внутренними POJO. */
@Mapper(componentModel = "spring")
public interface EventEntityMapper {

    /** Конвертирует сущность сбора во внутренний POJO. */
    Event toEvent(EventEntity entity);

    /** Конвертирует внутренний POJO сбора в сущность. */
    EventEntity toEntity(Event event);

    /** Конвертирует сущность участника во внутренний POJO. */
    EventParticipant toEventParticipant(EventParticipantEntity entity);

    /** Конвертирует внутренний POJO участника в сущность. */
    EventParticipantEntity toEntity(EventParticipant participant);
}
