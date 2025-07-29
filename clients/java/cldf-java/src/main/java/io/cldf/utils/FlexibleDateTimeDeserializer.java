package io.cldf.utils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Custom Jackson deserializer for OffsetDateTime that handles multiple date formats. This
 * deserializer attempts to parse dates using several common formats in order.
 */
public class FlexibleDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

  /**
   * List of supported date formats in order of preference. The deserializer will try each format
   * until one succeeds.
   */
  private static final List<DateTimeFormatter> FORMATTERS =
      Arrays.asList(
          // ISO-8601 with milliseconds and offset
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
          // ISO-8601 with milliseconds and Z
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
          // ISO-8601 without milliseconds with offset
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
          // ISO-8601 without milliseconds with Z
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
          // ISO-8601 basic format
          DateTimeFormatter.ISO_OFFSET_DATE_TIME,
          // ISO-8601 with Z suffix (converted to UTC)
          DateTimeFormatter.ISO_INSTANT);

  @Override
  public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    String dateString = parser.getValueAsString();

    if (dateString == null || dateString.trim().isEmpty()) {
      return null;
    }

    dateString = dateString.trim();

    // Try each formatter in order
    for (DateTimeFormatter formatter : FORMATTERS) {
      try {
        // Special handling for ISO_INSTANT which returns an Instant
        if (formatter == DateTimeFormatter.ISO_INSTANT) {
          return OffsetDateTime.ofInstant(
              formatter.parse(dateString, java.time.Instant::from), ZoneOffset.UTC);
        }

        return OffsetDateTime.parse(dateString, formatter);
      } catch (DateTimeParseException e) {
        // Continue to next formatter
        continue;
      }
    }

    // If no formatter worked, throw a descriptive error
    throw new IOException(
        String.format(
            "Cannot parse date string '%s'. Supported formats include: "
                + "yyyy-MM-dd'T'HH:mm:ss.SSSXXX, yyyy-MM-dd'T'HH:mm:ss'Z', "
                + "yyyy-MM-dd'T'HH:mm:ssXXX, yyyy-MM-dd'T'HH:mm:ss'Z', "
                + "and other ISO-8601 variants",
            dateString));
  }
}
