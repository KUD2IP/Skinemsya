package skinemsya.vse.ru.groups.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skinemsya.vse.ru.groups.domain.GroupRole;

public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, Long> {

    Optional<GroupMemberEntity> findByGroupIdAndUserId(long groupId, long userId);

    List<GroupMemberEntity> findByUserId(long userId);

    List<GroupMemberEntity> findByGroupId(long groupId);

    boolean existsByGroupIdAndUserId(long groupId, long userId);

    @Query(
            """
            SELECT gm FROM GroupMemberEntity gm
            WHERE gm.groupId = :groupId
            ORDER BY CASE WHEN gm.role = :ownerRole THEN 0 ELSE 1 END ASC, gm.joinedAt ASC
            """)
    Page<GroupMemberEntity> findByGroupIdOrdered(
            @Param("groupId") long groupId, @Param("ownerRole") GroupRole ownerRole, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
            INSERT INTO group_members (group_id, user_id, role, joined_at)
            VALUES (:groupId, :userId, :role, :joinedAt)
            ON CONFLICT (group_id, user_id) DO NOTHING
            """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("groupId") long groupId,
            @Param("userId") long userId,
            @Param("role") String role,
            @Param("joinedAt") Instant joinedAt);
}
