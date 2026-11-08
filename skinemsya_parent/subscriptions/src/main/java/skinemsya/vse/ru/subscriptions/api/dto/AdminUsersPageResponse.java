package skinemsya.vse.ru.subscriptions.api.dto;

import java.util.List;

public record AdminUsersPageResponse(List<AdminUserResponse> items, int page, int size, long total) {}
