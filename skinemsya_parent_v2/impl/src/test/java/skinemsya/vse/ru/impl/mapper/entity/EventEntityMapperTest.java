package skinemsya.vse.ru.impl.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createEventEntity;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createEventParticipantEntity;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Тесты MapStruct-маппинга сборов и участников. */
class EventEntityMapperTest {

    private final EventEntityMapper mapper = Mappers.getMapper(EventEntityMapper.class);

    /** Проверяет преобразование сбора в обе стороны без потери полей. */
    @Test
    void shouldMapEventInBothDirections() {
        var entity = createEventEntity();

        var event = mapper.toEvent(entity);
        var mappedEntity = mapper.toEntity(event);

        assertThat(event.getName()).isEqualTo("Dinner");
        assertThat(mappedEntity).usingRecursiveComparison().ignoringFields("deletedAt").isEqualTo(entity);
    }

    /** Проверяет преобразование участника сбора в обе стороны без потери полей. */
    @Test
    void shouldMapEventParticipantInBothDirections() {
        var entity = createEventParticipantEntity();

        var participant = mapper.toEventParticipant(entity);
        var mappedEntity = mapper.toEntity(participant);

        assertThat(participant.getUserId()).isEqualTo(2L);
        assertThat(mappedEntity).usingRecursiveComparison().isEqualTo(entity);
    }
}
