package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import ru.LevLezhnin.NauJava.model.File;

import java.util.UUID;

@RepositoryRestResource
public interface FileRepository extends JpaRepository<File, UUID> {}
