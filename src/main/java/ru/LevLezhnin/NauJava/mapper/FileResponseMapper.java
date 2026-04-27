package ru.LevLezhnin.NauJava.mapper;

import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.dto.file.FileResponseDto;
import ru.LevLezhnin.NauJava.model.File;

@Component
public class FileResponseMapper implements Mapper<File, FileResponseDto> {
    @Override
    public FileResponseDto map(File object) {
        return new FileResponseDto(
                object.getId().toString(),
                object.getName(),
                object.getUploadedAt(),
                object.getExpireAt(),
                object.getFileStatistics().getTimesDownloaded(),
                object.getMaxDownloads(),
                object.hasPassword()
        );
    }
}
