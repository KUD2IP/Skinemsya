package skinemsya.vse.ru.impl.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createUserEntity;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createUserProfileEntity;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import skinemsya.vse.ru.api.internal.pojo.users.User;
import skinemsya.vse.ru.api.internal.pojo.users.UserProfile;
import skinemsya.vse.ru.data.entity.users.UserEntity;
import skinemsya.vse.ru.data.entity.users.UserProfileEntity;

/** Тесты MapStruct-маппинга JPA-сущностей пользователей. */
class UserEntityMapperTest {

    private final UserEntityMapper mapper = Mappers.getMapper(UserEntityMapper.class);

    /** Проверяет преобразование пользователя в обе стороны без потери полей. */
    @Test
    void shouldMapUserInBothDirections() {
        var entity = createUserEntity();

        User user = mapper.toUser(entity);
        UserEntity mappedEntity = mapper.toEntity(user);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getTelegramUsername()).isEqualTo("alice");
        assertThat(mappedEntity).usingRecursiveComparison().isEqualTo(entity);
    }

    /** Проверяет преобразование профиля в обе стороны без потери полей. */
    @Test
    void shouldMapUserProfileInBothDirections() {
        var entity = createUserProfileEntity();

        UserProfile profile = mapper.toUserProfile(entity);
        UserProfileEntity mappedEntity = mapper.toEntity(profile);

        assertThat(profile.getUserId()).isEqualTo(1L);
        assertThat(profile.getPreferredBank()).isEqualTo("Bank");
        assertThat(mappedEntity).usingRecursiveComparison().isEqualTo(entity);
    }
}
