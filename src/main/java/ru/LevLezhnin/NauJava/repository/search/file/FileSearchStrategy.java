package ru.LevLezhnin.NauJava.repository.search.file;

import org.springframework.data.jpa.domain.Specification;
import ru.LevLezhnin.NauJava.model.File;

/**
 * Стратегия построения JPA Specification для поиска файлов администратором.
 * <p>
 * Реализации регистрируются в Map по ключу критерия.
 *
 * @author Лев Лежнин
 * @see ru.LevLezhnin.NauJava.controller.admin.FileAdminController
 */
public interface FileSearchStrategy {

    /**
     * Возвращает строковый ключ, по которому стратегия регистрируется в Map.
     * <p>
     * Этот ключ передаётся в контроллер через параметр {@code searchBy}.
     *
     * @return уникальный идентификатор стратегии (например "authorId", "fileName")
     */
    String getCriteriaKey();

    /**
     * Строит JPA Specification по переданному значению поиска.
     * <p>
     * Реализация решает, как именно искать (по точному id, по like и т.д.).
     *
     * @param searchValue значение из запроса администратора
     * @return Specification для фильтрации файлов
     */
    Specification<File> getSpecification(String searchValue);
}
