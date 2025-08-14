package app.crushlog.cldf.tool.commands;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import jakarta.inject.Inject;

import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Route;
import app.crushlog.cldf.qr.*;
import app.crushlog.cldf.qr.impl.*;
import app.crushlog.cldf.qr.result.QRError;
import app.crushlog.cldf.qr.result.Result;
import app.crushlog.cldf.tool.services.CLDFService;
import app.crushlog.cldf.tool.utils.OutputHandler;
import app.crushlog.cldf.tool.utils.OutputFormat;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import jakarta.inject.Singleton;

@Singleton
@Command(
    name = "qr",
    description = "Generate or scan QR codes for routes and locations",
    mixinStandardHelpOptions = true,
    subcommands = {QRCommand.GenerateCommand.class, QRCommand.ScanCommand.class})
public class QRCommand implements Callable<Integer> {

  @Spec CommandSpec spec;

  // For PicoCLI
  public QRCommand() {}

  @Override
  public Integer call() {
    spec.commandLine().usage(System.out);
    return 0;
  }

  @Singleton
  @Command(name = "generate", description = "Generate QR code for a route or location")
  static class GenerateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to CLDF archive")
    private Path archivePath;

    @Parameters(index = "1", description = "CLID of the route or location")
    private String clid;

    @Option(
        names = {"-o", "--output"},
        description = "Output file path (PNG format)",
        required = true)
    private Path outputPath;

    @Option(
        names = {"-s", "--size"},
        description = "QR code size in pixels",
        defaultValue = "256")
    private int size;

    @Option(
        names = {"--base-url"},
        description = "Base URL for QR code links",
        defaultValue = "https://crushlog.pro")
    private String baseUrl;

    @Option(
        names = {"--include-ipfs"},
        description = "Include IPFS hash if available")
    private boolean includeIPFS;

    @Option(
        names = {"--ipfs-hash"},
        description = "IPFS hash to include")
    private String ipfsHash;

    @Option(
        names = {"-v", "--verbose"},
        description = "Verbose output")
    private boolean verbose;

    @ParentCommand private QRCommand parent;

    @Inject private CLDFService cldfService;

    // For PicoCLI
    public GenerateCommand() {
      this.cldfService = null;
    }

    @Inject
    public GenerateCommand(CLDFService cldfService) {
      this.cldfService = cldfService;
    }

    @Override
    public Integer call() {
      if (cldfService == null) {
        System.err.println("Error: CLDFService not initialized");
        return 1;
      }
      
      // Create OutputHandler
      OutputHandler outputHandler = new OutputHandler(OutputFormat.text, verbose);
      
      try {

        // Load the archive
        CLDFArchive archive = cldfService.read(archivePath.toFile());

        // Find the entity by CLID
        String entityType = extractEntityType(clid);
        Object entity = findEntityByClid(archive, clid, entityType);

        if (entity == null) {
          outputHandler.writeError("Entity not found with CLID: " + clid);
          return 1;
        }

        // Generate QR code
        QRCodeGenerator generator = new DefaultQRCodeGenerator();
        QROptions qrOptions =
            QROptions.builder()
                .baseUrl(baseUrl)
                .includeIPFS(includeIPFS)
                .ipfsHash(ipfsHash)
                .build();

        QRImageOptions imageOptions = QRImageOptions.builder().size(size).build();

        BufferedImage qrImage;
        if (entity instanceof Route) {
          qrImage = generator.generateImage((Route) entity, qrOptions);
        } else if (entity instanceof Location) {
          qrImage = generator.generateImage((Location) entity, qrOptions);
        } else {
          outputHandler.writeError("Unsupported entity type for QR generation");
          return 1;
        }

        // Save the image
        File outputFile = outputPath.toFile();
        ImageIO.write(qrImage, "PNG", outputFile);

        outputHandler.writeInfo("QR code generated successfully: " + outputPath);
        outputHandler.writeInfo("Entity: " + entity.getClass().getSimpleName() + " [" + clid + "]");

        return 0;
      } catch (Exception e) {
        outputHandler.writeError("Failed to generate QR code: " + e.getMessage());
        if (verbose) {
          e.printStackTrace();
        }
        return 1;
      }
    }

    private String extractEntityType(String clid) {
      if (clid != null && clid.startsWith("clid:")) {
        String[] parts = clid.split(":");
        if (parts.length >= 2) {
          return parts[1];
        }
      }
      return null;
    }

    private Object findEntityByClid(CLDFArchive archive, String clid, String entityType) {
      if ("route".equals(entityType) && archive.getRoutes() != null) {
        return archive.getRoutes().stream()
            .filter(r -> clid.equals(r.getClid()))
            .findFirst()
            .orElse(null);
      }
      if ("location".equals(entityType) && archive.getLocations() != null) {
        return archive.getLocations().stream()
            .filter(l -> clid.equals(l.getClid()))
            .findFirst()
            .orElse(null);
      }
      return null;
    }
  }

  @Singleton
  @Command(name = "scan", description = "Scan and parse QR code from an image")
  static class ScanCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to QR code image")
    private Path imagePath;

    @Option(
        names = {"-o", "--output"},
        description = "Output format",
        defaultValue = "json")
    private String outputFormat;

    @Option(
        names = {"--extract-route"},
        description = "Extract route data from QR code")
    private boolean extractRoute;

    @Option(
        names = {"--extract-location"},
        description = "Extract location data from QR code")
    private boolean extractLocation;

    @Option(
        names = {"-v", "--verbose"},
        description = "Verbose output")
    private boolean verbose;

    @ParentCommand private QRCommand parent;

    // For PicoCLI
    public ScanCommand() {
    }

    @Override
    public Integer call() {
      // Create OutputHandler
      OutputHandler outputHandler = new OutputHandler(
          "json".equals(outputFormat) ? OutputFormat.json : OutputFormat.text, 
          verbose);
      
      try {

        // Read the image
        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null) {
          outputHandler.writeError("Failed to read image: " + imagePath);
          return 1;
        }

        // Scan the QR code
        QRScanner scanner = QRCodeFactory.createScanner();
        Result<ParsedQRData, QRError> result = scanner.scan(image);

        if (result.isFailure()) {
          outputHandler.writeError("Failed to scan QR code: " + result.getError().orElse(null));
          return 1;
        }

        ParsedQRData data = result.getSuccess().orElse(null);
        if (data == null) {
          outputHandler.writeError("No data found in QR code");
          return 1;
        }

        // Output parsed data
        outputHandler.writeInfo("QR code scanned successfully");
        outputHandler.writeJson(data);

        // Extract specific data if requested
        if (extractRoute && data.getRoute() != null) {
          Result<Route, QRError> routeResult = scanner.toRoute(data);
          if (routeResult.isSuccess()) {
            outputHandler.writeInfo("Extracted route:");
            outputHandler.writeJson(routeResult.getSuccess().orElse(null));
          }
        }

        if (extractLocation && data.getLocation() != null) {
          Result<Location, QRError> locationResult = scanner.toLocation(data);
          if (locationResult.isSuccess()) {
            outputHandler.writeInfo("Extracted location:");
            outputHandler.writeJson(locationResult.getSuccess().orElse(null));
          }
        }

        return 0;
      } catch (IOException e) {
        outputHandler.writeError("Failed to read image: " + e.getMessage());
        return 1;
      } catch (Exception e) {
        outputHandler.writeError("Error scanning QR code: " + e.getMessage());
        if (verbose) {
          e.printStackTrace();
        }
        return 1;
      }
    }
  }
}
