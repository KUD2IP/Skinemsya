package skinemsya.vse.ru.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

    Optional<UserProfileEntity> findByUserId(long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO user_profiles (user_id)
            VALUES (:userId)
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") long userId);
}
