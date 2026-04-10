package com.scholarops.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class TimestampNormalizer {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a"),
            DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm:ss a"),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy")
    );

    private TimestampNormalizer() {
        // Utility class
    }

    /**
     * Normalizes a raw timestamp string to a LocalDateTime in the specified timezone.
     *
     * @param rawTimestamp    the raw timestamp string (ISO 8601, US format, or Unix epoch seconds/millis)
     * @param targetTimezone  the target timezone ID (e.g. "America/New_York", "UTC")
     * @return the normalized LocalDateTime in the target timezone
     * @throws IllegalArgumentException if the timestamp cannot be parsed
     */
    public static LocalDateTime normalize(String rawTimestamp, String targetTimezone) {
        if (rawTimestamp == null || rawTimestamp.isBlank()) {
            throw new IllegalArgumentException("Timestamp cannot be null or blank");
        }

        ZoneId targetZone = ZoneId.of(targetTimezone);
        String trimmed = rawTimestamp.trim();

        // Try Unix timestamp (seconds or milliseconds)
        if (trimmed.matches("^-?\\d{1,13}$")) {
            long value = Long.parseLong(trimmed);
            Instant instant;
            if (Math.abs(value) > 1_000_000_000_000L) {
                // Milliseconds
                instant = Instant.ofEpochMilli(value);
            } else {
                // Seconds
                instant = Instant.ofEpochSecond(value);
            }
            return instant.atZone(targetZone).toLocalDateTime();
        }

        // Try Unix timestamp with decimal (seconds.fraction)
        if (trimmed.matches("^-?\\d{1,10}\\.\\d+$")) {
            double seconds = Double.parseDouble(trimmed);
            long epochSeconds = (long) seconds;
            long nanos = (long) ((seconds - epochSeconds) * 1_000_000_000);
            Instant instant = Instant.ofEpochSecond(epochSeconds, nanos);
            return instant.atZone(targetZone).toLocalDateTime();
        }

        // Try each date-time formatter
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                // Try as ZonedDateTime first (has timezone info)
                ZonedDateTime zdt = ZonedDateTime.parse(trimmed, formatter);
                return zdt.withZoneSameInstant(targetZone).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                // Continue
            }

            try {
                // Try as OffsetDateTime
                OffsetDateTime odt = OffsetDateTime.parse(trimmed, formatter);
                return odt.atZoneSameInstant(targetZone).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                // Continue
            }

            try {
                // Try as LocalDateTime (no timezone info, assume UTC)
                LocalDateTime ldt = LocalDateTime.parse(trimmed, formatter);
                return ldt.atZone(ZoneOffset.UTC).withZoneSameInstant(targetZone).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                // Continue
            }

            try {
                // Try as LocalDate (date only, set time to midnight)
                LocalDate ld = LocalDate.parse(trimmed, formatter);
                return ld.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(targetZone).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                // Continue
            }
        }

        throw new IllegalArgumentException("Unable to parse timestamp: " + trimmed);
    }
}
