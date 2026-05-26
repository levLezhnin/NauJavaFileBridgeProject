package ru.LevLezhnin.NauJava.repository.search.user;

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

        long userId;

        try {
            userId = Long.parseLong(searchValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID пользователя должен быть целым числом");
        }

        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.equal(root.get("id"), userId);
        };
    }
}
