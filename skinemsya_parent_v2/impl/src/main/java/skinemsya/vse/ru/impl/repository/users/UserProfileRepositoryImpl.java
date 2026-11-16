package skinemsya.vse.ru.impl.repository.users;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.pojo.users.UserProfile;
import skinemsya.vse.ru.api.internal.repository.users.UserProfileRepository;
import skinemsya.vse.ru.impl.mapper.entity.UserEntityMapper;

/** Адаптер между внутренним портом профилей и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class UserProfileRepositoryImpl implements UserProfileRepository {

    private final UserProfileJpaRepository userProfileJpaRepository;
    private final UserEntityMapper userEntityMapper;

    @Override
    public Optional<UserProfile> findByUserId(long userId) {
        return userProfileJpaRepository.findByUserId(userId).map(userEntityMapper::toUserProfile);
    }

    @Override
    public void insertIfAbsent(long userId) {
        userProfileJpaRepository.insertIfAbsent(userId);
    }

    @Override
    public UserProfile save(UserProfile profile) {
        var saved = userProfileJpaRepository.save(userEntityMapper.toEntity(profile));
        return userEntityMapper.toUserProfile(saved);
    }
}
