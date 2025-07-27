package io.cldf.tool;

import jakarta.inject.Inject;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "cldf",
    description = "CLDF Tool for creating and manipulating climbing data archives",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    subcommands = {
      io.cldf.tool.commands.CreateCommand.class,
      io.cldf.tool.commands.ValidateCommand.class,
      io.cldf.tool.commands.ExtractCommand.class,
      io.cldf.tool.commands.MergeCommand.class,
      io.cldf.tool.commands.ConvertCommand.class
    })
public class Application implements Runnable {

  @Inject ApplicationContext applicationContext;

  public static void main(String[] args) {
    System.exit(PicocliRunner.execute(Application.class, args));
  }

  @Override
  public void run() {
    CommandLine cmd = new CommandLine(this);
    cmd.usage(System.out);
  }
}
