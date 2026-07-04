package skinemsya.vse.ru.files.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
}
