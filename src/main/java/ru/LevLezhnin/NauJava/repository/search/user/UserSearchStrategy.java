package ru.LevLezhnin.NauJava.repository.search.user;

import org.springframework.data.jpa.domain.Specification;
import ru.LevLezhnin.NauJava.model.User;

/**
 * Стратегия построения JPA Specification для административного поиска пользователей.
 *
 * @author Лев Лежнин
 */
public interface UserSearchStrategy {

    /**
     * Возвращает строковый ключ стратегии (используется как значение параметра searchBy в админ-поиске).
     */
    String getCriteriaKey();

    /**
     * Строит условие фильтрации пользователей на основе переданного значения.
     *
     * @param searchValue строка поиска от администратора
     * @return Specification для JpaSpecificationExecutor
     */
    Specification<User> getSpecification(String searchValue);
}
