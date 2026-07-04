package skinemsya.vse.ru.receipts.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<ReceiptEntity, Long> {

    List<ReceiptEntity> findByEventIdOrderByCreatedAtDesc(long eventId);

    List<ReceiptEntity> findByFileId(long fileId);

    Optional<ReceiptEntity> findByIdAndEventId(long id, long eventId);
}
