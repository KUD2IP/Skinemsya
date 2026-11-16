package skinemsya.vse.ru.impl.repository.events;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.data.entity.events.EventEntity;

/** Spring Data репозиторий сборов. */
public interface EventJpaRepository extends JpaRepository<EventEntity, Long> {

    Optional<EventEntity> findByInviteCode(String inviteCode);

    List<EventEntity> findByGroupIdOrderByCreatedAtDesc(long groupId);

    Page<EventEntity> findByGroupIdOrderByCreatedAtDesc(long groupId, Pageable pageable);

    boolean existsByGroupIdAndDeletedAtIsNull(long groupId);

    boolean existsByGroupIdAndDeletedAtIsNullAndStatusNot(long groupId, EventStatus status);

    boolean existsByGroupIdAndPayerId(long groupId, long payerId);

    List<EventEntity> findByGroupIdAndStatusAndDeletedAtIsNull(long groupId, EventStatus status);

    List<EventEntity> findByGroupIdAndDeletedAtIsNullAndStatusIn(long groupId, Collection<EventStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE EventEntity e
            SET e.deletedAt = :deletedAt, e.updatedAt = :updatedAt
            WHERE e.id = :eventId
            """)
    int markDeleted(
            @Param("eventId") long eventId,
            @Param("deletedAt") Instant deletedAt,
            @Param("updatedAt") Instant updatedAt);
}
