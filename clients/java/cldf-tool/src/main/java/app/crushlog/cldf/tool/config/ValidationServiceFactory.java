package app.crushlog.cldf.tool.config;

import jakarta.inject.Singleton;

import app.crushlog.cldf.tool.services.DefaultValidationReportService;
import app.crushlog.cldf.tool.services.ValidationReportService;
import app.crushlog.cldf.tool.services.ValidationService;
import io.micronaut.context.annotation.Factory;

/** Factory to provide validation-related services. */
@Factory
public class ValidationServiceFactory {

  @Singleton
  public ValidationReportService validationReportService(ValidationService validationService) {
    return new DefaultValidationReportService(validationService);
  }
}
