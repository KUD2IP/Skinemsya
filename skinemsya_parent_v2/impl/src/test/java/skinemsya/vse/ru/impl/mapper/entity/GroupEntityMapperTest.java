package skinemsya.vse.ru.impl.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createGroupEntity;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createGroupMemberEntity;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Тесты MapStruct-маппинга групп и участников. */
class GroupEntityMapperTest {

    private final GroupEntityMapper mapper = Mappers.getMapper(GroupEntityMapper.class);

    /** Проверяет преобразование группы в обе стороны без потери полей. */
    @Test
    void shouldMapGroupInBothDirections() {
        var entity = createGroupEntity();

        var group = mapper.toGroup(entity);
        var mappedEntity = mapper.toEntity(group);

        assertThat(group.getName()).isEqualTo("Friends");
        assertThat(mappedEntity).usingRecursiveComparison().ignoringFields("deletedAt").isEqualTo(entity);
    }

    /** Проверяет преобразование участника группы в обе стороны без потери полей. */
    @Test
    void shouldMapGroupMemberInBothDirections() {
        var entity = createGroupMemberEntity();

        var member = mapper.toGroupMember(entity);
        var mappedEntity = mapper.toEntity(member);

        assertThat(member.getGroupId()).isEqualTo(10L);
        assertThat(mappedEntity).usingRecursiveComparison().isEqualTo(entity);
    }
}
