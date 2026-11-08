package skinemsya.vse.ru.receipts.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.receipts.domain.ReceiptStatus;

public interface ReceiptRepository extends JpaRepository<ReceiptEntity, Long> {

    List<ReceiptEntity> findByEventIdOrderByCreatedAtDesc(long eventId);

    List<ReceiptEntity> findByFileId(long fileId);

    Optional<ReceiptEntity> findByIdAndEventId(long id, long eventId);

    Optional<ReceiptEntity> findFirstByImageSha256AndStatusAndMlRawJsonIsNotNullOrderByIdDesc(
            String imageSha256, ReceiptStatus status);
}
