package skinemsya.vse.ru.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByTelegramUserId(long telegramUserId);

    Optional<UserEntity> findByTelegramUsernameIgnoreCase(String telegramUsername);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO users (telegram_user_id, display_name, telegram_username, created_at, updated_at)
            VALUES (:telegramUserId, :displayName, :telegramUsername, :createdAt, :updatedAt)
            ON CONFLICT (telegram_user_id) DO NOTHING
            """, nativeQuery = true)
    int insertTelegramUserIfAbsent(
            @Param("telegramUserId") long telegramUserId,
            @Param("displayName") String displayName,
            @Param("telegramUsername") String telegramUsername,
            @Param("createdAt") Instant createdAt,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE users
            SET telegram_username = NULL,
                updated_at = :updatedAt
            WHERE lower(telegram_username) = lower(:telegramUsername)
              AND telegram_user_id <> :telegramUserId
            """, nativeQuery = true)
    int clearTelegramUsernameForOtherUsers(
            @Param("telegramUsername") String telegramUsername,
            @Param("telegramUserId") long telegramUserId,
            @Param("updatedAt") Instant updatedAt
    );
}
