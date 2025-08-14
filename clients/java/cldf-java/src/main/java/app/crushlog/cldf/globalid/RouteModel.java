package app.crushlog.cldf.globalid;

/** Route-related models for CLID generation */
public class RouteModel {

  /** Represents a climbing route for CLID generation */
  public record Route(
      String name, String grade, RouteType type, FirstAscent firstAscent, Double height) {

    // Additional constructor without firstAscent and height
    public Route(String name, String grade, RouteType type) {
      this(name, grade, type, null, null);
    }
  }

  /** Route type enumeration */
  public enum RouteType {
    SPORT("sport"),
    TRAD("trad"),
    BOULDER("boulder"),
    ALPINE("alpine"),
    ICE("ice"),
    MIXED("mixed"),
    AID("aid"),
    TOP_ROPE("top_rope"),
    DEEP_WATER_SOLO("deep_water_solo"),
    FREE_SOLO("free_solo"),
    SPEED("speed"),
    VIA_FERRATA("via_ferrata");

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
  public record FirstAscent(String name, Integer year) {}
}
