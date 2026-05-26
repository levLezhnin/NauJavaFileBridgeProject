package ru.LevLezhnin.NauJava.repository.search.file;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.model.File;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

@Component
public class FindAllUploadedBeforeDateStrategy implements FileSearchStrategy {

    private static final DateTimeFormatter RU_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy[ HH:mm:ss][ HH:mm]", Locale.forLanguageTag("ru"))
            .withZone(ZoneId.of("UTC"));

    @Override
    public String getCriteriaKey() { return "uploadedBefore"; }

    @Override
    public Specification<File> getSpecification(String searchValue) {
        Instant cutoffInstant = parseFlexibleDate(searchValue, true);
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("uploadedAt"), cutoffInstant);
    }

    /**
     * Умный парсер: понимает YYYY-MM-DD, ISO-8601 с таймзоной и без.
     * @param endOfDay если true и введена только дата, считаем 23:59:59 (для "Before")
     */
    private Instant parseFlexibleDate(String value, boolean endOfDay) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {}

        try {
            TemporalAccessor parsed = RU_FORMATTER.parseBest(
                    value,
                    LocalDateTime::from,
                    LocalDate::from
            );
            if (parsed instanceof LocalDateTime ldt) {
                return applyZone(ldt, endOfDay);
            } else if (parsed instanceof LocalDate ld) {
                return applyZone(ld.atStartOfDay(), endOfDay);
            }
        } catch (DateTimeParseException ignored) {}

        try {
            LocalDateTime ldt = LocalDateTime.parse(value);
            return applyZone(ldt, endOfDay);
        } catch (DateTimeParseException ignored) {}

        try {
            LocalDate ld = LocalDate.parse(value);
            return applyZone(ld.atStartOfDay(), endOfDay);
        } catch (DateTimeParseException ignored) {}

        throw new IllegalArgumentException(
                "Неверный формат даты. Ожидается: ДД.ММ.ГГГГ, ДД.ММ.ГГГГ ЧЧ:ММ, ГГГГ-ММ-ДД или ГГГГ-ММ-ДДTЧЧ:ММ:СС");
    }

    private Instant applyZone(LocalDateTime dateTime, boolean endOfDay) {
        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime zoned = dateTime.atZone(zone);

        if (endOfDay) {
            zoned = zoned.with(LocalTime.MAX);
        }
        return zoned.toInstant();
    }
}