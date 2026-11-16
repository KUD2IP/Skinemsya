package skinemsya.vse.ru.impl.rest;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.files.FileUploadPurpose;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.security.AuthenticatedUser;
import skinemsya.vse.ru.api.internal.service.files.FileService;
import skinemsya.vse.ru.api.rest.files.dto.FileResponse;
import skinemsya.vse.ru.impl.mapper.rest.FileRestMapper;

/** REST API загрузки и скачивания файлов. */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileRestMapper fileRestMapper;

    /** Загружает файл. Назначение по умолчанию — чек. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileResponse upload(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "receipt") String purpose) {
        long userId = requireUserId(authenticatedUser);
        try (InputStream content = file.getInputStream()) {
            return fileRestMapper.toResponse(fileService.upload(
                    userId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    content,
                    FileUploadPurpose.from(purpose)));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded file", ex);
        }
    }

    /** Возвращает метаданные файла. */
    @GetMapping("/{fileId}")
    public FileResponse getFile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long fileId,
            @RequestParam(defaultValue = "false") boolean sharedAccess) {
        long userId = requireUserId(authenticatedUser);
        var stored = fileService.requireById(fileId);
        fileService.requireOwnerOrShared(fileId, userId, sharedAccess || stored.getOwnerId() == userId);
        return fileRestMapper.toResponse(stored);
    }

    /** Возвращает содержимое файла. */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<InputStreamResource> getContent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long fileId,
            @RequestParam(defaultValue = "false") boolean sharedAccess) {
        long userId = requireUserId(authenticatedUser);
        var stored = fileService.requireById(fileId);
        var stream = fileService.getContent(fileId, userId, sharedAccess || stored.getOwnerId() == userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, stored.getMimeType())
                .body(new InputStreamResource(stream));
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
