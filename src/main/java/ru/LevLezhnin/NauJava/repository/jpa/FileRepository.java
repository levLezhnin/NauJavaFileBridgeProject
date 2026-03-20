package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.LevLezhnin.NauJava.model.File;

import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {}
