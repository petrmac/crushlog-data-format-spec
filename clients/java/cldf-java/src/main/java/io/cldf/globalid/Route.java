package io.cldf.globalid;

import lombok.Builder;

/** Represents a climbing route for CLID generation */
@Builder
public record Route(
    String name, String grade, RouteType type, FirstAscent firstAscent, Double height) {}

/** Route type enumeration */
enum RouteType {
  SPORT("sport"),
  TRAD("trad"),
  BOULDER("boulder"),
  ICE("ice"),
  MIXED("mixed");

  private final String value;

  RouteType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}

/** First ascent information */
@Builder
record FirstAscent(Integer year, String name) {}
