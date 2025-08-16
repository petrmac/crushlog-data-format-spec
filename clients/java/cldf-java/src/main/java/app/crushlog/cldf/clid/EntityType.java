package app.crushlog.cldf.clid;

import lombok.Getter;

/** Valid entity types for CLID generation. */
@Getter
public enum EntityType {
  LOCATION("location"),
  ROUTE("route"),
  SECTOR("sector"),
  CLIMB("climb"),
  SESSION("session"),
  MEDIA("media");

  public final String value;

  EntityType(String value) {
    this.value = value;
  }

  public static EntityType fromString(String value) {
    for (EntityType type : EntityType.values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown entity type: " + value);
  }
}
