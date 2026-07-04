package skinemsya.vse.ru.events.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.events.domain.EventStatus;

import java.util.Collection;
import java.util.List;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByGroupIdOrderByCreatedAtDesc(long groupId);

    Page<EventEntity> findByGroupIdOrderByCreatedAtDesc(long groupId, Pageable pageable);

    boolean existsByGroupIdAndDeletedAtIsNull(long groupId);

    boolean existsByGroupIdAndDeletedAtIsNullAndStatusNot(long groupId, EventStatus status);

    List<EventEntity> findByGroupIdAndStatusAndDeletedAtIsNull(long groupId, EventStatus status);

    List<EventEntity> findByGroupIdAndDeletedAtIsNullAndStatusIn(long groupId, Collection<EventStatus> statuses);
}
