package ru.LevLezhnin.NauJava.repository.custom;

import java.io.InputStream;

/**
 * Абстракция над объектным хранилищем (MinIO / S3-совместимое).
 * <p>
 * Используется FileService и Batch Job очистки для работы с бинарными данными файлов.
 *
 * @author Лев Лежнин
 */
public interface ObjectStorageRepository {

    /**
     * Загружает (или перезаписывает) объект по указанному пути.
     * <p>
     * <b>Контракт реализации (MinioStorageRepositoryImpl):</b>
     * <ul>
     *   <li>Валидирует: {@code path} не пустой, {@code inputStream != null}, {@code size > 0}</li>
     *   <li>Если {@code contentType} невалидный - логирует предупреждение и использует {@code application/octet-stream}</li>
     *   <li><b>Всегда</b> закрывает переданный {@code InputStream} в блоке finally (даже при ошибке)</li>
     *   <li>При любой ошибке MinIO - выбрасывает {@link ru.LevLezhnin.NauJava.exception.file.FileStorageException} (uploadFailed)</li>
     * </ul>
     *
     * @param path        уникальный путь/ключ объекта в бакете (например "/files/userId=1/fileId=xxx/data.bin")
     * @param inputStream поток с данными (владелец потока - вызывающий код)
     * @param size        точный размер в байтах
     * @param contentType MIME-тип (может быть null)
     * @throws IllegalArgumentException если нарушены preconditions
     * @throws ru.LevLezhnin.NauJava.exception.file.FileStorageException при ошибках загрузки
     */
    void uploadWithPath(String path, InputStream inputStream, long size, String contentType);

    /**
     * Возвращает InputStream для скачивания объекта.
     * <p>
     * <b>Контракт:</b>
     * <ul>
     *   <li>Возвращает "живой" InputStream из объектного хранилища - его нужно закрыть после использования</li>
     *   <li>При отсутствии объекта (NoSuchKey и аналоги) - {@link ru.LevLezhnin.NauJava.exception.file.FileStorageException} (fileNotFound)</li>
     *   <li>Другие ошибки объектного хранилища - downloadFailed</li>
     * </ul>
     *
     * @param path путь к объекту
     * @return InputStream содержимого
     * @throws ru.LevLezhnin.NauJava.exception.file.FileStorageException если файл не найден или произошла ошибка чтения
     */
    InputStream downloadByPath(String path);

    /**
     * Проверяет существование объекта по пути.
     * <p>
     * <b>Контракт:</b>
     * <ul>
     *   <li>Использует HEAD-запрос (statObject)</li>
     *   <li>При "не найден" (NoSuchKey / NoSuchBucket / ResourceNotFound) - возвращает {@code false}</li>
     *   <li>При других ошибках MinIO - выбрасывает {@link ru.LevLezhnin.NauJava.exception.file.FileStorageException}</li>
     * </ul>
     *
     * @param path путь к объекту
     * @return {@code true} если объект существует
     */
    boolean fileExistsByPath(String path);

    /**
     * Возвращает размер объекта в байтах.
     * <p>
     * <b>Контракт:</b>
     * <ul>
     *   <li>При отсутствии объекта - FileStorageException (fileNotFound)</li>
     *   <li>При других ошибках - FileStorageException</li>
     * </ul>
     *
     * @param path путь к объекту
     * @return размер в байтах
     * @throws ru.LevLezhnin.NauJava.exception.file.FileStorageException если файл не найден или ошибка метаданных
     */
    long findFileSizeBytesByPath(String path);

    /**
     * Удаляет один объект по пути.
     * <p>
     * <b>Контракт (идемпотентное поведение):</b>
     * <ul>
     *   <li>Если объект не существует - молча возвращается (логирует warning)</li>
     *   <li>При других ошибках объектного хранилища - FileStorageException (deleteFailed)</li>
     * </ul>
     *
     * @param path путь к объекту
     */
    void deleteByPath(String path);

    /**
     * Пакетное удаление объектов.
     * <p>
     * <b>Контракт реализации:</b>
     * <ul>
     *   <li>Null или пустой список - молча игнорируется (warning / debug)</li>
     *   <li>Null/blank пути внутри списка фильтруются</li>
     *   <li>Отдельные ошибки "файл не найден" игнорируются</li>
     *   <li>Реальные ошибки удаления считаются и логируются</li>
     *   <li>При критической ошибке всего batch - FileStorageException (deleteFailed)</li>
     * </ul>
     * Используется Batch Job'ом очистки просроченных файлов.
     *
     * @param paths коллекция путей (может содержать null/дубликаты)
     */
    void deleteAllByPathsInBatch(Iterable<String> paths);
}
