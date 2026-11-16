package skinemsya.vse.ru.impl.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createDebtEntity;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Тесты MapStruct-маппинга долгов. */
class DebtEntityMapperTest {

    private final DebtEntityMapper mapper = Mappers.getMapper(DebtEntityMapper.class);

    /** Проверяет преобразование долга в обе стороны без потери полей. */
    @Test
    void shouldMapDebtInBothDirections() {
        var entity = createDebtEntity();

        var debt = mapper.toDebt(entity);
        var mappedEntity = mapper.toEntity(debt);

        assertThat(debt.getAmountKopecks()).isEqualTo(5_000L);
        assertThat(mappedEntity).usingRecursiveComparison().isEqualTo(entity);
    }
}
