package app.crushlog.cldf.tool.utils;

import java.io.PrintStream;

import app.crushlog.cldf.tool.models.CommandResult;
import app.crushlog.cldf.tool.models.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OutputHandler {
  private final OutputFormat format;
  @Getter private final boolean quiet;
  private final PrintStream out;
  private final PrintStream err;
  private final ObjectMapper jsonMapper;

  public OutputHandler(OutputFormat format, boolean quiet) {
    this(format, quiet, System.out, System.err);
  }

  public OutputHandler(OutputFormat format, boolean quiet, PrintStream out, PrintStream err) {
    this.format = format;
    this.quiet = quiet;
    this.out = out;
    this.err = err;
    this.jsonMapper = createObjectMapper();
  }

  private ObjectMapper createObjectMapper() {
    return JsonUtils.createPrettyMapper();
  }

  public void writeResult(CommandResult result) {
    try {
      if (format == OutputFormat.json) {
        out.println(jsonMapper.writeValueAsString(result));
      } else {
        if (result.getMessage() != null) {
          out.println(result.getMessage());
        }
      }
    } catch (Exception e) {
      log.error("Failed to write result", e);
      writeError("Failed to format output: " + e.getMessage());
    }
  }

  public void writeError(ErrorResponse error) {
    try {
      if (format == OutputFormat.json) {
        err.println(jsonMapper.writeValueAsString(error));
      } else {
        err.println("Error: " + error.getError().getMessage());
        if (error.getError().getSuggestion() != null) {
          err.println("Suggestion: " + error.getError().getSuggestion());
        }
      }
    } catch (Exception e) {
      log.error("Failed to write error", e);
      err.println("Error: " + error.getError().getMessage());
    }
  }

  public void writeError(String message) {
    if (format == OutputFormat.json) {
      ErrorResponse error =
          ErrorResponse.builder()
              .success(false)
              .error(ErrorResponse.Error.builder().code("ERROR").message(message).build())
              .build();
      writeError(error);
    } else {
      err.println("Error: " + message);
    }
  }

  public void writeInfo(String message) {
    if (!quiet && format != OutputFormat.json) {
      err.println(message);
    }
  }

  public void writeWarning(String message) {
    if (!quiet && format != OutputFormat.json) {
      err.println("Warning: " + message);
    }
  }

  public void writeDebug(String message) {
    if (!quiet && format != OutputFormat.json) {
      log.debug(message);
    }
  }

  public void writeJson(Object data) {
    try {
      out.println(jsonMapper.writeValueAsString(data));
    } catch (Exception e) {
      log.error("Failed to write JSON", e);
      writeError("Failed to format JSON: " + e.getMessage());
    }
  }

  public void write(String text) {
    out.println(text);
  }

  public void writeError(String text, boolean isError) {
    if (isError) {
      err.println(text);
    } else {
      out.println(text);
    }
  }

  public boolean isJsonFormat() {
    return format == OutputFormat.json;
  }
}
