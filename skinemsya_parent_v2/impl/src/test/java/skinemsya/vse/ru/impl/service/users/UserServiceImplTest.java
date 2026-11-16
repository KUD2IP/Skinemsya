package skinemsya.vse.ru.impl.service.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createTelegramUserData;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createUser;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.api.internal.pojo.users.TelegramUserData;
import skinemsya.vse.ru.api.internal.repository.users.UserProfileRepository;
import skinemsya.vse.ru.api.internal.repository.users.UserRepository;

/** Тесты бизнес-логики пользователей. */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserServiceImpl userService;

    /** Проверяет создание пользователя и профиля при первом входе. */
    @Test
    void shouldCreateUserAndProfileOnFirstTelegramLogin() {
        var telegramData = createTelegramUserData();
        telegramData.setPhotoUrl(null);
        var savedUser = createUser();
        savedUser.setTelegramUsername(null);

        when(userRepository.findByTelegramUserId(100_001L)).thenReturn(Optional.empty(), Optional.of(savedUser));
        when(userRepository.save(savedUser)).thenReturn(savedUser);

        var result = userService.upsertFromTelegram(telegramData);

        assertThat(result.getDisplayName()).isEqualTo("Alice");
        verify(userProfileRepository).insertIfAbsent(1L);
        verify(userRepository).clearTelegramUsernameForOtherUsers(eq("alice"), eq(100_001L), any(Instant.class));
        verify(userRepository)
                .insertTelegramUserIfAbsent(
                        eq(100_001L), eq("Alice"), eq(null), any(Instant.class), any(Instant.class));
        assertThat(savedUser.getTelegramUsername()).isEqualTo("alice");
    }

    /** Проверяет обновление данных существующего пользователя. */
    @Test
    void shouldUpdateDisplayNameForExistingUser() {
        var telegramData = new TelegramUserData(100_001L, "Bob", "bob", "https://t.me/i/userpic/320/bob.jpg");
        var existing = createUser();

        when(userRepository.findByTelegramUserId(100_001L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        var result = userService.upsertFromTelegram(telegramData);

        assertThat(result.getDisplayName()).isEqualTo("Bob");
        assertThat(existing.getPhotoUrl()).isEqualTo("https://t.me/i/userpic/320/bob.jpg");
        verify(userRepository, never())
                .insertTelegramUserIfAbsent(anyLong(), any(), any(), any(Instant.class), any(Instant.class));
        verify(userProfileRepository).insertIfAbsent(1L);
    }

    /** Проверяет сохранение фотографии, если Telegram не передал её повторно. */
    @Test
    void shouldKeepStoredPhotoWhenLaterLoginOmitsIt() {
        var telegramData = new TelegramUserData(100_001L, "Bob", "bob", null);
        var existing = createUser();
        existing.setDisplayName("Bob");
        existing.setPhotoUrl("https://t.me/i/userpic/320/bob.jpg");

        when(userRepository.findByTelegramUserId(100_001L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        userService.upsertFromTelegram(telegramData);

        assertThat(existing.getPhotoUrl()).isEqualTo("https://t.me/i/userpic/320/bob.jpg");
    }

    /** Проверяет нормализацию HTTPS-адреса фотографии. */
    @Test
    void shouldDropPhotoUrlThatIsNotHttps() {
        assertThat(UserServiceImpl.normalizePhotoUrl("http://example.com/a.jpg"))
                .isNull();
        assertThat(UserServiceImpl.normalizePhotoUrl(" https://cdn.example/a.jpg "))
                .isEqualTo("https://cdn.example/a.jpg");
    }
}
