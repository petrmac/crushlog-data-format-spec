package app.crushlog.cldf.tool.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import app.crushlog.cldf.api.CLDFArchive;
import app.crushlog.cldf.clid.EntityType;
import app.crushlog.cldf.clid.RouteModel;
import app.crushlog.cldf.constants.CLDFConstants;
import app.crushlog.cldf.models.Location;
import app.crushlog.cldf.models.Route;
import app.crushlog.cldf.qr.*;
import app.crushlog.cldf.qr.impl.DefaultQRCodeGenerator;
import app.crushlog.cldf.qr.impl.QRDataGenerator;
import app.crushlog.cldf.qr.result.QRError;
import app.crushlog.cldf.qr.result.Result;
import app.crushlog.cldf.tool.services.CLDFService;
import app.crushlog.cldf.tool.utils.CLIDUtils;
import app.crushlog.cldf.tool.utils.OutputFormat;
import app.crushlog.cldf.tool.utils.OutputHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Slf4j
@Singleton
@Command(
    name = "qr",
    description = "Generate, scan, or parse QR codes for routes and locations",
    mixinStandardHelpOptions = true,
    subcommands = {
      QRCommand.GenerateCommand.class,
      QRCommand.ScanCommand.class,
      QRCommand.ParseCommand.class
    })
public class QRCommand implements Callable<Integer> {

  @Spec CommandSpec spec;

  public QRCommand() {
    // Constructor for PicoCLI framework
  }

  @Override
  public Integer call() {
    spec.commandLine().usage(System.out);
    return 0;
  }

  @Singleton
  @Command(name = "generate", description = "Generate QR code for a route or location")
  static class GenerateCommand implements Callable<Integer> {

    // Archive mode parameters
    @Parameters(
        index = "0",
        arity = "0..1",
        description = "Path to CLDF archive (for archive mode)")
    private Path archivePath;

    @Parameters(
        index = "1",
        arity = "0..1",
        description = "CLID of the route or location (for archive mode)")
    private String archiveClid;

    // Direct mode parameters
    @Option(
        names = {"--clid"},
        description = "CLID for direct generation mode")
    private String directClid;

    @Option(
        names = {"--type"},
        description = "Entity type (route or location)")
    private String entityType;

    @Option(
        names = {"--name"},
        description = "Name of the route or location")
    private String name;

    @Option(
        names = {"--grade"},
        description = "Grade (for routes)")
    private String grade;

    @Option(
        names = {"--route-type"},
        description = "Route type (sport, boulder, trad, etc.)")
    private String routeType;

    @Option(
        names = {"--location-id"},
        description = "Location ID (for routes)")
    private Integer locationId;

    @Option(
        names = {"--location-name"},
        description = "Location name (for routes without archive)")
    private String locationName;

    @Option(
        names = {"--location-clid"},
        description = "Location CLID (for routes without archive)")
    private String locationClid;

    @Option(
        names = {"--country"},
        description = "Country (for locations)")
    private String country;

    @Option(
        names = {"--state"},
        description = "State/Province (for locations)")
    private String state;

    @Option(
        names = {"--city"},
        description = "City (for locations)")
    private String city;

    @Option(
        names = {"--latitude"},
        description = "Latitude (for locations)")
    private Double latitude;

    @Option(
        names = {"--longitude"},
        description = "Longitude (for locations)")
    private Double longitude;

    @Option(
        names = {"--indoor"},
        description = "Is indoor location")
    private boolean indoor;

    @Option(
        names = {"--height"},
        description = "Height in meters (for routes)")
    private Double height;

    @Option(
        names = {"--first-ascent-name"},
        description = "First ascent climber name")
    private String firstAscentName;

    @Option(
        names = {"--first-ascent-year"},
        description = "First ascent year")
    private Integer firstAscentYear;

    @Option(
        names = {"-o", "--output"},
        description = "Output file path (PNG or SVG format)",
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
        names = {"--format"},
        description = "QR code data format (json, url, uri)",
        defaultValue = "json")
    private String format;

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

      OutputHandler outputHandler = new OutputHandler(OutputFormat.TEXT, verbose);

      try {
        // Determine which mode to use
        if (archivePath != null && archiveClid != null) {
          // Archive mode
          return generateFromArchive(outputHandler);
        } else if (entityType != null) {
          // Direct mode
          return generateDirect(outputHandler);
        } else {
          outputHandler.writeError(
              "Either provide archive path with CLID, or use --type with entity parameters");
          return 1;
        }
      } catch (Exception e) {
        outputHandler.writeError("Failed to generate QR code: " + e.getMessage());
        if (verbose) {
          e.printStackTrace();
        }
        return 1;
      }
    }

    private Integer generateFromArchive(OutputHandler outputHandler) throws Exception {
      CLDFArchive archive = cldfService.read(archivePath.toFile());
      Object entity = findEntity(archive, archiveClid);

      if (entity == null) {
        outputHandler.writeError("Entity not found with CLID: " + archiveClid);
        return 1;
      }

      String qrContent = generateQRContent(entity, outputHandler);
      if (qrContent == null) {
        return 1;
      }

      return saveQRCode(qrContent, outputHandler);
    }

    private Integer generateDirect(OutputHandler outputHandler) throws Exception {
      // Validate required fields
      if (name == null || name.isEmpty()) {
        outputHandler.writeError("Name is required for direct generation");
        return 1;
      }

      Object entity;
      String generatedClid = directClid;

      if ("route".equalsIgnoreCase(entityType)) {
        entity = createRouteForDirectMode();
        if (generatedClid == null) {
          generatedClid = generateRouteCLIDDirect();
        }
      } else if ("location".equalsIgnoreCase(entityType)) {
        entity = createLocationForDirectMode();
        if (generatedClid == null) {
          generatedClid = generateLocationCLIDDirect();
        }
      } else {
        outputHandler.writeError("Invalid entity type. Must be 'route' or 'location'");
        return 1;
      }

      if (entity == null) {
        outputHandler.writeError("Failed to create entity for QR generation");
        return 1;
      }

      // Set the CLID on the entity
      if (entity instanceof Route) {
        ((Route) entity).setClid(generatedClid);
      } else if (entity instanceof Location) {
        ((Location) entity).setClid(generatedClid);
      }

      outputHandler.writeInfo("Generated CLID: " + generatedClid);

      String qrContent = generateQRContent(entity, outputHandler);
      if (qrContent == null) {
        return 1;
      }

      return saveQRCode(qrContent, outputHandler);
    }

    private Route createRouteForDirectMode() {
      Route route = new Route();
      route.setName(name);

      setRouteLocationId(route);
      setRouteGrades(route);
      setRouteType(route);
      setRouteHeight(route);
      setRouteFirstAscent(route);

      return route;
    }

    private void setRouteLocationId(Route route) {
      if (locationId != null) {
        route.setLocationId(locationId);
      }
    }

    private void setRouteGrades(Route route) {
      if (grade != null) {
        Route.Grades grades = new Route.Grades();
        setGradeByPattern(grades, grade);
        route.setGrades(grades);
      }
    }

    void setGradeByPattern(Route.Grades grades, String gradeValue) {
      if (gradeValue.matches("5\\..*")) {
        grades.setYds(gradeValue);
      } else if (gradeValue.matches("V\\d{1,2}([+-])?$")) {
        grades.setVScale(gradeValue);
      } else if (gradeValue.matches("\\d+[abc]?\\+?")) {
        grades.setFrench(gradeValue);
      } else {
        // Default to YDS
        grades.setYds(gradeValue);
      }
    }

    private void setRouteType(Route route) {
      if (routeType != null) {
        try {
          if (routeType.equalsIgnoreCase("boulder")) {
            route.setRouteType(app.crushlog.cldf.models.enums.RouteType.BOULDER);
          } else {
            route.setRouteType(app.crushlog.cldf.models.enums.RouteType.ROUTE);
          }
        } catch (Exception e) {
          // Default to route
          route.setRouteType(app.crushlog.cldf.models.enums.RouteType.ROUTE);
        }
      }
    }

    private void setRouteHeight(Route route) {
      if (height != null) {
        route.setHeight(height);
      }
    }

    private void setRouteFirstAscent(Route route) {
      if (firstAscentName != null || firstAscentYear != null) {
        Route.FirstAscent fa = new Route.FirstAscent();
        fa.setName(firstAscentName);
        if (firstAscentYear != null) {
          fa.setDate(java.time.LocalDate.of(firstAscentYear, 1, 1));
        }
        route.setFirstAscent(fa);
      }
    }

    private Location createLocationForDirectMode() {
      if (country == null || latitude == null || longitude == null) {
        return null;
      }

      Location location = new Location();
      location.setName(name);
      location.setCountry(country);
      location.setState(state);
      location.setCity(city);
      location.setIsIndoor(indoor);

      Location.Coordinates coords = new Location.Coordinates();
      coords.setLatitude(latitude);
      coords.setLongitude(longitude);
      location.setCoordinates(coords);

      return location;
    }

    private String generateRouteCLIDDirect() {
      // For direct mode route CLID generation
      if (locationClid != null && grade != null) {
        // Create minimal route model for CLID generation
        RouteModel.Route routeModel = getRouteModel();

        return app.crushlog.cldf.clid.CLIDGenerator.generateRouteCLID(locationClid, routeModel);
      }

      // Fallback to random CLID
      return app.crushlog.cldf.clid.CLIDGenerator.generateRandomCLID(EntityType.ROUTE);
    }

    private RouteModel.Route getRouteModel() {
      RouteModel.RouteType type = RouteModel.RouteType.SPORT;
      if (routeType != null) {
        try {
          if (routeType.equalsIgnoreCase("boulder")) {
            type = RouteModel.RouteType.BOULDER;
          }
        } catch (Exception e) {
          // Use default
        }
      }

      RouteModel.FirstAscent fa = null;
      if (firstAscentName != null || firstAscentYear != null) {
        fa = new RouteModel.FirstAscent(firstAscentName, firstAscentYear);
      }

      return new RouteModel.Route(name, grade, type, fa, height);
    }

    private String generateLocationCLIDDirect() {
      // For direct mode location CLID generation
      if (country != null && latitude != null && longitude != null) {
        app.crushlog.cldf.clid.Location locModel =
            new app.crushlog.cldf.clid.Location(
                country,
                state,
                city,
                name,
                new app.crushlog.cldf.clid.Coordinates(latitude, longitude),
                indoor);

        return app.crushlog.cldf.clid.CLIDGenerator.generateLocationCLID(locModel);
      }

      // Fallback to random CLID
      return app.crushlog.cldf.clid.CLIDGenerator.generateRandomCLID(EntityType.LOCATION);
    }

    private String generateQRContent(Object entity, OutputHandler outputHandler) {
      try {
        QRCodeGenerator generator = new DefaultQRCodeGenerator();
        QROptions qrOptions = buildQROptions();

        QRCodeData qrData = generateQRData(generator, entity, qrOptions);
        if (qrData == null) {
          outputHandler.writeError("Unsupported entity type for QR generation");
          return null;
        }

        return createPayload(qrData, qrOptions);
      } catch (Exception e) {
        outputHandler.writeError("Failed to generate QR content: " + e.getMessage());
        return null;
      }
    }

    private QROptions buildQROptions() {
      QROptions.QRDataFormat dataFormat =
          switch (format.toLowerCase()) {
            case "url" -> QROptions.QRDataFormat.URL;
            case "uri" -> QROptions.QRDataFormat.CUSTOM_URI;
            default -> QROptions.QRDataFormat.JSON;
          };

      return QROptions.builder()
          .baseUrl(baseUrl)
          .includeIPFS(includeIPFS)
          .ipfsHash(ipfsHash)
          .format(dataFormat)
          .build();
    }

    private QRCodeData generateQRData(QRCodeGenerator generator, Object entity, QROptions options) {
      if (entity instanceof Route) {
        return generator.generateData((Route) entity, options);
      } else if (entity instanceof Location) {
        return generator.generateData((Location) entity, options);
      }
      return null;
    }

    private String createPayload(QRCodeData qrData, QROptions qrOptions) {
      QRDataGenerator dataGen = new QRDataGenerator();
      return switch (qrOptions.getFormat()) {
        case JSON -> dataGen.toJson(qrData);
        case URL -> qrData.getUrl();
        case CUSTOM_URI -> createCustomUri(qrData);
      };
    }

    private String createCustomUri(QRCodeData qrData) {
      return CLIDUtils.toCustomUri(qrData.getClid())
          .orElseThrow(
              () -> new IllegalArgumentException("Invalid CLID format: " + qrData.getClid()));
    }

    private int saveQRCode(String payload, OutputHandler outputHandler) throws IOException {
      QRCodeGenerator generator = new DefaultQRCodeGenerator();
      QRImageOptions imageOptions = QRImageOptions.builder().size(size).build();

      File outputFile = outputPath.toFile();
      String outputFileName = outputFile.getName().toLowerCase();

      if (outputFileName.endsWith(".svg")) {
        return saveSVG(generator, payload, imageOptions, outputFile, outputHandler);
      } else {
        return savePNG(generator, payload, imageOptions, outputFile, outputHandler);
      }
    }

    private int saveSVG(
        QRCodeGenerator generator,
        String payload,
        QRImageOptions imageOptions,
        File outputFile,
        OutputHandler outputHandler)
        throws IOException {
      String svgContent = generator.generateSVG(payload, imageOptions);
      Files.writeString(outputFile.toPath(), svgContent);
      outputHandler.writeInfo("QR code generated as SVG: " + outputFile);
      return 0;
    }

    private int savePNG(
        QRCodeGenerator generator,
        String payload,
        QRImageOptions imageOptions,
        File outputFile,
        OutputHandler outputHandler)
        throws IOException {
      byte[] pngData = generator.generatePNG(payload, imageOptions);
      Files.write(outputFile.toPath(), pngData);
      outputHandler.writeInfo("QR code generated as PNG: " + outputFile);
      return 0;
    }

    private Object findEntity(CLDFArchive archive, String clid) {
      Optional<String> entityType = CLIDUtils.extractEntityType(clid);

      if (entityType.isPresent()) {
        if ("route".equals(entityType.get())) {
          return findRoute(archive, clid).orElse(null);
        } else if ("location".equals(entityType.get())) {
          return findLocation(archive, clid).orElse(null);
        }
      }
      return null;
    }

    private Optional<Route> findRoute(CLDFArchive archive, String clid) {
      if (archive.getRoutes() == null) {
        return Optional.empty();
      }
      return archive.getRoutes().stream().filter(r -> clid.equals(r.getClid())).findFirst();
    }

    private Optional<Location> findLocation(CLDFArchive archive, String clid) {
      if (archive.getLocations() == null) {
        return Optional.empty();
      }
      return archive.getLocations().stream().filter(l -> clid.equals(l.getClid())).findFirst();
    }
  }

  @Singleton
  @Command(name = "scan", description = "Scan QR code from image file")
  static class ScanCommand implements Callable<Integer> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Parameters(index = "0", description = "Path to image file containing QR code")
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

    @Inject private QRScanner qrScanner;

    // For PicoCLI framework
    public ScanCommand() {
      this.qrScanner = null;
    }

    @Inject
    public ScanCommand(QRScanner qrScanner) {
      this.qrScanner = qrScanner;
    }

    @Override
    public Integer call() {
      OutputFormat format = "json".equals(outputFormat) ? OutputFormat.JSON : OutputFormat.TEXT;
      OutputHandler outputHandler = new OutputHandler(format, verbose);

      try {
        // Read image file
        byte[] imageBytes = Files.readAllBytes(imagePath);

        // Scan QR code from image
        if (qrScanner == null) {
          outputHandler.writeError("QRScanner not initialized");
          return 1;
        }
        Result<ParsedQRData, QRError> result = qrScanner.scan(imageBytes);

        if (result.isFailure()) {
          handleScanFailure(result, outputHandler);
          return 1;
        }

        ParsedQRData parsedData =
            result
                .getSuccess()
                .orElseThrow(() -> new IllegalStateException("Result success but no data"));

        outputParsedData(parsedData, outputHandler);

        if (extractRoute || extractLocation) {
          extractEntities(qrScanner, parsedData, outputHandler);
        }

        return 0;

      } catch (IOException e) {
        outputHandler.writeError("Failed to read image file: " + e.getMessage());
        return 1;
      } catch (Exception e) {
        outputHandler.writeError(CLDFConstants.FAILED_TO_SCAN_QR_CODE + e.getMessage());
        if (verbose) {
          e.printStackTrace();
        }
        return 1;
      }
    }

    private void handleScanFailure(
        Result<ParsedQRData, QRError> result, OutputHandler outputHandler) {
      QRError error = result.getError().orElse(QRError.scanError("Unknown error"));
      outputHandler.writeError(CLDFConstants.FAILED_TO_SCAN_QR_CODE + error.getMessage());
      if (verbose && error.getDetails() != null) {
        outputHandler.writeError("Details: " + error.getDetails());
      }
    }

    private void outputParsedData(ParsedQRData parsedData, OutputHandler outputHandler)
        throws Exception {
      if ("json".equals(outputFormat)) {
        String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(parsedData);
        outputHandler.write(json);
      } else {
        outputHandler.write(formatAsText(parsedData));
      }
    }

    private void extractEntities(
        QRScanner scanner, ParsedQRData parsedData, OutputHandler outputHandler) {
      if (extractRoute) {
        extractRoute(scanner, parsedData, outputHandler);
      }

      if (extractLocation) {
        extractLocation(scanner, parsedData, outputHandler);
      }
    }

    private void extractRoute(
        QRScanner scanner, ParsedQRData parsedData, OutputHandler outputHandler) {
      Result<Route, QRError> routeResult = scanner.toRoute(parsedData);
      if (routeResult.isSuccess()) {
        routeResult
            .getSuccess()
            .ifPresent(route -> outputHandler.writeInfo(CLDFConstants.EXTRACTED_ROUTE + route));
      } else {
        routeResult
            .getError()
            .ifPresent(
                error ->
                    outputHandler.writeWarning(
                        CLDFConstants.FAILED_TO_EXTRACT_ROUTE + error.getMessage()));
      }
    }

    private void extractLocation(
        QRScanner scanner, ParsedQRData parsedData, OutputHandler outputHandler) {
      Result<Location, QRError> locationResult = scanner.toLocation(parsedData);
      if (locationResult.isSuccess()) {
        locationResult
            .getSuccess()
            .ifPresent(
                location -> outputHandler.writeInfo(CLDFConstants.EXTRACTED_LOCATION + location));
      } else {
        locationResult
            .getError()
            .ifPresent(
                error ->
                    outputHandler.writeWarning(
                        CLDFConstants.FAILED_TO_EXTRACT_LOCATION + error.getMessage()));
      }
    }

    private String formatAsText(ParsedQRData data) {
      StringBuilder sb = new StringBuilder();
      sb.append("QR Code Data:\n");
      sb.append("  Version: ").append(data.getVersion()).append("\n");

      appendIfPresent(sb, "CLID", data.getClid());
      appendIfPresent(sb, "URL", data.getUrl());
      appendIfPresent(sb, "IPFS Hash", data.getIpfsHash());

      if (data.getRoute() != null) {
        appendRouteInfo(sb, data.getRoute());
      }

      if (data.getLocation() != null) {
        appendLocationInfo(sb, data.getLocation());
      }

      sb.append("  Has Offline Data: ").append(data.isHasOfflineData()).append("\n");

      if (data.isBlockchainVerified()) {
        sb.append("  Blockchain Verified: true\n");
      }

      return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
      if (value != null) {
        sb.append("  ").append(label).append(": ").append(value).append("\n");
      }
    }

    private void appendRouteInfo(StringBuilder sb, ParsedQRData.RouteInfo route) {
      sb.append("  Route:\n");
      appendIfPresent(sb, "    ID", route.getId() != null ? route.getId().toString() : null);
      appendIfPresent(sb, "    Name", route.getName());
      appendIfPresent(sb, "    Grade", route.getGrade());
      appendIfPresent(sb, "    Grade System", route.getGradeSystem());
      appendIfPresent(sb, "    Type", route.getType());
      if (route.getHeight() != null) {
        sb.append("    Height: ").append(route.getHeight()).append("m\n");
      }
    }

    private void appendLocationInfo(StringBuilder sb, ParsedQRData.LocationInfo location) {
      sb.append("  Location:\n");
      appendIfPresent(sb, "    ID", location.getId() != null ? location.getId().toString() : null);
      appendIfPresent(sb, "    Name", location.getName());
      appendIfPresent(sb, "    Country", location.getCountry());
      appendIfPresent(sb, "    State", location.getState());
      appendIfPresent(sb, "    City", location.getCity());
      sb.append("    Indoor: ").append(location.isIndoor()).append("\n");
    }
  }

  @Singleton
  @Command(name = "parse", description = "Parse QR code data from text string")
  static class ParseCommand implements Callable<Integer> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Parameters(index = "0", description = "QR code data string or file containing QR data")
    private String input;

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

    @Inject private QRScanner qrScanner;

    // For PicoCLI framework
    public ParseCommand() {
      this.qrScanner = null;
    }

    @Inject
    public ParseCommand(QRScanner qrScanner) {
      this.qrScanner = qrScanner;
    }

    @Override
    public Integer call() {
      OutputFormat format = "json".equals(outputFormat) ? OutputFormat.JSON : OutputFormat.TEXT;
      OutputHandler outputHandler = new OutputHandler(format, verbose);

      try {
        // Check if input is a file path
        String qrData;
        Path inputPath = Path.of(input);
        if (Files.exists(inputPath)) {
          qrData = Files.readString(inputPath);
        } else {
          qrData = input;
        }

        if (qrScanner == null) {
          outputHandler.writeError("QRScanner not initialized");
          return 1;
        }
        Result<ParsedQRData, QRError> result = qrScanner.parse(qrData);

        if (result.isFailure()) {
          handleScanFailure(result, outputHandler);
          return 1;
        }

        ParsedQRData parsedData =
            result
                .getSuccess()
                .orElseThrow(() -> new IllegalStateException("Result success but no data"));

        outputParsedData(parsedData, outputHandler);

        if (extractRoute || extractLocation) {
          extractEntities(qrScanner, parsedData, outputHandler);
        }

        return 0;

      } catch (IOException e) {
        outputHandler.writeError("Failed to read input file: " + e.getMessage());
        return 1;
      } catch (Exception e) {
        outputHandler.writeError("Failed to parse QR code: " + e.getMessage());
        if (verbose) {
          e.printStackTrace();
        }
        return 1;
      }
    }

    private void handleScanFailure(
        Result<ParsedQRData, QRError> result, OutputHandler outputHandler) {
      QRError error = result.getError().orElse(QRError.scanError("Unknown error"));
      outputHandler.writeError(CLDFConstants.FAILED_TO_SCAN_QR_CODE + error.getMessage());
      if (verbose && error.getDetails() != null) {
        outputHandler.writeError("Details: " + error.getDetails());
      }
    }

    private void outputParsedData(ParsedQRData parsedData, OutputHandler outputHandler)
        throws Exception {
      if ("json".equals(outputFormat)) {
        String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(parsedData);
        outputHandler.write(json);
      } else {
        outputHandler.write(formatAsText(parsedData));
      }
    }

    private void extractEntities(
        QRScanner scanner, ParsedQRData parsedData, OutputHandler outputHandler) {
      if (extractRoute) {
        extractRoute(scanner, parsedData, outputHandler);
      }

      if (extractLocation) {
        extractLocation(scanner, parsedData, outputHandler);
      }
    }

    private void extractRoute(
        QRScanner scanner, ParsedQRData parsedData, OutputHandler outputHandler) {
      Result<Route, QRError> routeResult = scanner.toRoute(parsedData);
      if (routeResult.isSuccess()) {
        routeResult
            .getSuccess()
            .ifPresent(route -> outputHandler.writeInfo(CLDFConstants.EXTRACTED_ROUTE + route));
      } else {
        routeResult
            .getError()
            .ifPresent(
                error ->
                    outputHandler.writeWarning(
                        CLDFConstants.FAILED_TO_EXTRACT_ROUTE + error.getMessage()));
      }
    }

    private void extractLocation(
        QRScanner scanner, ParsedQRData parsedData, OutputHandler outputHandler) {
      Result<Location, QRError> locationResult = scanner.toLocation(parsedData);
      if (locationResult.isSuccess()) {
        locationResult
            .getSuccess()
            .ifPresent(
                location -> outputHandler.writeInfo(CLDFConstants.EXTRACTED_LOCATION + location));
      } else {
        locationResult
            .getError()
            .ifPresent(
                error ->
                    outputHandler.writeWarning(
                        CLDFConstants.FAILED_TO_EXTRACT_LOCATION + error.getMessage()));
      }
    }

    private String formatAsText(ParsedQRData data) {
      StringBuilder sb = new StringBuilder();
      sb.append("QR Code Data:\n");
      sb.append("  Version: ").append(data.getVersion()).append("\n");

      appendIfPresent(sb, "CLID", data.getClid());
      appendIfPresent(sb, "URL", data.getUrl());
      appendIfPresent(sb, "IPFS Hash", data.getIpfsHash());

      if (data.getRoute() != null) {
        appendRouteInfo(sb, data.getRoute());
      }

      if (data.getLocation() != null) {
        appendLocationInfo(sb, data.getLocation());
      }

      sb.append("  Has Offline Data: ").append(data.isHasOfflineData()).append("\n");

      if (data.isBlockchainVerified()) {
        sb.append("  Blockchain Verified: true\n");
      }

      return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
      if (value != null) {
        sb.append("  ").append(label).append(": ").append(value).append("\n");
      }
    }

    private void appendRouteInfo(StringBuilder sb, ParsedQRData.RouteInfo route) {
      sb.append("  Route:\n");
      appendIfPresent(sb, "    ID", route.getId() != null ? route.getId().toString() : null);
      appendIfPresent(sb, "    Name", route.getName());
      appendIfPresent(sb, "    Grade", route.getGrade());
      appendIfPresent(sb, "    Grade System", route.getGradeSystem());
      appendIfPresent(sb, "    Type", route.getType());
      if (route.getHeight() != null) {
        sb.append("    Height: ").append(route.getHeight()).append("m\n");
      }
    }

    private void appendLocationInfo(StringBuilder sb, ParsedQRData.LocationInfo location) {
      sb.append("  Location:\n");
      appendIfPresent(sb, "    ID", location.getId() != null ? location.getId().toString() : null);
      appendIfPresent(sb, "    Name", location.getName());
      appendIfPresent(sb, "    Country", location.getCountry());
      appendIfPresent(sb, "    State", location.getState());
      appendIfPresent(sb, "    City", location.getCity());
      sb.append("    Indoor: ").append(location.isIndoor()).append("\n");
    }
  }
}
