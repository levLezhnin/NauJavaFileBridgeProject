package ru.LevLezhnin.NauJava.repository.user.search;

import org.springframework.data.jpa.domain.Specification;
import ru.LevLezhnin.NauJava.model.User;

public interface UserSearchStrategy {
    String getCriteriaKey();
    Specification<User> getSpecification(String searchValue);
}
