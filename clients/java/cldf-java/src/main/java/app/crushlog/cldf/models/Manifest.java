package app.crushlog.cldf.models;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import app.crushlog.cldf.models.enums.MediaStrategy;
import app.crushlog.cldf.models.enums.Platform;
import app.crushlog.cldf.utils.FlexibleDateTimeDeserializer;
import app.crushlog.cldf.utils.FlexibleLocalDateDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Archive manifest containing metadata about the CLDF export. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Manifest {

  @JsonProperty(required = true)
  private String version;

  @JsonProperty(required = true)
  private String format;

  @JsonProperty(required = true)
  @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
  private OffsetDateTime creationDate;

  @JsonProperty(required = true)
  private String appVersion;

  @JsonProperty(required = true)
  private Platform platform;

  private Author author;

  private String source;

  private Stats stats;

  private ExportOptions exportOptions;

  /** Author information for the export. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Author {
    private String name;
    private String email;
    private String website;
  }

  /** Statistics about the archive contents. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Stats {
    private Integer climbsCount;
    private Integer sessionsCount;
    private Integer locationsCount;
    private Integer routesCount;
    private Integer sectorsCount;
    private Integer tagsCount;
    private Integer mediaCount;
  }

  /** Options used when creating the export. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ExportOptions {
    private Boolean includeMedia;
    private MediaStrategy mediaStrategy;
    private DateRange dateRange;

    /** Date range for filtered exports. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
      @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
      private LocalDate start;

      @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
      private LocalDate end;
    }
  }
}
