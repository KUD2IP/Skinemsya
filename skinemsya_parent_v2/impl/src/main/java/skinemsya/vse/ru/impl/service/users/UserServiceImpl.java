package skinemsya.vse.ru.impl.service.users;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.pojo.users.PaymentDetails;
import skinemsya.vse.ru.api.internal.pojo.users.TelegramUserData;
import skinemsya.vse.ru.api.internal.pojo.users.User;
import skinemsya.vse.ru.api.internal.pojo.users.UserProfile;
import skinemsya.vse.ru.api.internal.pojo.users.UserSlice;
import skinemsya.vse.ru.api.internal.repository.users.UserProfileRepository;
import skinemsya.vse.ru.api.internal.repository.users.UserRepository;
import skinemsya.vse.ru.api.internal.service.users.UserService;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.impl.support.util.TelegramUsernameNormalizer;

/** JPA-реализация операций с пользователями. */
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(long userId) {
        return userRepository.findById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByTelegramUserId(long telegramUserId) {
        return userRepository.findByTelegramUserId(telegramUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByTelegramUsername(String telegramUsername) {
        var normalized = TelegramUsernameNormalizer.normalize(telegramUsername);
        if (normalized == null) {
            return Optional.empty();
        }
        return userRepository.findByTelegramUsername(normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSlice searchOthers(long excludeUserId, String usernameQuery, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 30);
        String query = usernameQuery == null ? "" : usernameQuery.trim();
        if (query.startsWith("@")) {
            query = query.substring(1).trim();
        }
        return query.isEmpty()
                ? userRepository.findAllExcept(excludeUserId, safePage, safeSize)
                : userRepository.findByUsernameExcept(excludeUserId, query, safePage, safeSize);
    }

    @Override
    public User upsertFromTelegram(TelegramUserData telegramUserData) {
        var now = Instant.now();
        var existing = userRepository.findByTelegramUserId(telegramUserData.getTelegramUserId());
        var normalizedUsername = TelegramUsernameNormalizer.normalize(telegramUserData.getTelegramUsername());
        if (normalizedUsername != null) {
            userRepository.clearTelegramUsernameForOtherUsers(
                    normalizedUsername, telegramUserData.getTelegramUserId(), now);
        }

        User user;
        if (existing.isPresent()) {
            user = existing.get();
            updateTelegramUser(user, telegramUserData, now);
        } else {
            userRepository.insertTelegramUserIfAbsent(
                    telegramUserData.getTelegramUserId(), telegramUserData.getDisplayName(), null, now, now);
            user = userRepository
                    .findByTelegramUserId(telegramUserData.getTelegramUserId())
                    .orElseThrow(() -> new DomainException(ErrorCode.INTERNAL_ERROR, "Telegram user was not created"));
            updateTelegramUser(user, telegramUserData, now);
        }

        user = userRepository.save(user);
        userProfileRepository.insertIfAbsent(user.getId());
        return user;
    }

    private static void updateTelegramUser(User user, TelegramUserData telegramUserData, Instant updatedAt) {
        user.setDisplayName(telegramUserData.getDisplayName());
        var normalizedUsername = TelegramUsernameNormalizer.normalize(telegramUserData.getTelegramUsername());
        if (normalizedUsername != null) {
            user.setTelegramUsername(normalizedUsername);
        }
        var photoUrl = normalizePhotoUrl(telegramUserData.getPhotoUrl());
        if (photoUrl != null) {
            user.setPhotoUrl(photoUrl);
        }
        user.setUpdatedAt(updatedAt);
    }

    static String normalizePhotoUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        var trimmed = raw.trim();
        if (trimmed.length() > 1024 || !trimmed.startsWith("https://")) {
            return null;
        }
        return trimmed;
    }

    @Override
    public UserProfile updateProfile(
            long userId, String paymentDetails, String phone, String preferredBank, String notificationSettings) {
        var profile = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User profile not found"));

        profile.setPaymentDetails(paymentDetails != null ? paymentDetails : profile.getPaymentDetails());
        profile.setPhone(phone != null ? phone : profile.getPhone());
        profile.setPreferredBank(
                preferredBank != null ? (preferredBank.isBlank() ? null : preferredBank) : profile.getPreferredBank());
        profile.setNotificationSettings(
                notificationSettings != null ? notificationSettings : profile.getNotificationSettings());
        return userProfileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetails getPaymentDetails(long userId) {
        var profile = userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User profile not found"));
        return new PaymentDetails(profile.getPaymentDetails(), profile.getPhone(), profile.getPreferredBank());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getProfile(long userId) {
        return userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User profile not found"));
    }
}
