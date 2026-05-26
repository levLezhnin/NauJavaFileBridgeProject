package ru.LevLezhnin.NauJava.repository.search.user;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.model.User;

@Component
public class FindByRoleStrategy implements UserSearchStrategy {
    @Override
    public String getCriteriaKey() {
        return "role";
    }

    @Override
    public Specification<User> getSpecification(String searchValue) {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("role")), "%" + searchValue.toLowerCase() + "%");
        };
    }
}
