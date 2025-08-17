package app.crushlog.cldf.tool.models;

/**
 * Immutable record representing statistics about a CLDF archive.
 *
 * @param locations Number of locations in the archive
 * @param sessions Number of sessions in the archive
 * @param climbs Number of climbs in the archive
 * @param routes Number of routes in the archive
 * @param sectors Number of sectors in the archive
 * @param tags Number of tags in the archive
 * @param mediaItems Number of media items in the archive
 */
public record Statistics(
    int locations, int sessions, int climbs, int routes, int sectors, int tags, int mediaItems) {

  /** Builder for Statistics to maintain compatibility with existing code. */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private int locations;
    private int sessions;
    private int climbs;
    private int routes;
    private int sectors;
    private int tags;
    private int mediaItems;

    public Builder locations(int locations) {
      this.locations = locations;
      return this;
    }

    public Builder sessions(int sessions) {
      this.sessions = sessions;
      return this;
    }

    public Builder climbs(int climbs) {
      this.climbs = climbs;
      return this;
    }

    public Builder routes(int routes) {
      this.routes = routes;
      return this;
    }

    public Builder sectors(int sectors) {
      this.sectors = sectors;
      return this;
    }

    public Builder tags(int tags) {
      this.tags = tags;
      return this;
    }

    public Builder mediaItems(int mediaItems) {
      this.mediaItems = mediaItems;
      return this;
    }

    public Statistics build() {
      return new Statistics(locations, sessions, climbs, routes, sectors, tags, mediaItems);
    }
  }
}
