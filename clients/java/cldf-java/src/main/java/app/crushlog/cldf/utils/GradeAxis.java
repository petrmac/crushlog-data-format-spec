package app.crushlog.cldf.utils;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.crushlog.cldf.models.enums.GradeSystem;

/**
 * The difficulty axis of the CrushLog Data Format.
 *
 * <p>Every grade of the five grading systems has an integer value on one axis per discipline: Font
 * and V-scale share the boulder axis; French, YDS and UIAA share the route axis. A higher value is
 * a harder grade. The tables are part of the format (see {@code grades.md} in the specification),
 * so every reader and writer computes the same value.
 */
public final class GradeAxis {

  /** Distance between two neighbouring grades of an evenly spaced ladder. */
  public static final int STEP = 100;

  private static final Map<GradeSystem, Map<String, Integer>> TABLES =
      new EnumMap<>(GradeSystem.class);

  static {
    Map<String, Integer> font = new LinkedHashMap<>();
    String[] fontGrades = {
      "4", "4A", "4B", "4C", "5A", "5A+", "5B", "5B+", "5C", "5C+", "6A", "6A+", "6B", "6B+", "6C",
      "6C+", "7A", "7A+", "7B", "7B+", "7C", "7C+", "8A", "8A+", "8B", "8B+", "8C", "8C+", "9A"
    };
    int[] fontValues = {
      0, 25, 50, 75, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 800, 900, 950,
      1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800
    };
    for (int i = 0; i < fontGrades.length; i++) {
      font.put(fontGrades[i], fontValues[i]);
    }
    TABLES.put(GradeSystem.FONT, Collections.unmodifiableMap(font));

    TABLES.put(
        GradeSystem.V_SCALE,
        evenly(
            "VB", "V0", "V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8", "V9", "V10", "V11", "V12",
            "V13", "V14", "V15", "V16", "V17"));
    TABLES.put(
        GradeSystem.FRENCH,
        evenly(
            "4a", "4b", "4c", "5a", "5b", "5c", "6a", "6a+", "6b", "6b+", "6c", "6c+", "7a", "7a+",
            "7b", "7b+", "7c", "7c+", "8a", "8a+", "8b", "8b+", "8c", "8c+", "9a", "9a+", "9b",
            "9b+", "9c", "9c+"));
    TABLES.put(
        GradeSystem.YDS,
        evenly(
            "5.6", "5.7", "5.8", "5.9", "5.10a", "5.10b", "5.10c", "5.10d", "5.11a", "5.11b",
            "5.11c", "5.11d", "5.12a", "5.12b", "5.12c", "5.12d", "5.13a", "5.13b", "5.13c",
            "5.13d", "5.14a", "5.14b", "5.14c", "5.14d", "5.15a", "5.15b", "5.15c", "5.15d",
            "5.16a", "5.16b"));
    TABLES.put(
        GradeSystem.UIAA,
        evenly(
            "IV", "IV+", "V-", "V", "V+", "VI-", "VI", "VI+", "VII-", "VII", "VII+", "VIII-",
            "VIII", "VIII+", "IX-", "IX", "IX+", "X-", "X", "X+", "XI-", "XI", "XI+", "XII-", "XII",
            "XII+", "XIII-", "XIII", "XIII+", "XIV-"));
  }

  private GradeAxis() {}

  private static Map<String, Integer> evenly(String... grades) {
    Map<String, Integer> table = new LinkedHashMap<>();
    for (int i = 0; i < grades.length; i++) {
      table.put(grades[i], i * STEP);
    }
    return Collections.unmodifiableMap(table);
  }

  /**
   * The grades of a system, easiest first.
   *
   * @param system the grading system
   * @return the ladder of that system
   */
  public static List<String> ladder(GradeSystem system) {
    return List.copyOf(TABLES.get(system).keySet());
  }

  /**
   * The value of a grade on the difficulty axis.
   *
   * @param system the grading system
   * @param grade the grade name, case sensitive
   * @return the value, or null when the grade is not on the system's ladder
   */
  public static Integer numericFor(GradeSystem system, String grade) {
    if (system == null || grade == null) {
      return null;
    }
    return TABLES.get(system).get(grade);
  }

  /**
   * The grade of a system nearest to a value. A value half way between two grades gives the easier
   * one; a value outside the ladder gives its nearest end.
   *
   * @param system the grading system
   * @param numeric the value on the difficulty axis
   * @return the nearest grade name
   */
  public static String gradeFor(GradeSystem system, int numeric) {
    String nearest = null;
    int smallest = Integer.MAX_VALUE;
    for (Map.Entry<String, Integer> entry : TABLES.get(system).entrySet()) {
      int distance = Math.abs(entry.getValue() - numeric);
      if (distance < smallest) {
        smallest = distance;
        nearest = entry.getKey();
      }
    }
    return nearest;
  }
}
