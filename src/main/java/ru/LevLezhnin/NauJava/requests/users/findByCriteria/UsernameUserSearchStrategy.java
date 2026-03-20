package ru.LevLezhnin.NauJava.requests.users.findByCriteria;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.model.User;

@Component
public class UsernameUserSearchStrategy implements UserSearchStrategy {
    @Override
    public String getCriteriaKey() {
        return "username";
    }

    @Override
    public Specification<User> getSpecification(String searchValue) {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("username")), "%" + searchValue.toLowerCase() + "%");
        };
    }
}
