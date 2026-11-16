package skinemsya.vse.ru.impl.mapper.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createUser;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createUserProfile;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Тесты MapStruct-маппинга пользовательских REST DTO. */
class UserRestMapperTest {

    private final UserRestMapper mapper = Mappers.getMapper(UserRestMapper.class);

    /** Проверяет объединение пользователя и профиля в сгенерированный DTO. */
    @Test
    void shouldCombineUserAndProfileIntoGeneratedResponse() {
        var response = mapper.toResponse(createUser(), createUserProfile());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTelegramUserId()).isEqualTo(100_001L);
        assertThat(response.getDisplayName()).isEqualTo("Alice");
        assertThat(response.getPaymentDetails()).isEqualTo("+79990000000");
        assertThat(response.getPhone()).isEqualTo("+79990000000");
        assertThat(response.getPreferredBank()).isEqualTo("Bank");
        assertThat(response.getNotificationSettings()).isEqualTo("{\"enabled\":true}");
        assertThat(response.getPhotoUrl()).isEqualTo("https://cdn.example/photo.jpg");
    }
}
