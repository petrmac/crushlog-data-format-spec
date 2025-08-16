package app.crushlog.cldf.tool.services;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.models.*;

@Singleton
public class TreeService {

  public record TreeNode(
      String name, String type, Map<String, Object> attributes, List<TreeNode> children) {}

  public TreeNode buildTree(CLDFArchive archive, boolean showDetails) {
    Map<String, Object> rootAttrs = new LinkedHashMap<>();
    rootAttrs.put("version", archive.getManifest().getVersion());
    rootAttrs.put("created", archive.getManifest().getCreationDate());

    List<TreeNode> rootChildren = new ArrayList<>();

    // Add locations
    Optional.ofNullable(archive.getLocations())
        .filter(locations -> !locations.isEmpty())
        .ifPresent(
            locations -> {
              List<TreeNode> locationNodes =
                  locations.stream()
                      .map(location -> buildLocationNode(archive, location, showDetails))
                      .collect(Collectors.toList());
              rootChildren.add(
                  new TreeNode(
                      "Locations", "container", Map.of("count", locations.size()), locationNodes));
            });

    // Add sessions
    Optional.ofNullable(archive.getSessions())
        .filter(sessions -> !sessions.isEmpty())
        .ifPresent(
            sessions -> {
              List<TreeNode> sessionNodes =
                  sessions.stream()
                      .map(session -> buildSessionNode(archive, session))
                      .collect(Collectors.toList());
              rootChildren.add(
                  new TreeNode(
                      "Sessions", "container", Map.of("count", sessions.size()), sessionNodes));
            });

    // Add tags
    Optional.ofNullable(archive.getTags())
        .filter(tags -> !tags.isEmpty())
        .ifPresent(
            tags -> {
              List<TreeNode> tagNodes =
                  tags.stream()
                      .map(
                          tag ->
                              new TreeNode(
                                  tag.getName()
                                      + (tag.getCategory() != null
                                          ? " (" + tag.getCategory() + ")"
                                          : ""),
                                  "tag",
                                  Collections.emptyMap(),
                                  Collections.emptyList()))
                      .collect(Collectors.toList());
              rootChildren.add(
                  new TreeNode("Tags", "container", Map.of("count", tags.size()), tagNodes));
            });

    return new TreeNode("CLDF Archive", "root", rootAttrs, rootChildren);
  }

  public Map<String, Object> buildTreeData(
      CLDFArchive archive, String fileName, boolean showDetails) {
    Map<String, Object> tree = new LinkedHashMap<>();
    tree.put("archive", fileName);
    tree.put("version", archive.getManifest().getVersion());
    tree.put("created", archive.getManifest().getCreationDate());

    // Build locations tree
    tree.put(
        "locations",
        Optional.ofNullable(archive.getLocations()).orElse(Collections.emptyList()).stream()
            .map(location -> buildLocationData(archive, location, showDetails))
            .collect(Collectors.toList()));

    // Add sessions
    tree.put(
        "sessions",
        Optional.ofNullable(archive.getSessions()).orElse(Collections.emptyList()).stream()
            .map(session -> buildSessionData(archive, session))
            .collect(Collectors.toList()));

    // Add tags
    Optional.ofNullable(archive.getTags())
        .filter(tags -> !tags.isEmpty())
        .ifPresent(
            tags ->
                tree.put(
                    "tags",
                    tags.stream()
                        .map(
                            tag ->
                                Map.of(
                                    "name",
                                    tag.getName(),
                                    "category",
                                    Objects.toString(tag.getCategory(), "")))
                        .collect(Collectors.toList())));

    return tree;
  }

  private TreeNode buildLocationNode(CLDFArchive archive, Location location, boolean showDetails) {
    Map<String, Object> attrs = new LinkedHashMap<>();
    attrs.put("id", location.getId());
    attrs.put("type", location.getIsIndoor() ? "Indoor" : "Outdoor");

    if (showDetails) {
      Optional.ofNullable(location.getCountry())
          .ifPresent(country -> attrs.put("country", country));
      Optional.ofNullable(location.getState()).ifPresent(state -> attrs.put("state", state));
      Optional.ofNullable(location.getCoordinates())
          .ifPresent(
              coords ->
                  attrs.put("coordinates", coords.getLatitude() + ", " + coords.getLongitude()));
    }

    List<TreeNode> children = new ArrayList<>();

    // Add sectors
    List<Sector> sectors = getSectorsForLocation(archive, location.getId());
    if (!sectors.isEmpty()) {
      List<TreeNode> sectorNodes =
          sectors.stream()
              .map(sector -> buildSectorNode(archive, sector))
              .collect(Collectors.toList());
      children.add(
          new TreeNode("Sectors", "container", Map.of("count", sectors.size()), sectorNodes));
    }

    return new TreeNode(location.getName(), "location", attrs, children);
  }

  private TreeNode buildSectorNode(CLDFArchive archive, Sector sector) {
    Map<String, Object> attrs = Map.of("id", sector.getId());
    List<TreeNode> children = new ArrayList<>();

    // Add routes
    List<Route> routes = getRoutesForSector(archive, sector.getId());
    if (!routes.isEmpty()) {
      List<TreeNode> routeNodes =
          routes.stream().map(this::buildRouteNode).collect(Collectors.toList());
      children.add(new TreeNode("Routes", "container", Map.of("count", routes.size()), routeNodes));
    }

    return new TreeNode(sector.getName(), "sector", attrs, children);
  }

  private TreeNode buildRouteNode(Route route) {
    Map<String, Object> attrs = new LinkedHashMap<>();
    attrs.put("id", route.getId());
    attrs.put("grades", formatGrades(convertGradesToMap(route.getGrades())));
    attrs.put("color", route.getColor());

    return new TreeNode(route.getName(), "route", attrs, Collections.emptyList());
  }

  private TreeNode buildSessionNode(CLDFArchive archive, Session session) {
    String locationName = getLocationName(archive, session.getLocationId());
    Map<String, Object> attrs = Map.of("id", session.getId(), "location", locationName);

    List<TreeNode> children = new ArrayList<>();

    // Add climbs
    List<Climb> climbs = getClimbsForSession(archive, session.getId());
    if (!climbs.isEmpty()) {
      List<TreeNode> climbNodes =
          climbs.stream().map(climb -> buildClimbNode(archive, climb)).collect(Collectors.toList());
      children.add(new TreeNode("Climbs", "container", Map.of("count", climbs.size()), climbNodes));
    }

    return new TreeNode(session.getDate().toString(), "session", attrs, children);
  }

  private TreeNode buildClimbNode(CLDFArchive archive, Climb climb) {
    String routeName =
        Optional.ofNullable(getRoute(archive, climb.getRouteId()))
            .map(Route::getName)
            .orElse("Unknown Route");

    Map<String, Object> attrs = Map.of("finishType", climb.getFinishType().toString());

    return new TreeNode(routeName, "climb", attrs, Collections.emptyList());
  }

  private Map<String, Object> buildLocationData(
      CLDFArchive archive, Location location, boolean showDetails) {
    Map<String, Object> locationData = new LinkedHashMap<>();
    locationData.put("id", location.getId());
    locationData.put("name", location.getName());
    locationData.put("type", location.getIsIndoor() ? "Indoor" : "Outdoor");

    if (showDetails) {
      Optional.ofNullable(location.getCountry())
          .ifPresent(country -> locationData.put("country", country));
      Optional.ofNullable(location.getState()).ifPresent(state -> locationData.put("state", state));
      Optional.ofNullable(location.getCoordinates())
          .ifPresent(
              coords ->
                  locationData.put(
                      "coordinates",
                      Map.of(
                          "latitude", coords.getLatitude(),
                          "longitude", coords.getLongitude())));
    }

    // Add sectors with their routes
    List<Map<String, Object>> sectorsData =
        getSectorsForLocation(archive, location.getId()).stream()
            .map(sector -> buildSectorData(archive, sector))
            .collect(Collectors.toList());

    if (!sectorsData.isEmpty()) {
      locationData.put("sectors", sectorsData);
    }

    return locationData;
  }

  private Map<String, Object> buildSectorData(CLDFArchive archive, Sector sector) {
    Map<String, Object> sectorData = new LinkedHashMap<>();
    sectorData.put("id", sector.getId());
    sectorData.put("name", sector.getName());

    // Add routes
    List<Map<String, Object>> routesData =
        getRoutesForSector(archive, sector.getId()).stream()
            .map(
                route -> {
                  Map<String, Object> routeMap = new LinkedHashMap<>();
                  routeMap.put("id", route.getId());
                  routeMap.put("name", route.getName());
                  routeMap.put("grades", convertGradesToMap(route.getGrades()));
                  routeMap.put("color", route.getColor());
                  return routeMap;
                })
            .collect(Collectors.toList());

    if (!routesData.isEmpty()) {
      sectorData.put("routes", routesData);
    }

    return sectorData;
  }

  private Map<String, Object> buildSessionData(CLDFArchive archive, Session session) {
    Map<String, Object> sessionData = new LinkedHashMap<>();
    sessionData.put("id", session.getId());
    sessionData.put("date", session.getDate());
    sessionData.put("location", getLocationName(archive, session.getLocationId()));

    List<Map<String, Object>> climbsData =
        getClimbsForSession(archive, session.getId()).stream()
            .map(
                climb -> {
                  Map<String, Object> climbMap = new LinkedHashMap<>();
                  climbMap.put(
                      "route",
                      Optional.ofNullable(getRoute(archive, climb.getRouteId()))
                          .map(Route::getName)
                          .orElse("Unknown"));
                  climbMap.put("finishType", climb.getFinishType().toString());
                  return climbMap;
                })
            .collect(Collectors.toList());

    if (!climbsData.isEmpty()) {
      sessionData.put("climbs", climbsData);
    }

    return sessionData;
  }

  private String formatGrades(Map<String, String> grades) {
    return Optional.ofNullable(grades)
        .filter(g -> !g.isEmpty())
        .map(g -> String.join(", ", g.values()))
        .orElse("No grade");
  }

  private List<Sector> getSectorsForLocation(CLDFArchive archive, Integer locationId) {
    return Optional.ofNullable(archive.getSectors()).orElse(Collections.emptyList()).stream()
        .filter(s -> locationId.equals(s.getLocationId()))
        .collect(Collectors.toList());
  }

  private List<Route> getRoutesForSector(CLDFArchive archive, Integer sectorId) {
    return Optional.ofNullable(archive.getRoutes()).orElse(Collections.emptyList()).stream()
        .filter(r -> sectorId.equals(r.getSectorId()))
        .collect(Collectors.toList());
  }

  private List<Climb> getClimbsForSession(CLDFArchive archive, Integer sessionId) {
    return Optional.ofNullable(archive.getClimbs()).orElse(Collections.emptyList()).stream()
        .filter(c -> sessionId.equals(c.getSessionId()))
        .collect(Collectors.toList());
  }

  private Route getRoute(CLDFArchive archive, Integer routeId) {
    return Optional.ofNullable(archive.getRoutes())
        .flatMap(routes -> routes.stream().filter(r -> routeId.equals(r.getId())).findFirst())
        .orElse(null);
  }

  private String getLocationName(CLDFArchive archive, Integer locationId) {
    if (locationId == null) return "Unknown";

    return Optional.ofNullable(archive.getLocations())
        .flatMap(
            locations ->
                locations.stream()
                    .filter(l -> locationId.equals(l.getId()))
                    .map(Location::getName)
                    .findFirst())
        .orElse("Unknown");
  }

  private Map<String, String> convertGradesToMap(Route.Grades grades) {
    if (grades == null) return Collections.emptyMap();

    Map<String, String> gradesMap = new LinkedHashMap<>();
    Optional.ofNullable(grades.getFrench()).ifPresent(g -> gradesMap.put("french", g));
    Optional.ofNullable(grades.getYds()).ifPresent(g -> gradesMap.put("yds", g));
    Optional.ofNullable(grades.getUiaa()).ifPresent(g -> gradesMap.put("uiaa", g));
    Optional.ofNullable(grades.getVScale()).ifPresent(g -> gradesMap.put("vScale", g));
    Optional.ofNullable(grades.getFont()).ifPresent(g -> gradesMap.put("font", g));

    return gradesMap;
  }
}
