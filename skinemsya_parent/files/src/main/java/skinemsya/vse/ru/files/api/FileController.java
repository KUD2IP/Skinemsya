package skinemsya.vse.ru.files.api;

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
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.files.api.dto.FileResponse;
import skinemsya.vse.ru.files.application.FileService;
import skinemsya.vse.ru.files.application.FileUploadPurpose;
import skinemsya.vse.ru.files.domain.StoredFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileResponse upload(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "receipt") String purpose
    ) {
        long userId = requireUserId(authenticatedUser);
        return toResponse(fileService.upload(userId, file, FileUploadPurpose.from(purpose)));
    }

    @GetMapping("/{fileId}")
    public FileResponse getFile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long fileId
    ) {
        long userId = requireUserId(authenticatedUser);
        var stored = fileService.requireById(fileId);
        fileService.requireOwnerOrShared(fileId, userId, stored.ownerId() == userId);
        return toResponse(stored);
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<InputStreamResource> getContent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable long fileId,
            @RequestParam(defaultValue = "false") boolean sharedAccess
    ) {
        long userId = requireUserId(authenticatedUser);
        var stored = fileService.requireById(fileId);
        var stream = fileService.getContent(fileId, userId, sharedAccess || stored.ownerId() == userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, stored.mimeType())
                .body(new InputStreamResource(stream));
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }

    private static FileResponse toResponse(StoredFile file) {
        return new FileResponse(
                file.id(),
                file.originalName(),
                file.mimeType(),
                file.sizeBytes(),
                file.createdAt()
        );
    }
}
