package ru.LevLezhnin.NauJava.repository.user.search;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.model.User;

@Component
public class FindByIdStrategy implements UserSearchStrategy {
    @Override
    public String getCriteriaKey() {
        return "id";
    }

    @Override
    public Specification<User> getSpecification(String searchValue) {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.equal(root.get("id"), Long.parseLong(searchValue));
        };
    }
}
