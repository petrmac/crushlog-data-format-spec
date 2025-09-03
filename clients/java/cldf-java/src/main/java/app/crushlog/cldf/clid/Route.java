package app.crushlog.cldf.clid;

import lombok.Builder;

/** Represents a climbing route for CLID generation */
@Builder
public record Route(String name, String grade, FirstAscent firstAscent, Double height) {

  // Additional constructor without firstAscent and height
  public Route(String name, String grade) {
    this(name, grade, null, null);
  }

  /** First ascent information */
  @Builder
  public record FirstAscent(String name, Integer year) {}
}
