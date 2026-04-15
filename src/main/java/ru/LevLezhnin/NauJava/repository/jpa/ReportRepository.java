package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.LevLezhnin.NauJava.model.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {}
