package skinemsya.vse.ru.impl.mapper.entity;

import java.util.List;
import org.mapstruct.Mapper;
import skinemsya.vse.ru.api.internal.pojo.users.User;
import skinemsya.vse.ru.api.internal.pojo.users.UserProfile;
import skinemsya.vse.ru.data.entity.users.UserEntity;
import skinemsya.vse.ru.data.entity.users.UserProfileEntity;

/** MapStruct-маппер между пользовательскими POJO и JPA-сущностями. */
@Mapper(componentModel = "spring")
public interface UserEntityMapper {

    /** Преобразует JPA-сущность пользователя во внутренний POJO. */
    User toUser(UserEntity entity);

    /** Преобразует список JPA-сущностей пользователей во внутренние POJO. */
    List<User> toUsers(List<UserEntity> entities);

    /** Преобразует внутренний POJO пользователя в JPA-сущность. */
    UserEntity toEntity(User user);

    /** Преобразует JPA-сущность профиля во внутренний POJO. */
    UserProfile toUserProfile(UserProfileEntity entity);

    /** Преобразует внутренний POJO профиля в JPA-сущность. */
    UserProfileEntity toEntity(UserProfile profile);
}
