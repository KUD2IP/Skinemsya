package skinemsya.vse.ru.users.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.users.domain.PaymentDetails;
import skinemsya.vse.ru.users.domain.TelegramUserData;
import skinemsya.vse.ru.users.domain.User;
import skinemsya.vse.ru.users.domain.UserProfile;
import skinemsya.vse.ru.users.infrastructure.mapper.UserMapper;
import skinemsya.vse.ru.users.infrastructure.persistence.UserEntity;
import skinemsya.vse.ru.users.infrastructure.persistence.UserProfileRepository;
import skinemsya.vse.ru.users.infrastructure.persistence.UserRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(long userId) {
        return userRepository.findById(userId).map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByTelegramUserId(long telegramUserId) {
        return userRepository.findByTelegramUserId(telegramUserId).map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByTelegramUsername(String telegramUsername) {
        var normalized = TelegramUsernameNormalizer.normalize(telegramUsername);
        if (normalized == null) {
            return Optional.empty();
        }
        return userRepository.findByTelegramUsernameIgnoreCase(normalized).map(userMapper::toDomain);
    }

    @Override
    public User upsertFromTelegram(TelegramUserData telegramUserData) {
        var now = Instant.now();
        var existing = userRepository.findByTelegramUserId(telegramUserData.telegramUserId());
        var normalizedUsername = TelegramUsernameNormalizer.normalize(telegramUserData.telegramUsername());
        if (normalizedUsername != null) {
            userRepository.clearTelegramUsernameForOtherUsers(
                    normalizedUsername,
                    telegramUserData.telegramUserId(),
                    now
            );
        }

        UserEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            updateTelegramUser(entity, telegramUserData.displayName(), normalizedUsername, now);
        } else {
            userRepository.insertTelegramUserIfAbsent(
                    telegramUserData.telegramUserId(),
                    telegramUserData.displayName(),
                    null,
                    now,
                    now
            );
            entity = userRepository.findByTelegramUserId(telegramUserData.telegramUserId())
                    .orElseThrow(() -> new DomainException(ErrorCode.INTERNAL_ERROR, "Telegram user was not created"));
            updateTelegramUser(entity, telegramUserData.displayName(), normalizedUsername, now);
        }

        entity = userRepository.save(entity);
        userProfileRepository.insertIfAbsent(entity.getId());
        return userMapper.toDomain(entity);
    }

    private static void updateTelegramUser(
            UserEntity entity,
            String displayName,
            String normalizedUsername,
            Instant updatedAt
    ) {
        entity.setDisplayName(displayName);
        if (normalizedUsername != null) {
            entity.setTelegramUsername(normalizedUsername);
        }
        entity.setUpdatedAt(updatedAt);
    }

    @Override
    public UserProfile updateProfile(long userId, String paymentDetails, String phone, String notificationSettings) {
        var profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User profile not found"));

        profile.setPaymentDetails(paymentDetails);
        profile.setPhone(phone);
        profile.setNotificationSettings(notificationSettings);
        return userMapper.toDomain(userProfileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetails getPaymentDetails(long userId) {
        var profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User profile not found"));
        return new PaymentDetails(profile.getPaymentDetails(), profile.getPhone());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getProfile(long userId) {
        return userProfileRepository.findByUserId(userId)
                .map(userMapper::toDomain)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User profile not found"));
    }
}
