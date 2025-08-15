package app.crushlog.cldf.tool.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;


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

        // Generate QR data
        QRCodeData qrData;
        if (entity instanceof Route) {
          qrData = generator.generateData((Route) entity, qrOptions);
        } else if (entity instanceof Location) {
          qrData = generator.generateData((Location) entity, qrOptions);
        } else {
          outputHandler.writeError("Unsupported entity type for QR generation");
          return 1;
        }

        // Generate payload string
        QRDataGenerator dataGen = new QRDataGenerator();
        String payload = switch (qrOptions.getFormat()) {
          case JSON -> dataGen.toJson(qrData);
          case URL -> qrData.getUrl();
          case CUSTOM_URI -> {
            // Determine type from entity
            String typeStr = entity instanceof Route ? "route" : "location";
            yield "cldf://" + typeStr + "/" + extractUuidFromClid(qrData.getClid());
          }
        };

        // Check if we're running in native image
        boolean isNativeImage = System.getProperty("org.graalvm.nativeimage.imagecode") != null;
        
        File outputFile = outputPath.toFile();
        String outputFileName = outputFile.getName();
        
        if (isNativeImage || outputFileName.endsWith(".svg")) {
          // Generate SVG (no AWT required)
          String svgContent = generator.generateSVG(payload, imageOptions);
          // Ensure output has .svg extension
          if (!outputFileName.endsWith(".svg")) {
            outputFile = new File(outputFile.getParentFile(), outputFileName.replaceAll("\\.[^.]*$", "") + ".svg");
          }
          Files.writeString(outputFile.toPath(), svgContent);
          outputHandler.writeInfo("QR code generated as SVG (native-image compatible): " + outputFile);
        } else {
          // Generate PNG using AWT (only in JVM mode)
          try {
            byte[] pngData = generator.generatePNG(payload, imageOptions);
            Files.write(outputFile.toPath(), pngData);
            outputHandler.writeInfo("QR code generated as PNG: " + outputFile);
          } catch (UnsatisfiedLinkError e) {
            // Fallback to SVG if AWT is not available
            outputHandler.writeWarning("AWT not available, generating SVG instead");
            String svgContent = generator.generateSVG(payload, imageOptions);
            outputFile = new File(outputFile.getParentFile(), outputFileName.replaceAll("\\.[^.]*$", "") + ".svg");
            Files.writeString(outputFile.toPath(), svgContent);
            outputHandler.writeInfo("QR code generated as SVG: " + outputFile);
          }
        }

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

    private String extractUuidFromClid(String clid) {
      if (clid == null) return "";
      String[] parts = clid.split(":");
      return parts.length >= 3 ? parts[2] : "";
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
      
      // Check if running in native image
      boolean isNativeImage = System.getProperty("org.graalvm.nativeimage.imagecode") != null;
      
      if (isNativeImage) {
        outputHandler.writeError("QR code scanning is not supported in native image mode (requires AWT).");
        outputHandler.writeInfo("Please use the standard Java runtime for QR code scanning:");
        outputHandler.writeInfo("  java -jar cldf-tool.jar qr scan <image-path>");
        return 1;
      }
      
      // This code will only run in JVM mode but we still can't use AWT classes
      // as they would prevent compilation for native image
      outputHandler.writeError("QR code scanning is temporarily disabled in this build.");
      outputHandler.writeInfo("The scan functionality requires AWT libraries which are not compatible with native image compilation.");
      outputHandler.writeInfo("To enable scanning, use a separate build without native image support.");
      return 1;
    }
  }
}
