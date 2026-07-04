package skinemsya.vse.ru.users.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.users.domain.TelegramUserData;
import skinemsya.vse.ru.users.domain.User;
import skinemsya.vse.ru.users.infrastructure.mapper.UserMapper;
import skinemsya.vse.ru.users.infrastructure.persistence.UserEntity;
import skinemsya.vse.ru.users.infrastructure.persistence.UserProfileRepository;
import skinemsya.vse.ru.users.infrastructure.persistence.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldCreateUserAndProfileOnFirstTelegramLogin() {
        var telegramData = new TelegramUserData(100_001L, "Alice", "alice");
        var savedEntity = new UserEntity();
        savedEntity.setId(1L);
        savedEntity.setTelegramUserId(100_001L);
        savedEntity.setDisplayName("Alice");
        savedEntity.setCreatedAt(Instant.now());
        savedEntity.setUpdatedAt(Instant.now());
        var domainUser = new User(1L, 100_001L, "Alice", "alice", Instant.now(), Instant.now());

        when(userRepository.findByTelegramUserId(100_001L)).thenReturn(Optional.empty(), Optional.of(savedEntity));
        when(userRepository.insertTelegramUserIfAbsent(
                eq(100_001L),
                eq("Alice"),
                eq(null),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(1);
        when(userRepository.save(savedEntity)).thenReturn(savedEntity);
        when(userMapper.toDomain(savedEntity)).thenReturn(domainUser);

        var result = userService.upsertFromTelegram(telegramData);

        assertThat(result.displayName()).isEqualTo("Alice");
        verify(userProfileRepository).insertIfAbsent(1L);
        verify(userRepository).clearTelegramUsernameForOtherUsers(eq("alice"), eq(100_001L), any(Instant.class));
        verify(userRepository).insertTelegramUserIfAbsent(
                eq(100_001L),
                eq("Alice"),
                eq(null),
                any(Instant.class),
                any(Instant.class)
        );
        assertThat(savedEntity.getTelegramUsername()).isEqualTo("alice");
    }

    @Test
    void shouldUpdateDisplayNameForExistingUser() {
        var telegramData = new TelegramUserData(100_001L, "Bob", "bob");
        var existing = new UserEntity();
        existing.setId(1L);
        existing.setTelegramUserId(100_001L);
        existing.setDisplayName("Alice");
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());
        var domainUser = new User(1L, 100_001L, "Bob", "bob", Instant.now(), Instant.now());

        when(userRepository.findByTelegramUserId(100_001L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(userMapper.toDomain(existing)).thenReturn(domainUser);

        var result = userService.upsertFromTelegram(telegramData);

        assertThat(result.displayName()).isEqualTo("Bob");
        verify(userRepository, never()).insertTelegramUserIfAbsent(anyLong(), any(), any(), any(), any());
        verify(userProfileRepository).insertIfAbsent(1L);
    }
}
