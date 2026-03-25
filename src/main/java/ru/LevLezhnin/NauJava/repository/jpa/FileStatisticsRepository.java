package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import ru.LevLezhnin.NauJava.model.FileStatistics;

@RepositoryRestResource
public interface FileStatisticsRepository extends JpaRepository<FileStatistics, Long> {}
