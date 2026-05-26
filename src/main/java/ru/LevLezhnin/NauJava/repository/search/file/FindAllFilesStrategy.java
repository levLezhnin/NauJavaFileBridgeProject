package ru.LevLezhnin.NauJava.repository.search.file;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.model.File;

@Component
public class FindAllFilesStrategy implements FileSearchStrategy {
    @Override
    public String getCriteriaKey() {
        return "all";
    }

    @Override
    public Specification<File> getSpecification(String searchValue) {
        return Specification.unrestricted();
    }
}
