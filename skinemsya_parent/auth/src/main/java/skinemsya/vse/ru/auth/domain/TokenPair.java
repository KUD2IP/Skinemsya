package skinemsya.vse.ru.auth.domain;

public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}
