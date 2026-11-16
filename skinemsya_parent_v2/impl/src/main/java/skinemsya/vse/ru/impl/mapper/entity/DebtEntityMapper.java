package skinemsya.vse.ru.impl.mapper.entity;

import org.mapstruct.Mapper;
import skinemsya.vse.ru.api.internal.pojo.debts.Debt;
import skinemsya.vse.ru.data.entity.debts.DebtEntity;

/** MapStruct-маппер между сущностью долга и внутренним POJO. */
@Mapper(componentModel = "spring")
public interface DebtEntityMapper {

    /** Конвертирует сущность долга во внутренний POJO. */
    Debt toDebt(DebtEntity entity);

    /** Конвертирует внутренний POJO долга в сущность. */
    DebtEntity toEntity(Debt debt);
}
