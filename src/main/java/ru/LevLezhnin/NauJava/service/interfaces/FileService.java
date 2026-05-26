package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.dto.file.*;

import java.io.InputStream;
import java.util.List;

/**
 * Сервис для управления файлами пользователей.
 * <p>
 * Основные возможности:
 * <ul>
 *   <li>Загрузка файлов с параметрами TTL и maxDownloads</li>
 *   <li>Получение списка файлов пользователя</li>
 *   <li>Административный поиск и просмотр всех файлов</li>
 *   <li>Формирование ссылок для скачивания</li>
 *   <li>Скачивание с проверкой пароля и лимитов</li>
 *   <li>Удаление файлов</li>
 * </ul>
 *
 * @author Лев Лежнин
 */
public interface FileService {

    /**
     * Загружает файл текущего пользователя в хранилище.
     * <p>
     * Контракт:
     * <ul>
     *   <li>Проверяет и списывает квоту <b>до</b> загрузки в объектное хранилище</li>
     *   <li>При ошибке сохранения метаданных - файл из объектного хранилища удаляется (best-effort cleanup)</li>
     *   <li>Генерирует UUID, путь в хранилище, хеш пароля (если указан)</li>
     *   <li>expireAt = now + ttlMinutes</li>
     *   <li>Возвращает DTO с метаданными (без содержимого)</li>
     * </ul>
     *
     * @param fileUploadRequestDto метаданные (имя, размер, contentType, ttl, maxDownloads, password)
     * @param fileDataStream       поток с содержимым файла (не закрывается сервисом)
     * @return метаданные загруженного файла
     * @throws ru.LevLezhnin.NauJava.exception.storagequotas.StorageQuotaExceededException если не хватает квоты
     */
    FileResponseDto uploadFile(FileUploadRequestDto fileUploadRequestDto, InputStream fileDataStream);

    /**
     * Возвращает постраничный список файлов <b>текущего</b> аутентифицированного пользователя.
     * <p>
     * Сортировка: по дате загрузки убыв.
     *
     * @param page     номер страницы (0-based)
     * @param pageSize размер страницы
     */
    List<FileResponseDto> getCurrentUserFiles(int page, int pageSize);

    /**
     * Административный поиск всех файлов в системе с поддержкой различных критериев.
     * <p>
     * Контракт:
     * <ul>
     *   <li>Требует роль ADMIN (иначе AccessDeniedException)</li>
     *   <li>{@code searchBy} должен соответствовать зарегистрированной {@link ru.LevLezhnin.NauJava.repository.search.file.FileSearchStrategy}</li>
     *   <li>При неизвестном критерии - {@link ru.LevLezhnin.NauJava.exception.common.InvalidSearchCriteriaException}</li>
     * </ul>
     *
     * @param searchBy    ключ стратегии поиска ("authorId", "fileName" и т.д.)
     * @param searchValue значение для поиска
     * @param page        страница
     * @param pageSize    размер страницы
     */
    List<FileAdminResponseDto> getAllFiles(String searchBy, String searchValue, int page, int pageSize);

    /**
     * Возвращает метаданные файла по его UUID.
     *
     * @param fileId UUID файла
     * @throws ru.LevLezhnin.NauJava.exception.file.FileNotFoundException если файл не найден
     */
    FileResponseDto getById(String fileId);

    /**
     * Формирует публичную ссылку на скачивание файла <b>только для его владельца</b>.
     * <p>
     * Контракт (строгий):
     * <ul>
     *   <li>Проверяет, что текущий пользователь - автор файла</li>
     *   <li>Проверяет, что файл не просрочен и лимит скачиваний не исчерпан</li>
     *   <li>Проверяет физическое существование файла в объектном хранилище</li>
     *   <li>При нарушении любого условия - соответствующее исключение (FileExpired, DownloadLimitExceeded, IllegalFileAccess и т.д.)</li>
     * </ul>
     *
     * @param fileId UUID файла
     * @return объект со сформированной полной ссылкой
     */
    FileDownloadLinkResponseDto formDownloadLinkPath(String fileId);

    /**
     * Выполняет скачивание файла с проверкой всех ограничений.
     * <p>
     * Контракт (критически важный):
     * <ul>
     *   <li>Использует <b>PESSIMISTIC_WRITE</b> lock + условное обновление счётчика (атомарность)</li>
     *   <li>Проверяет существование в объектном хранилище, срок жизни, лимит скачиваний</li>
     *   <li>При наличии пароля - проверяет его (InvalidPasswordException)</li>
     *   <li>При одновременном скачивании несколькими клиентами - только один успешно увеличит счётчик</li>
     *   <li>Возвращает InputStream содержимого (ответственность за закрытие на вызывающем коде)</li>
     * </ul>
     *
     * @param fileDownloadRequestDto id файла + опциональный пароль
     * @return DTO с потоком, именем, mime и размером
     */
    FileDownloadResponseDto downloadById(FileDownloadRequestDto fileDownloadRequestDto);

    /**
     * Удаляет файл.
     * <p>
     * Контракт:
     * <ul>
     *   <li>Доступно владельцу или администратору</li>
     *   <li>Использует pessimistic lock</li>
     *   <li>Возвращает квоту автору файла</li>
     *   <li>Удаление из объектного хранилища - best-effort (ошибка логируется, но не прерывает операцию)</li>
     * </ul>
     *
     * @param fileId UUID файла
     */
    void deleteById(String fileId);
}
