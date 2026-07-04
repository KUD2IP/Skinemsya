package skinemsya.vse.ru.integrations.application;

import skinemsya.vse.ru.integrations.domain.TelegramIdentity;
import skinemsya.vse.ru.integrations.domain.TelegramInitData;

public interface TelegramInitDataValidator {

    TelegramIdentity validate(String initData);

    TelegramInitData validateWithChat(String initData);
}
