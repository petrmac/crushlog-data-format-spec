package app.crushlog.cldf.tool.services;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import app.crushlog.cldf.models.Climb;
import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Session;
import app.crushlog.cldf.models.enums.FinishType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DefaultQueryService implements QueryService {

  private static final Pattern FILTER_PATTERN = Pattern.compile("(\\w+)(=|!=|>=|<=|>|<)(.+)");

  @Override
  public List<Object> applyFilter(List<Object> items, String filterExpression) {
    if (items.isEmpty() || filterExpression == null || filterExpression.trim().isEmpty()) {
      return items;
    }

    // Parse filter expressions (support AND for now)
    String[] filters = filterExpression.split("\\s+AND\\s+");
    List<Object> result = new ArrayList<>(items);

    for (String filter : filters) {
      result = applySingleFilter(result, filter.trim());
    }

    return result;
  }

  private List<Object> applySingleFilter(List<Object> items, String filter) {
    Matcher matcher = FILTER_PATTERN.matcher(filter);
    if (!matcher.matches()) {
      log.warn("Invalid filter expression: {}", filter);
      return items;
    }

    String field = matcher.group(1);
    String operator = matcher.group(2);
    String rawValue = matcher.group(3).trim();

    // Remove quotes if present
    final String value;
    if (rawValue.startsWith("'") && rawValue.endsWith("'")
        || rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
      value = rawValue.substring(1, rawValue.length() - 1);
    } else {
      value = rawValue;
    }

    return items.stream()
        .filter(item -> matchesFilter(item, field, operator, value))
        .collect(Collectors.toList());
  }

  private boolean matchesFilter(Object item, String field, String operator, String value) {
    try {
      Object fieldValue = getFieldValue(item, field);
      if (fieldValue == null) {
        return false;
      }

      return switch (operator) {
        case "=" -> equals(fieldValue, value);
        case "!=" -> !equals(fieldValue, value);
        case ">" -> compare(fieldValue, value) > 0;
        case ">=" -> compare(fieldValue, value) >= 0;
        case "<" -> compare(fieldValue, value) < 0;
        case "<=" -> compare(fieldValue, value) <= 0;
        default -> false;
      };
    } catch (Exception e) {
      log.debug("Error applying filter on field {}: {}", field, e.getMessage());
      return false;
    }
  }

  private Object getFieldValue(Object item, String fieldName) {
    // Special handling for nested fields (e.g., grades.grade)
    String[] parts = fieldName.split("\\.");
    Object current = item;

    for (String part : parts) {
      current = getDirectFieldValue(current, part);
      if (current == null) {
        return null;
      }
    }

    return current;
  }

  private Object getDirectFieldValue(Object item, String fieldName) {
    // Special cases for common fields
    if (item instanceof Climb climb) {
      switch (fieldName) {
        case "type":
          return climb.getType() != null ? climb.getType().name() : null;
        case "grade":
          return climb.getGrades() != null ? climb.getGrades().getGrade() : null;
        case "rating":
          return climb.getRating();
        case "date":
          return climb.getDate();
        case "routeName":
          return climb.getRouteName();
        case "finishType":
          return climb.getFinishType();
        case "attempts":
          return climb.getAttempts();
        case "isIndoor":
          return climb.getIsIndoor();
        default:
          // Fall through to generic reflection-based access
          break;
      }
    } else if (item instanceof Session session) {
      switch (fieldName) {
        case "location":
          return session.getLocation();
        case "date":
          return session.getDate();
        case "isIndoor":
          return session.getIsIndoor();
        default:
          // Fall through to generic reflection-based access
          break;
      }
    } else if (item instanceof Location location) {
      switch (fieldName) {
        case "name":
          return location.getName();
        case "country":
          return location.getCountry();
        case "isIndoor":
          return location.getIsIndoor();
        default:
          // Fall through to generic reflection-based access
          break;
      }
    }

    // Generic reflection-based field access
    try {
      Field field = item.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(item);
    } catch (NoSuchFieldException e) {
      // Try getter method
      String getterName =
          "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
      try {
        return item.getClass().getMethod(getterName).invoke(item);
      } catch (Exception ex) {
        log.debug("Field {} not found on {}", fieldName, item.getClass().getSimpleName());
        return null;
      }
    } catch (IllegalAccessException e) {
      log.debug("Cannot access field {} on {}", fieldName, item.getClass().getSimpleName());
      return null;
    }
  }

  private boolean equals(Object fieldValue, String filterValue) {
    if (fieldValue instanceof String) {
      return fieldValue.toString().equalsIgnoreCase(filterValue);
    } else if (fieldValue instanceof Boolean) {
      return fieldValue.equals(Boolean.parseBoolean(filterValue));
    } else if (fieldValue instanceof Number) {
      return fieldValue.equals(parseNumber(filterValue));
    } else if (fieldValue instanceof Enum) {
      return ((Enum<?>) fieldValue).name().equalsIgnoreCase(filterValue);
    }
    return fieldValue.toString().equals(filterValue);
  }

  private int compare(Object fieldValue, String filterValue) {
    if (fieldValue instanceof Number) {
      double fieldNum = ((Number) fieldValue).doubleValue();
      double filterNum = parseNumber(filterValue).doubleValue();
      return Double.compare(fieldNum, filterNum);
    } else if (fieldValue instanceof LocalDate fieldDate) {
      LocalDate filterDate = LocalDate.parse(filterValue);
      return fieldDate.compareTo(filterDate);
    } else if (fieldValue instanceof OffsetDateTime fieldDate) {
      OffsetDateTime filterDate = OffsetDateTime.parse(filterValue);
      return fieldDate.compareTo(filterDate);
    } else if (fieldValue instanceof String) {
      // Special handling for grades
      if (isGrade(fieldValue.toString())) {
        return compareGrades(fieldValue.toString(), filterValue);
      }
      return fieldValue.toString().compareTo(filterValue);
    }
    throw new IllegalArgumentException("Cannot compare " + fieldValue.getClass().getSimpleName());
  }

  private Number parseNumber(String value) {
    try {
      if (value.contains(".")) {
        return Double.parseDouble(value);
      }
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number: " + value);
    }
  }

  private boolean isGrade(String value) {
    return value.matches("^(V\\d+|5\\.\\d+[a-d]?)$");
  }

  private int compareGrades(String grade1, String grade2) {
    // Simple V-scale comparison
    if (grade1.startsWith("V") && grade2.startsWith("V")) {
      int v1 = Integer.parseInt(grade1.substring(1));
      int v2 = Integer.parseInt(grade2.substring(1));
      return Integer.compare(v1, v2);
    }
    // Add more grade system comparisons as needed
    return grade1.compareTo(grade2);
  }

  public List<Object> sort(List<Object> items, String sortExpression) {
    if (items.isEmpty() || sortExpression == null || sortExpression.trim().isEmpty()) {
      return items;
    }

    boolean descending = sortExpression.startsWith("-");
    String field = descending ? sortExpression.substring(1) : sortExpression;

    Comparator<Object> comparator =
        (a, b) -> {
          Object valueA = getFieldValue(a, field);
          Object valueB = getFieldValue(b, field);

          if (valueA == null && valueB == null) return 0;
          if (valueA == null) return descending ? 1 : -1;
          if (valueB == null) return descending ? -1 : 1;

          int result;
          if (valueA instanceof Comparable<?>) {
            result = ((Comparable<Object>) valueA).compareTo(valueB);
          } else {
            result = valueA.toString().compareTo(valueB.toString());
          }

          return descending ? -result : result;
        };

    return items.stream().sorted(comparator).collect(Collectors.toList());
  }

  public List<Object> filterFields(List<Object> items, List<String> fields) {
    // For now, return items as-is since field filtering would require
    // creating new objects with only selected fields
    // This could be implemented with dynamic proxy or map conversion
    return items;
  }

  public Map<String, Object> calculateStatistics(List<Object> items, String type) {
    Map<String, Object> stats = new HashMap<>();
    stats.put("total", items.size());

    if (items.isEmpty()) {
      return stats;
    }

    Object first = items.getFirst();
    if (first instanceof Climb) {
      calculateClimbStats(items.stream().map(i -> (Climb) i).collect(Collectors.toList()), stats);
    } else if (first instanceof Session) {
      calculateSessionStats(
          items.stream().map(i -> (Session) i).collect(Collectors.toList()), stats);
    } else if (first instanceof Location) {
      calculateLocationStats(
          items.stream().map(i -> (Location) i).collect(Collectors.toList()), stats);
    }

    return stats;
  }

  private void calculateClimbStats(List<Climb> climbs, Map<String, Object> stats) {
    Map<String, Long> byType =
        climbs.stream()
            .filter(c -> c.getType() != null)
            .collect(Collectors.groupingBy(c -> c.getType().getValue(), Collectors.counting()));
    stats.put("byType", byType);

    Map<FinishType, Long> byFinishType =
        climbs.stream()
            .filter(c -> c.getFinishType() != null)
            .collect(Collectors.groupingBy(Climb::getFinishType, Collectors.counting()));
    // Convert enum keys to strings for JSON serialization
    Map<String, Long> byFinishTypeStr =
        byFinishType.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().getValue(), Map.Entry::getValue));
    stats.put("byFinishType", byFinishTypeStr);

    double avgRating =
        climbs.stream()
            .filter(c -> c.getRating() != null)
            .mapToInt(Climb::getRating)
            .average()
            .orElse(0.0);
    stats.put("averageRating", Math.round(avgRating * 10) / 10.0);

    long indoorCount =
        climbs.stream().filter(c -> c.getIsIndoor() != null && c.getIsIndoor()).count();
    stats.put("indoorCount", indoorCount);
    stats.put("outdoorCount", climbs.size() - indoorCount);
  }

  private void calculateSessionStats(List<Session> sessions, Map<String, Object> stats) {
    Map<String, Long> byLocation =
        sessions.stream()
            .filter(s -> s.getLocation() != null)
            .collect(Collectors.groupingBy(Session::getLocation, Collectors.counting()));
    stats.put("byLocation", byLocation);

    long indoorCount =
        sessions.stream().filter(s -> s.getIsIndoor() != null && s.getIsIndoor()).count();
    stats.put("indoorCount", indoorCount);
    stats.put("outdoorCount", sessions.size() - indoorCount);
  }

  private void calculateLocationStats(List<Location> locations, Map<String, Object> stats) {
    Map<String, Long> byCountry =
        locations.stream()
            .filter(l -> l.getCountry() != null)
            .collect(Collectors.groupingBy(Location::getCountry, Collectors.counting()));
    stats.put("byCountry", byCountry);

    long indoorCount =
        locations.stream().filter(l -> l.getIsIndoor() != null && l.getIsIndoor()).count();
    stats.put("indoorCount", indoorCount);
    stats.put("outdoorCount", locations.size() - indoorCount);
  }
}
