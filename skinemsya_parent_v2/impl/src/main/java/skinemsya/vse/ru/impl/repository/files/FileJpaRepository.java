package skinemsya.vse.ru.impl.repository.files;

import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.data.entity.files.FileEntity;

/** Spring Data репозиторий файлов. */
public interface FileJpaRepository extends JpaRepository<FileEntity, Long> {}
