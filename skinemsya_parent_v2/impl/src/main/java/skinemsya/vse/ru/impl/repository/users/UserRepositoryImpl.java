package skinemsya.vse.ru.impl.repository.users;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.pojo.users.User;
import skinemsya.vse.ru.api.internal.pojo.users.UserSlice;
import skinemsya.vse.ru.api.internal.repository.users.UserRepository;
import skinemsya.vse.ru.data.entity.users.UserEntity;
import skinemsya.vse.ru.impl.mapper.entity.UserEntityMapper;

/** Адаптер между внутренним портом пользователей и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserEntityMapper userEntityMapper;

    @Override
    public Optional<User> findById(long userId) {
        return userJpaRepository.findById(userId).map(userEntityMapper::toUser);
    }

    @Override
    public Optional<User> findByTelegramUserId(long telegramUserId) {
        return userJpaRepository.findByTelegramUserId(telegramUserId).map(userEntityMapper::toUser);
    }

    @Override
    public Optional<User> findByTelegramUsername(String telegramUsername) {
        return userJpaRepository.findByTelegramUsernameIgnoreCase(telegramUsername).map(userEntityMapper::toUser);
    }

    @Override
    public UserSlice findAllExcept(long excludeUserId, int page, int size) {
        var result = userJpaRepository.findByIdNotOrderByCreatedAtDesc(excludeUserId, PageRequest.of(page, size));
        return toSlice(result);
    }

    @Override
    public UserSlice findByUsernameExcept(long excludeUserId, String usernameQuery, int page, int size) {
        var result = userJpaRepository.findByIdNotAndTelegramUsernameContainingIgnoreCaseOrderByCreatedAtDesc(
                excludeUserId, usernameQuery, PageRequest.of(page, size));
        return toSlice(result);
    }

    @Override
    public void insertTelegramUserIfAbsent(
            long telegramUserId, String displayName, String telegramUsername, Instant createdAt, Instant updatedAt) {
        userJpaRepository.insertTelegramUserIfAbsent(telegramUserId, displayName, telegramUsername, createdAt, updatedAt);
    }

    @Override
    public void clearTelegramUsernameForOtherUsers(String telegramUsername, long telegramUserId, Instant updatedAt) {
        userJpaRepository.clearTelegramUsernameForOtherUsers(telegramUsername, telegramUserId, updatedAt);
    }

    @Override
    public User save(User user) {
        var saved = userJpaRepository.save(userEntityMapper.toEntity(user));
        return userEntityMapper.toUser(saved);
    }

    private UserSlice toSlice(Page<UserEntity> result) {
        return new UserSlice(
                userEntityMapper.toUsers(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }
}
