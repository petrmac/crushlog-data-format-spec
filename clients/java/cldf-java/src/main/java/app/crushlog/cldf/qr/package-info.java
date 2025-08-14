/**
 * QR code generation and scanning for CLDF entities using functional programming patterns.
 *
 * <p>This package provides comprehensive QR code support for CLDF, enabling:
 *
 * <ul>
 *   <li>Generation of QR codes for routes and locations with embedded data
 *   <li>Hybrid QR codes containing both offline data and online references
 *   <li>Support for IPFS and blockchain integration
 *   <li>Scanning and parsing of QR codes in multiple formats
 *   <li>Error handling without exceptions using functional Result patterns
 * </ul>
 *
 * <p>The QR code system uses CLIDs (CrushLog IDs) for global uniqueness.
 *
 * <h2>Architecture</h2>
 *
 * <p>The package provides two approaches for QR code operations:
 *
 * <ul>
 *   <li><b>Interface-based (recommended)</b>: {@link QRCodeGenerator} with dependency injection
 *       support
 *   <li><b>Static utility</b>: {@link QRGenerator} for simple use cases
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Generate QR code for a route (Interface-based)</h3>
 *
 * <pre>{@code
 * Route route = Route.builder()
 *     .name("The Nose")
 *     .grades(Route.Grades.builder().yds("5.14a").build())
 *     .build();
 *
 * QRCodeGenerator generator = QRCodeFactory.createGenerator();
 * QRCodeData qrData = generator.generateData(route, QROptions.builder().build());
 * BufferedImage image = generator.generateImage(route, QROptions.builder().build());
 * }</pre>
 *
 * <h3>Generate QR code using static utility</h3>
 *
 * <pre>{@code
 * // Static convenience method for simple use cases
 * BufferedImage qrImage = QRGenerator.generateQRCodeImage(
 *     route,
 *     QRGenerator.QROptions.builder()
 *         .size(256)
 *         .errorCorrectionLevel(ErrorCorrectionLevel.M)
 *         .build()
 * );
 * }</pre>
 *
 * <h3>Parse QR code with error handling</h3>
 *
 * <pre>{@code
 * QRScanner scanner = QRCodeFactory.createScanner();
 *
 * // Using Result type for error handling
 * Result<ParsedQRData, QRError> result = scanner.scan(image);
 *
 * result
 *     .map(data -> data.getRouteInfo())
 *     .ifSuccess(route -> System.out.println("Found route: " + route.getName()))
 *     .ifFailure(error -> System.err.println("Error: " + error.getMessage()));
 * }</pre>
 *
 * <h3>Static scanning methods</h3>
 *
 * <pre>{@code
 * // Static convenience methods for simple use cases
 * ParsedQRData data = QRScanner.parse(scannedString);
 * Optional<Route> route = QRScanner.toRoute(data);
 * }</pre>
 *
 * @since 1.3.0
 */
package app.crushlog.cldf.qr;
