package skinemsya.vse.ru.app.predictions;

public record ChatPerson(long telegramUserId, String displayName, String username) {

    public ChatPerson {
        if (displayName == null || displayName.isBlank()) {
            displayName = "участник";
        } else {
            displayName = displayName.trim();
        }
        if (displayName.length() > 255) {
            displayName = displayName.substring(0, 255);
        }
        if (username != null) {
            username = username.trim();
            if (username.startsWith("@")) {
                username = username.substring(1);
            }
            if (username.isBlank()) {
                username = null;
            } else if (username.length() > 64) {
                username = username.substring(0, 64);
            }
        }
    }
}
