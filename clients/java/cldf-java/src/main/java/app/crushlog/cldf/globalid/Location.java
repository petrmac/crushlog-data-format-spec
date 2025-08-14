package app.crushlog.cldf.globalid;

import lombok.Builder;

/** Represents a climbing location for CLID generation */
@Builder
public record Location(
    String country,
    String state,
    String city,
    String name,
    Coordinates coordinates,
    boolean isIndoor) {}
