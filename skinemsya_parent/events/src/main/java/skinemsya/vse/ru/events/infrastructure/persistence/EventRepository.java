package skinemsya.vse.ru.events.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skinemsya.vse.ru.events.domain.EventStatus;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    Optional<EventEntity> findByInviteCode(String inviteCode);

    List<EventEntity> findByGroupIdOrderByCreatedAtDesc(long groupId);

    Page<EventEntity> findByGroupIdOrderByCreatedAtDesc(long groupId, Pageable pageable);

    @Query(
            """
            SELECT e FROM EventEntity e
            WHERE e.groupId IN (
                SELECT g.id FROM GroupEntity g
                WHERE g.type = skinemsya.vse.ru.groups.domain.GroupType.ONE_OFF
                AND g.id IN (SELECT gm.groupId FROM GroupMemberEntity gm WHERE gm.userId = :userId)
            )
            ORDER BY e.createdAt DESC
            """)
    Page<EventEntity> findOneOffByMemberUserId(@Param("userId") long userId, Pageable pageable);

    boolean existsByGroupIdAndDeletedAtIsNull(long groupId);

    boolean existsByGroupIdAndDeletedAtIsNullAndStatusNot(long groupId, EventStatus status);

    boolean existsByGroupIdAndPayerId(long groupId, long payerId);

    List<EventEntity> findByGroupIdAndStatusAndDeletedAtIsNull(long groupId, EventStatus status);

    List<EventEntity> findByGroupIdAndDeletedAtIsNullAndStatusIn(long groupId, Collection<EventStatus> statuses);
}
