package io.cldf.globalid;

import lombok.Builder;

/** Represents a sector within a climbing location */
@Builder
public record Sector(String name, Integer order) {

  /** Constructor with just name (order defaults to null) */
  public Sector(String name) {
    this(name, null);
  }
}
