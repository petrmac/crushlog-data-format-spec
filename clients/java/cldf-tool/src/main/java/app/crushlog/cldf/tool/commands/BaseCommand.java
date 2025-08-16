package app.crushlog.cldf.tool.commands;

import app.crushlog.cldf.tool.models.CommandResult;
import app.crushlog.cldf.tool.models.ErrorResponse;
import app.crushlog.cldf.tool.utils.OutputFormat;
import app.crushlog.cldf.tool.utils.OutputHandler;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Slf4j
public abstract class BaseCommand implements Runnable {

  @Spec protected CommandSpec spec;

  @Option(
      names = {"--json", "--output-format"},
      description = "Output format: ${COMPLETION-CANDIDATES}",
      defaultValue = "text",
      scope = picocli.CommandLine.ScopeType.INHERIT)
  protected OutputFormat outputFormat;

  @Option(
      names = {"--quiet", "-q"},
      description = "Suppress non-essential output",
      scope = picocli.CommandLine.ScopeType.INHERIT)
  protected boolean quiet;

  protected OutputHandler output;

  @Override
  public void run() {
    output = new OutputHandler(outputFormat, quiet);
    try {
      CommandResult result = execute();
      handleResult(result);
    } catch (Exception e) {
      handleError(e);
    }
  }

  protected abstract CommandResult execute() throws Exception;

  protected void handleResult(CommandResult result) {
    if (outputFormat == OutputFormat.json) {
      output.writeResult(result);
    } else {
      outputText(result);
    }

    System.exit(result.getExitCode());
  }

  protected void handleError(Exception e) {
    log.error("Command failed", e);

    ErrorResponse error =
        ErrorResponse.builder()
            .success(false)
            .error(
                ErrorResponse.Error.builder()
                    .code("COMMAND_FAILED")
                    .message(e.getMessage())
                    .type(e.getClass().getSimpleName())
                    .build())
            .build();

    output.writeError(error);
    System.exit(1);
  }

  protected abstract void outputText(CommandResult result);

  protected void logInfo(String message) {
    output.writeInfo(message);
  }

  protected void logWarning(String message) {
    output.writeWarning(message);
  }
}
