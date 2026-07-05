package skinemsya.vse.ru.auth.domain;

import java.util.Optional;

public record AuthResult(TokenPair tokens, Optional<ChatBootstrap> chatBootstrap) {}
