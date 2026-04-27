package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.LevLezhnin.NauJava.model.File;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    @EntityGraph(attributePaths = {"fileStatistics", "author", "author.storageQuota"})
    Optional<File> findWithDetailsById(UUID fileId);
    List<File> findAllByAuthorIdOrderByUploadedAtDesc(Long authorId, Pageable pageable);
}
