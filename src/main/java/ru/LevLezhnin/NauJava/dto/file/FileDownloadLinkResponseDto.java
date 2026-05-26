package ru.LevLezhnin.NauJava.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO ответа с ссылкой на страницу скачивания файла
 * @param downloadLink ссылка на страницу скачивания файла (обязательный, непустой)
 * @author Лев Лежнин
 */
public record FileDownloadLinkResponseDto(@JsonProperty("download_link") String downloadLink) {}
