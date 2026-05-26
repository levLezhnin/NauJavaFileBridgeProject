package ru.LevLezhnin.NauJava.repository.search.file;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.model.File;

@Component
public class FindAllByFileNameContainsStrategy implements FileSearchStrategy {
    @Override
    public String getCriteriaKey() {
        return "fileNameContains";
    }

    @Override
    public Specification<File> getSpecification(String searchValue) {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), "%" + searchValue.toLowerCase() + "%");
        };
    }
}
