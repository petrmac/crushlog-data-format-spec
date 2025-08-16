package app.crushlog.cldf.api;

import java.util.List;
import java.util.Map;

import app.crushlog.cldf.models.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a complete CLDF (CrushLog Data Format) archive containing climbing data. This is the
 * main data structure for working with CLDF archives in memory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CLDFArchive {

  private Manifest manifest;
  private Checksums checksums;
  private List<Location> locations;
  private List<Climb> climbs;
  private List<Session> sessions;
  private List<Route> routes;
  private List<Sector> sectors;
  private List<Tag> tags;
  private List<MediaMetadataItem> mediaItems;
  private Map<String, byte[]> mediaFiles;

  /**
   * Checks if this archive contains route data.
   *
   * @return true if routes are present and not empty
   */
  public boolean hasRoutes() {
    return routes != null && !routes.isEmpty();
  }

  /**
   * Checks if this archive contains sector data.
   *
   * @return true if sectors are present and not empty
   */
  public boolean hasSectors() {
    return sectors != null && !sectors.isEmpty();
  }

  /**
   * Checks if this archive contains tag data.
   *
   * @return true if tags are present and not empty
   */
  public boolean hasTags() {
    return tags != null && !tags.isEmpty();
  }

  /**
   * Checks if this archive contains media metadata.
   *
   * @return true if media items are present and not empty
   */
  public boolean hasMedia() {
    return mediaItems != null && !mediaItems.isEmpty();
  }

  /**
   * Checks if this archive contains embedded media files.
   *
   * @return true if media files are present and not empty
   */
  public boolean hasEmbeddedMedia() {
    return mediaFiles != null && !mediaFiles.isEmpty();
  }
}
