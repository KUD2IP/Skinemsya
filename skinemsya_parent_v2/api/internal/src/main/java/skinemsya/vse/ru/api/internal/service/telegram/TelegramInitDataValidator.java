package skinemsya.vse.ru.api.internal.service.telegram;

import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramIdentity;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramInitData;

/** Проверка подписи и содержимого Telegram initData. */
public interface TelegramInitDataValidator {

    /** Проверяет initData и возвращает личность пользователя. */
    TelegramIdentity validate(String initData);

    /** Проверяет initData и возвращает пользователя, чат и приглашения. */
    TelegramInitData validateWithChat(String initData);
}
