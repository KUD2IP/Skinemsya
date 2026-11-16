package skinemsya.vse.ru.impl.repository.files;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.pojo.files.StoredFile;
import skinemsya.vse.ru.api.internal.repository.files.FileRepository;
import skinemsya.vse.ru.impl.mapper.entity.FileEntityMapper;

/** Адаптер между внутренним портом файлов и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class FileRepositoryImpl implements FileRepository {

    private final FileJpaRepository fileJpaRepository;
    private final FileEntityMapper fileEntityMapper;

    @Override
    public Optional<StoredFile> findById(long fileId) {
        return fileJpaRepository.findById(fileId).map(fileEntityMapper::toStoredFile);
    }

    @Override
    public StoredFile save(StoredFile file) {
        var entity = fileEntityMapper.toEntity(file);
        if (file.getId() == 0L) {
            entity.setId(null);
        }
        return fileEntityMapper.toStoredFile(fileJpaRepository.save(entity));
    }

    @Override
    public void delete(long fileId) {
        fileJpaRepository.deleteById(fileId);
    }
}
