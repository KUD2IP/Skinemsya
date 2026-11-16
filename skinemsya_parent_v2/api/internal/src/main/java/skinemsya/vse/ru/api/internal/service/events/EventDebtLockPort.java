package skinemsya.vse.ru.api.internal.service.events;

/** Признак подтверждённых или ожидающих подтверждения переводов по сбору. */
public interface EventDebtLockPort {

    /** Проверяет, есть ли у сбора заблокированные долги. */
    boolean hasLockedDebts(long eventId);
}
