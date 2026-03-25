package ru.LevLezhnin.NauJava.repository.custom;

import java.io.InputStream;

public interface FileStorageRepository {

    /**
     * Загружает файл по пути
     * @param path путь до папки файла
     * @param inputStream поток ввода с данными файла
     * @param size размер файла
     * @param contentType расширение файла
     */
    void uploadWithPath(String path, InputStream inputStream, long size, String contentType);

    /**
     * Скачивает файл как InputStream по пути path
     * @param path путь до файла
     * @return InputStream данных из файла
     */
    InputStream downloadByPath(String path);

    /**
     * Проверяет существует ли файл по пути path
     * @param path путь до файла
     * @return true, если файл существует. false - иначе
     */
    boolean fileExistsByPath(String path);

    /**
     * Возвращает размер файла по пути path в байтах
     * @param path путь до файла
     * @return размер файла в байтах
     */
    long findFileSizeBytesByPath(String path);

    /**
     * Удаляет файл по пути path
     * @param path путь до файла
     */
    void deleteByPath(String path);
}
