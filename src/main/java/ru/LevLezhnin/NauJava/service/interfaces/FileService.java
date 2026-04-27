package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.dto.file.*;

import java.io.InputStream;
import java.util.List;

public interface FileService {
    FileResponseDto uploadFile(FileUploadRequestDto fileUploadRequestDto, InputStream fileDataStream);
    List<FileResponseDto> getCurrentUserFiles(int page, int pageSize);
    FileResponseDto getById(String fileId);

    /**
     * @param fileId id файла
     * @return полная ссылка на скачивание файла с id = fileId
     */
    FileDownloadLinkResponseDto formDownloadLinkPath(String fileId);
    FileDownloadResponseDto downloadById(FileDownloadRequestDto fileDownloadRequestDto);
    void deleteById(String fileId);
}
