package skinemsya.vse.ru.impl.mapper.rest;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import skinemsya.vse.ru.api.internal.pojo.users.User;
import skinemsya.vse.ru.api.internal.pojo.users.UserProfile;
import skinemsya.vse.ru.api.rest.users.dto.UserResponse;

/** MapStruct-маппер внутренних пользовательских POJO в REST DTO. */
@Mapper(componentModel = "spring")
public interface UserRestMapper {

    /** Объединяет пользователя и профиль в сгенерированный REST DTO. */
    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "telegramUserId", source = "user.telegramUserId")
    @Mapping(target = "displayName", source = "user.displayName")
    @Mapping(target = "photoUrl", source = "user.photoUrl")
    @Mapping(target = "paymentDetails", source = "profile.paymentDetails")
    @Mapping(target = "phone", source = "profile.phone")
    @Mapping(target = "preferredBank", source = "profile.preferredBank")
    @Mapping(target = "notificationSettings", source = "profile.notificationSettings")
    UserResponse toResponse(User user, UserProfile profile);
}
