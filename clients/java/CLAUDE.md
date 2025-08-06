# CLDF Java Project Guidelines

## Architecture and Best Practices

### Dependency Injection

This project uses the Micronaut framework with constructor-based dependency injection. Follow these guidelines:

1. **Always use constructor injection** - Never use field injection with `@Inject` on fields
   ```java
   // ❌ BAD - Don't do this
   @Inject
   private MyService myService;
   
   // ✅ GOOD - Do this instead
   private final MyService myService;
   
   @Inject
   public MyClass(MyService myService) {
       this.myService = myService;
   }
   ```

2. **Services should be final fields** - This ensures immutability and thread safety

3. **Provide no-arg constructors for CLI commands** - PicoCLI requires no-arg constructors for command instantiation. When needed, provide both:
   ```java
   @Inject
   public MyCommand(MyService myService) {
       this.myService = myService;
   }
   
   // For PicoCLI framework
   public MyCommand() {
       this.myService = null;
   }
   ```

### Testing

- Use Spock framework for testing
- Mock dependencies using `Mock(ServiceClass)`
- When testing commands, inject mocks through constructor

### Code Quality

- Run `./gradlew spotlessApply` before committing to fix formatting issues
- Run `./gradlew test` to ensure all tests pass
- Run `./gradlew build` for a full build including tests and code quality checks

### Build Commands

- `./gradlew clean build` - Clean build with all checks
- `./gradlew :cldf-tool:test` - Run tests for cldf-tool module
- `./gradlew :cldf-java:test` - Run tests for cldf-java module
- `./gradlew spotlessApply` - Auto-fix code formatting
- `./gradlew shadowJar` - Build executable fat JAR

### Native Image Build

The project supports GraalVM native image compilation. Native reflection configuration is provided in:
- `cldf-tool/src/main/resources/META-INF/native-image/`

### Known Issues

1. Some CreateCommandSpec tests are temporarily disabled due to mock validation service interaction issues
2. When updating models or schemas, ensure both are synchronized to avoid validation failures