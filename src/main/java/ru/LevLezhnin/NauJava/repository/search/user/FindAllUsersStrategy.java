package ru.LevLezhnin.NauJava.repository.search.user;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.model.User;

@Component
public class FindAllUsersStrategy implements UserSearchStrategy {
    @Override
    public String getCriteriaKey() {
        return "all";
    }

    @Override
    public Specification<User> getSpecification(String searchValue) {
        return Specification.unrestricted();
    }
}
