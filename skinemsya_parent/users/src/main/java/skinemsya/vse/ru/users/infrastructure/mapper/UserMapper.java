package skinemsya.vse.ru.users.infrastructure.mapper;

import org.mapstruct.Mapper;
import skinemsya.vse.ru.users.domain.User;
import skinemsya.vse.ru.users.domain.UserProfile;
import skinemsya.vse.ru.users.infrastructure.persistence.UserEntity;
import skinemsya.vse.ru.users.infrastructure.persistence.UserProfileEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toDomain(UserEntity entity);

    UserProfile toDomain(UserProfileEntity entity);
}
