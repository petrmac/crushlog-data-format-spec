package io.cldf.tool.config;

import jakarta.inject.Singleton;

import io.cldf.schema.SchemaService;
import io.micronaut.context.annotation.Factory;

/** Factory to provide SchemaService implementation from cldf-java module. */
@Factory
public class SchemaServiceFactory {

  @Singleton
  public SchemaService schemaService() {
    return SchemaService.create();
  }
}
