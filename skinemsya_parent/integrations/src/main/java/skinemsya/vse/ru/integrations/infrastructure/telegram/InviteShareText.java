package skinemsya.vse.ru.integrations.infrastructure.telegram;

public final class InviteShareText {

    private InviteShareText() {}

    public static String forGroup(String groupName) {
        return "Присоединяйся к группе «" + groupName + "» в Skinemsya — делим расходы вместе:";
    }

    public static String forEvent(String eventName, String groupName) {
        return "Присоединяйся к сбору «" + eventName + "» в группе «" + groupName
                + "» в Skinemsya — делим расходы вместе:";
    }
}
