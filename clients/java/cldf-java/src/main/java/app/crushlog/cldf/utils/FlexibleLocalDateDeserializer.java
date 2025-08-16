package app.crushlog.cldf.utils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Custom Jackson deserializer for LocalDate that handles multiple date formats. This deserializer
 * attempts to parse dates using several common formats in order.
 */
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

  /**
   * List of supported date formats in order of preference. The deserializer will try each format
   * until one succeeds.
   */
  private static final List<DateTimeFormatter> FORMATTERS =
      Arrays.asList(
          // ISO-8601 date format (preferred)
          DateTimeFormatter.ofPattern("yyyy-MM-dd"),
          // Alternative separators
          DateTimeFormatter.ofPattern("yyyy/MM/dd"),
          DateTimeFormatter.ofPattern("yyyy.MM.dd"),
          // Different orderings
          DateTimeFormatter.ofPattern("MM/dd/yyyy"),
          DateTimeFormatter.ofPattern("dd/MM/yyyy"),
          DateTimeFormatter.ofPattern("dd.MM.yyyy"),
          DateTimeFormatter.ofPattern("MM-dd-yyyy"),
          DateTimeFormatter.ofPattern("dd-MM-yyyy"),
          // ISO basic format
          DateTimeFormatter.BASIC_ISO_DATE,
          // ISO standard format
          DateTimeFormatter.ISO_LOCAL_DATE);

  @Override
  public LocalDate deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    String dateString = parser.getValueAsString();

    if (dateString == null || dateString.trim().isEmpty()) {
      return null;
    }

    dateString = dateString.trim();

    // Try each formatter in order
    for (DateTimeFormatter formatter : FORMATTERS) {
      try {
        return LocalDate.parse(dateString, formatter);
      } catch (DateTimeParseException e) {
        // Try next formatter
      }
    }

    // If no formatter worked, throw a descriptive error
    throw new IOException(
        String.format(
            "Cannot parse date string '%s'. Supported formats include: "
                + "yyyy-MM-dd, yyyy/MM/dd, MM/dd/yyyy, dd/MM/yyyy, "
                + "and other common date formats",
            dateString));
  }
}
