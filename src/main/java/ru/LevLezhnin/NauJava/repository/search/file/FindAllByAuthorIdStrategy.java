package ru.LevLezhnin.NauJava.repository.search.file;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.model.File;

@Component
public class FindAllByAuthorIdStrategy implements FileSearchStrategy {
    @Override
    public String getCriteriaKey() {
        return "authorId";
    }

    @Override
    public Specification<File> getSpecification(String searchValue) {

        long authorId;

        try {
            authorId = Long.parseLong(searchValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID автора файла должен быть целым числом");
        }

        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.equal(root.get("author").get("id"), authorId);
        };
    }
}
