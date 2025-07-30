package io.cldf.tool.services;

import java.util.List;
import java.util.Map;

/**
 * Interface for querying and filtering CLDF data. Provides methods to apply filter expressions to
 * collections of objects.
 */
public interface QueryService {

  /**
   * Applies a filter expression to a list of objects. Supports various operators (=, !=, &gt;=,
   * &lt;=, &gt;, &lt;) and AND expressions.
   *
   * @param items the list of objects to filter
   * @param filterExpression the filter expression (e.g., "type=boulder AND attempts&gt;1")
   * @return filtered list of objects
   */
  List<Object> applyFilter(List<Object> items, String filterExpression);

  /**
   * Sorts a list of objects by the specified field.
   *
   * @param items the list of objects to sort
   * @param sortExpression the sort expression (e.g., "date desc")
   * @return sorted list of objects
   */
  List<Object> sort(List<Object> items, String sortExpression);

  /**
   * Filters objects to include only specified fields.
   *
   * @param items the list of objects to filter
   * @param fields the fields to include
   * @return list of objects with only specified fields
   */
  List<Object> filterFields(List<Object> items, List<String> fields);

  /**
   * Calculates statistics for a collection of objects.
   *
   * @param items the list of objects to analyze
   * @param type the type of objects being analyzed
   * @return map containing statistical data
   */
  Map<String, Object> calculateStatistics(List<Object> items, String type);
}
