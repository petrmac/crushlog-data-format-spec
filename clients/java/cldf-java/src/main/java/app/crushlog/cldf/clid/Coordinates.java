package app.crushlog.cldf.clid;

import lombok.Builder;
import lombok.NonNull;

/** Geographic coordinates for locations */
@Builder
public record Coordinates(double lat, double lon) {

  @Override
  @NonNull
  public String toString() {
    return "%.6f,%.6f".formatted(lat, lon);
  }
}
