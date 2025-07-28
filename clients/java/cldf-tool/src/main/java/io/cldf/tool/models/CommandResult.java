package io.cldf.tool.models;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommandResult {
  private boolean success;
  private String message;
  private Object data;
  private List<String> warnings;
  private Map<String, Object> metadata;

  @Builder.Default private int exitCode = 0;

  public static CommandResult success() {
    return CommandResult.builder().success(true).build();
  }

  public static CommandResult success(String message) {
    return CommandResult.builder().success(true).message(message).build();
  }

  public static CommandResult success(String message, Object data) {
    return CommandResult.builder().success(true).message(message).data(data).build();
  }

  public static CommandResult failure(String message) {
    return CommandResult.builder().success(false).message(message).exitCode(1).build();
  }
}
