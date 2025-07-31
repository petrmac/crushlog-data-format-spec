package io.cldf.tool.commands;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import io.cldf.api.CLDFArchive;
import io.cldf.tool.models.CommandResult;
import io.cldf.tool.services.CLDFService;
import io.cldf.tool.services.TreeService;
import io.cldf.tool.utils.OutputFormat;
import picocli.CommandLine;

@CommandLine.Command(
    name = "tree",
    description = "Display CLDF archive contents in a tree structure",
    mixinStandardHelpOptions = true)
public class TreeCommand extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "CLDF file to display")
  private File inputFile;

  @CommandLine.Option(
      names = {"--show-details"},
      description = "Show detailed information for each item",
      defaultValue = "false")
  private boolean showDetails;

  private final CLDFService cldfService;
  private final TreeService treeService;

  @Inject
  public TreeCommand(CLDFService cldfService, TreeService treeService) {
    this.cldfService = cldfService;
    this.treeService = treeService;
  }

  // No-arg constructor for PicoCLI
  public TreeCommand() {
    this.cldfService = null;
    this.treeService = null;
  }

  @Override
  protected CommandResult execute() throws Exception {
    CLDFArchive archive = cldfService.read(inputFile);

    if (outputFormat == OutputFormat.json) {
      Map<String, Object> treeData =
          treeService.buildTreeData(archive, inputFile.getName(), showDetails);
      output.writeJson(treeData);
    } else {
      TreeService.TreeNode tree = treeService.buildTree(archive, showDetails);
      String textOutput = formatTreeAsText(tree, "", true);
      output.write(textOutput);
    }

    return CommandResult.builder()
        .success(true)
        .message("Tree displayed successfully")
        .exitCode(0)
        .build();
  }

  @Override
  protected void outputText(CommandResult result) {
    // This method is called by BaseCommand for text output
    // But we already handle text output in execute(), so this is empty
  }

  private String formatTreeAsText(TreeService.TreeNode node, String prefix, boolean isRoot) {
    StringBuilder sb = new StringBuilder();

    if (isRoot) {
      sb.append("CLDF Archive: ").append(inputFile.getName()).append("\n");
      node.attributes()
          .forEach(
              (key, value) ->
                  sb.append("└── ").append(key).append(": ").append(value).append("\n"));
      sb.append("\n");

      for (int i = 0; i < node.children().size(); i++) {
        TreeService.TreeNode child = node.children().get(i);
        boolean isLast = (i == node.children().size() - 1);
        sb.append(formatTreeAsText(child, isLast ? "└── " : "├── ", false));
      }
    } else {
      sb.append(prefix).append(node.name());
      if ("container".equals(node.type()) && node.attributes().containsKey("count")) {
        sb.append(" (").append(node.attributes().get("count")).append(")");
      } else if (!node.attributes().isEmpty() && !"container".equals(node.type())) {
        sb.append(" (");
        String attrs =
            node.attributes().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
        sb.append(attrs).append(")");
      }
      sb.append("\n");

      // Determine the continuation prefix based on the current prefix
      String continuationPrefix;
      if (prefix.endsWith("└── ")) {
        continuationPrefix = prefix.substring(0, prefix.length() - 4) + "    ";
      } else if (prefix.endsWith("├── ")) {
        continuationPrefix = prefix.substring(0, prefix.length() - 4) + "│   ";
      } else {
        continuationPrefix = prefix;
      }

      for (int i = 0; i < node.children().size(); i++) {
        TreeService.TreeNode child = node.children().get(i);
        boolean isLast = (i == node.children().size() - 1);
        String childPrefix = continuationPrefix + (isLast ? "└── " : "├── ");
        sb.append(formatTreeAsText(child, childPrefix, false));
      }
    }

    return sb.toString();
  }
}
