package skinemsya.vse.ru.users.application;

import java.util.List;
import skinemsya.vse.ru.users.domain.User;

public record UserSlice(List<User> items, int page, int size, long total) {}
