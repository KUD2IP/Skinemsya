package skinemsya.vse.ru.impl.repository.users;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skinemsya.vse.ru.data.entity.users.UserProfileEntity;

/** Spring Data репозиторий профилей пользователей. */
public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, Long> {

    Optional<UserProfileEntity> findByUserId(long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
            INSERT INTO user_profiles (user_id)
            VALUES (:userId)
            ON CONFLICT (user_id) DO NOTHING
            """,
            nativeQuery = true)
    int insertIfAbsent(@Param("userId") long userId);
}
