package skinemsya.vse.ru.groups.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

    Optional<GroupEntity> findByTelegramChatId(Long telegramChatId);

    List<GroupEntity> findByIdIn(List<Long> ids);

    @Query("""
            SELECT g FROM GroupEntity g
            WHERE g.id IN (SELECT gm.groupId FROM GroupMemberEntity gm WHERE gm.userId = :userId)
            """)
    Page<GroupEntity> findAllByMemberUserId(@Param("userId") long userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO groups (name, type, telegram_chat_id, owner_id, created_at, updated_at)
            VALUES (:name, 'CHAT_LINKED', :telegramChatId, :ownerId, :createdAt, :updatedAt)
            ON CONFLICT (telegram_chat_id) DO NOTHING
            """, nativeQuery = true)
    int insertChatLinkedGroupIfAbsent(
            @Param("name") String name,
            @Param("telegramChatId") long telegramChatId,
            @Param("ownerId") long ownerId,
            @Param("createdAt") Instant createdAt,
            @Param("updatedAt") Instant updatedAt
    );
}
