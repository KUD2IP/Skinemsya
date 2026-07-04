package skinemsya.vse.ru.events.infrastructure.mapper;

import org.mapstruct.Mapper;
import skinemsya.vse.ru.events.domain.Event;
import skinemsya.vse.ru.events.infrastructure.persistence.EventEntity;

@Mapper(componentModel = "spring")
public interface EventMapper {

    Event toDomain(EventEntity entity);
}
