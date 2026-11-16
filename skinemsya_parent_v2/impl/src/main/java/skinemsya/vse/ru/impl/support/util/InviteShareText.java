package skinemsya.vse.ru.impl.support.util;

import lombok.experimental.UtilityClass;

/** Тексты сообщений для отправки приглашений. */
@UtilityClass
public class InviteShareText {


    /** Возвращает текст приглашения в группу. */
    public static String forGroup(String groupName) {
        return "Присоединяйся к группе «" + groupName + "» в Skinemsya — делим расходы вместе:";
    }

    /** Возвращает текст приглашения в событие. */
    public static String forEvent(String eventName, String groupName) {
        return "Присоединяйся к сбору «" + eventName + "» в группе «" + groupName
                + "» в Skinemsya — делим расходы вместе:";
    }
}
