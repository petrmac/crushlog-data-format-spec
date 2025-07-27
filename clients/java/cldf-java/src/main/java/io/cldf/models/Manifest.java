package io.cldf.models;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private OffsetDateTime exportDate;

  @JsonProperty(required = true)
  private String appVersion;

  @JsonProperty(required = true)
  private Platform platform;

  private Stats stats;

  private ExportOptions exportOptions;

  /** Platform that created the export. */
  public enum Platform {
    iOS,
    Android,
    Web,
    Desktop
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

    /** Media export strategy. */
    public enum MediaStrategy {
      reference,
      thumbnails,
      full
    }

    /** Date range for filtered exports. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      private LocalDate start;

      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
      private LocalDate end;
    }
  }
}
