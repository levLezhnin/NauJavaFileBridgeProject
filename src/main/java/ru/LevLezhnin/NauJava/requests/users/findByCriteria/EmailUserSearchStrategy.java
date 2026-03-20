package ru.LevLezhnin.NauJava.requests.users.findByCriteria;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.model.User;

@Component
public class EmailUserSearchStrategy implements UserSearchStrategy {
    @Override
    public String getCriteriaKey() {
        return "email";
    }

    @Override
    public Specification<User> getSpecification(String searchValue) {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("email")), searchValue.toLowerCase());
        };
    }
}
