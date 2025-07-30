package io.cldf.schema;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Service for retrieving and managing CLDF schema information. */
public interface SchemaService {

  /**
   * Creates a new instance of the default SchemaService implementation.
   *
   * @return a new DefaultSchemaService instance
   */
  static SchemaService create() {
    return new DefaultSchemaService();
  }

  /**
   * Retrieves schema information for a specific component.
   *
   * @param componentName the name of the component (e.g., "manifest", "location", "all")
   * @return a map containing the schema information
   * @throws IOException if schema files cannot be read
   * @throws IllegalArgumentException if the component name is invalid
   */
  Map<String, Object> getSchemaInfo(String componentName) throws IOException;

  /**
   * Builds complete schema information for all components.
   *
   * @return a map containing all schema information
   * @throws IOException if schema files cannot be read
   */
  Map<String, Object> buildCompleteSchema() throws IOException;

  /**
   * Extracts enum definitions from all schema files.
   *
   * @return a map of enum types and their allowed values
   */
  Map<String, Object> buildEnumsFromSchemas();

  /**
   * Provides information about supported date formats.
   *
   * @return a map containing date format information and examples
   */
  Map<String, Object> buildDateFormatsInfo();

  /**
   * Lists common mistakes when working with CLDF format.
   *
   * @return a list of common mistakes and their explanations
   */
  List<String> buildCommonMistakes();

  /**
   * Provides example data structures for CLDF format.
   *
   * @return a map containing example data
   */
  Map<String, Object> buildExampleData();
}
