package app.crushlog.cldf.tool.config;

import jakarta.inject.Singleton;

import app.crushlog.cldf.schema.DefaultSchemaService;
import app.crushlog.cldf.schema.SchemaService;
import io.micronaut.context.annotation.Factory;

/** Factory to provide SchemaService implementation from cldf-java module. */
@Factory
public class SchemaServiceFactory {

  @Singleton
  public SchemaService schemaService() {
    return new DefaultSchemaService();
  }
}
