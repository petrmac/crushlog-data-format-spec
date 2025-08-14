package app.crushlog.cldf.tool;

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
      app.crushlog.cldf.tool.commands.CreateCommand.class,
      app.crushlog.cldf.tool.commands.ValidateCommand.class,
      app.crushlog.cldf.tool.commands.ExtractCommand.class,
      app.crushlog.cldf.tool.commands.MergeCommand.class,
      app.crushlog.cldf.tool.commands.ConvertCommand.class,
      app.crushlog.cldf.tool.commands.QueryCommand.class,
      app.crushlog.cldf.tool.commands.LoadCommand.class,
      app.crushlog.cldf.tool.commands.GraphQueryCommand.class,
      app.crushlog.cldf.tool.commands.SchemaCommand.class,
      app.crushlog.cldf.tool.commands.TreeCommand.class
    })
public class Application implements Runnable {

  private final ApplicationContext applicationContext;

  @Inject
  public Application(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public static void main(String[] args) {
    // Set system properties to avoid logging conflicts
    System.setProperty("log4j2.disable.jmx", "true");
    System.setProperty("log4j.shutdownHookEnabled", "false");

    System.exit(PicocliRunner.execute(Application.class, args));
  }

  @Override
  public void run() {
    CommandLine cmd = new CommandLine(this);
    cmd.usage(System.out);
  }
}
