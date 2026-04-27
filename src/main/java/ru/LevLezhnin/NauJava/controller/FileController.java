package ru.LevLezhnin.NauJava.controller;

import jakarta.validation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;
import ru.LevLezhnin.NauJava.dto.file.*;
import ru.LevLezhnin.NauJava.exceptions.FileUploadException;
import ru.LevLezhnin.NauJava.service.interfaces.FileService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;
    private final Validator validator;

    @Autowired
    public FileController(FileService fileService, Validator validator) {
        this.fileService = fileService;
        this.validator = validator;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponseDto uploadFile(@RequestPart("file") MultipartFile multipartFile,
                                      @Valid @RequestPart("payload") FileUploadWebRequestPayloadDto fileUploadWebRequestPayloadDto) {
        FileUploadRequestDto fileUploadRequestDto = new FileUploadRequestDto(
                multipartFile.getOriginalFilename(),
                multipartFile.getContentType(),
                multipartFile.getSize(),
                fileUploadWebRequestPayloadDto.ttlMinutes(),
                fileUploadWebRequestPayloadDto.maxDownloads(),
                fileUploadWebRequestPayloadDto.password());

        Set<ConstraintViolation<FileUploadRequestDto>> violations = validator.validate(fileUploadRequestDto);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        try (InputStream fileDataInputStream = multipartFile.getInputStream()) {
            return fileService.uploadFile(
                    fileUploadRequestDto,
                    fileDataInputStream);
        } catch (IOException e) {
            log.error("Не удалось прочитать данные загружаемого файла", e);
            throw new FileUploadException("Ошибка чтения загруженного файла", e);
        }
    }

    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public List<FileResponseDto> getUserFiles(@RequestParam("page") int page,
                                              @RequestParam("page_size") int pageSize) {
        return fileService.getCurrentUserFiles(page, pageSize);
    }

    @GetMapping("/{fileId}")
    @ResponseStatus(HttpStatus.OK)
    public FileResponseDto getFileDataById(@PathVariable String fileId) {
        return fileService.getById(fileId);
    }

    @GetMapping("/link/{fileId}")
    @ResponseStatus(HttpStatus.OK)
    public FileDownloadLinkResponseDto getFileDownloadLinkById(@PathVariable String fileId) {
        return fileService.formDownloadLinkPath(fileId);
    }

    @PostMapping("/download")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Resource> downloadFileById(@RequestBody FileDownloadRequestDto fileDownloadRequestDto) {
        FileDownloadResponseDto fileDownloadResponseDto = fileService.downloadById(fileDownloadRequestDto);

        String encodedFilename = UriUtils.encode(fileDownloadResponseDto.originalFilename(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileDownloadResponseDto.contentType()))
                .contentLength(fileDownloadResponseDto.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s\"; filename*=UTF-8''%s".formatted(fileDownloadResponseDto.originalFilename(), encodedFilename))
                .body(new InputStreamResource(fileDownloadResponseDto.fileDataInputStream()));
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFileById(@PathVariable String fileId) {
        fileService.deleteById(fileId);
    }
}
