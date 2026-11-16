package skinemsya.vse.ru.api.internal.repository.debts;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.pojo.debts.Debt;

/** Порт хранения долгов. */
public interface DebtRepository {

    /** Находит долг по идентификатору. */
    Optional<Debt> findById(long debtId);

    /** Находит долг сбора по идентификатору. */
    Optional<Debt> findByIdAndEventId(long id, long eventId);

    /** Возвращает долги сбора. */
    List<Debt> findByEventId(long eventId);

    /** Возвращает долги, где пользователь должник. */
    List<Debt> findByDebtorId(long debtorId);

    /** Возвращает долги, где пользователь кредитор. */
    List<Debt> findByCreditorId(long creditorId);

    /** Находит долг участника в сборе. */
    Optional<Debt> findByEventIdAndDebtorId(long eventId, long debtorId);

    /** Считает долги сбора не в заданном статусе. */
    long countByEventIdAndStatusNot(long eventId, DebtStatus status);

    /** Проверяет, есть ли долг сбора не в заданном статусе. */
    boolean existsByEventIdAndStatusNot(long eventId, DebtStatus status);

    /** Проверяет, есть ли долг сбора в одном из статусов. */
    boolean existsByEventIdAndStatusIn(long eventId, Collection<DebtStatus> statuses);

    /** Проверяет долг пользователя в сборе и статусе. */
    boolean existsByDebtorIdAndEventIdAndStatus(long debtorId, long eventId, DebtStatus status);

    /** Проверяет долг пользователя по сборам и статусам. */
    boolean existsByDebtorIdAndEventIdInAndStatusIn(
            long debtorId, Collection<Long> eventIds, Collection<DebtStatus> statuses);

    /** Сохраняет долг. */
    Debt save(Debt debt);

    /** Удаляет долг. */
    void delete(Debt debt);

    /** Удаляет все долги сбора. */
    void deleteByEventId(long eventId);

    /** Удаляет долги сбора в заданном статусе. */
    void deleteByEventIdAndStatus(long eventId, DebtStatus status);

    /** Удаляет долг участника в заданном статусе. */
    void deleteByEventIdAndDebtorIdAndStatus(long eventId, long debtorId, DebtStatus status);
}
