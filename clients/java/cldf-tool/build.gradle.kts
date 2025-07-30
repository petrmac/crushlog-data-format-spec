plugins {
    id("io.micronaut.application") version "4.3.5"
    id("io.micronaut.graalvm") version "4.3.5"
    id("com.diffplug.spotless") version "6.25.0"
    groovy
    id("io.freefair.lombok") version "8.6"
    id("idea")
}

// Group and version inherited from root project

repositories {
    mavenCentral()
}

micronaut {
    version.set("4.3.5")
}

// Versions
val mapstructVersion = "1.5.5.Final"
val neo4jVersion = "5.15.0"
val jmhVersion = "1.37"

dependencies {
    // ===== IMPLEMENTATION DEPENDENCIES =====
    // Internal dependency
    implementation(project(":cldf-java"))
    
    // Micronaut Framework
    implementation("io.micronaut:micronaut-runtime")
    implementation("io.micronaut:micronaut-inject")
    implementation("io.micronaut.picocli:micronaut-picocli")
    implementation("jakarta.annotation:jakarta.annotation-api")
    
    // CLI Framework
    implementation("info.picocli:picocli")
    
    // JSON Processing (explicit versions to ensure consistency)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    
    // MapStruct
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    
    // Logging
    implementation("org.slf4j:slf4j-api")
    
    // Neo4j Database
    implementation("org.neo4j:neo4j:$neo4jVersion") {
        exclude(group = "org.slf4j", module = "slf4j-nop")
        exclude(module = "neo4j-logging")
        exclude(module = "neo4j-slf4j-provider")
        exclude(group = "org.neo4j", module = "neo4j-slf4j-provider")
    }
    implementation("org.neo4j:neo4j-cypher-dsl:2024.0.0")
    
    // ===== COMPILE-ONLY DEPENDENCIES =====
    compileOnly("org.projectlombok:lombok")
    
    // ===== RUNTIME-ONLY DEPENDENCIES =====
    runtimeOnly("ch.qos.logback:logback-classic")
    
    // ===== ANNOTATION PROCESSORS =====
    annotationProcessor("io.micronaut:micronaut-inject-java")
    annotationProcessor("info.picocli:picocli-codegen")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    
    // ===== TEST IMPLEMENTATION DEPENDENCIES =====
    // Micronaut Testing
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("io.micronaut.test:micronaut-test-spock")
    
    // Spock Framework
    testImplementation("org.spockframework:spock-core:2.4-M1-groovy-4.0")
    testImplementation("org.apache.groovy:groovy-json:4.0.21")
    
    // JUnit and Mockito
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    
    // Neo4j Test Harness
    testImplementation("org.neo4j.test:neo4j-harness:$neo4jVersion")
    testImplementation("org.assertj:assertj-core:3.25.1")
    
    // JMH Benchmarking
    testImplementation("org.openjdk.jmh:jmh-core:$jmhVersion")
    
    // ===== TEST COMPILE-ONLY DEPENDENCIES =====
    testCompileOnly("org.projectlombok:lombok")
    
    // ===== TEST RUNTIME-ONLY DEPENDENCIES =====
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    
    // ===== TEST ANNOTATION PROCESSORS =====
    testAnnotationProcessor("org.projectlombok:lombok")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:$jmhVersion")
}

application {
    mainClass.set("io.cldf.tool.Application")
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("io.cldf.tool.*")
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("cldf")
            mainClass.set("io.cldf.tool.Application")
            buildArgs.add("--no-fallback")
            buildArgs.add("--enable-http")
            buildArgs.add("--enable-https")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--initialize-at-build-time=ch.qos.logback")
            buildArgs.add("--initialize-at-build-time=org.slf4j")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.databind.ObjectMapper")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.databind.type.TypeFactory")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.databind.type.TypeBindings")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.databind.type.TypeBase")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.databind.type.SimpleType")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.databind.cfg.BaseSettings")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.databind.util.internal.PrivateMaxEntriesMap")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.databind.util.ClassUtil")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.core.PrettyPrinter")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.core.io.JsonStringEncoder")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.core.io.SerializedString")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.core.Base64Variants")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.core.io.CharTypes")
            buildArgs.add("--initialize-at-run-time=com.fasterxml.jackson.annotation.JsonInclude\$Value")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

// Create a custom fat JAR task that produces an executable JAR
tasks.register<Jar>("fatJar") {
    dependsOn(configurations.runtimeClasspath)
    from(sourceSets.main.get().output)
    
    // Include all runtime dependencies
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        exclude("META-INF/MANIFEST.MF")
    }
    
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    
    manifest {
        attributes(
            "Main-Class" to "io.cldf.tool.Application",
            "Multi-Release" to "true"
        )
    }
    
    archiveBaseName.set("cldf-tool")
    archiveClassifier.set("standalone")
}

// JMH Benchmark configuration
tasks.register<JavaExec>("jmh") {
    dependsOn("testClasses")
    mainClass.set("org.openjdk.jmh.Main")
    classpath = sourceSets["test"].runtimeClasspath
    
    // JMH arguments
    args("-rf", "json")
    args("-rff", "build/jmh-results.json")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    
    // Configure annotation processing for MapStruct with Lombok
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.defaultComponentModel=default",
        "-Amapstruct.unmappedTargetPolicy=ERROR"
    ))
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat("1.22.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        importOrder("java", "javax", "jakarta", "", "\\#")
    }
}

// Configure IDEA plugin for better Lombok support
idea {
    module {
        // Ensure annotation processing is enabled
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

// Configure Lombok for better IDE support
configure<io.freefair.gradle.plugins.lombok.LombokExtension> {
    version.set("1.18.32")
}

// Configure the application plugin's built-in tasks
java {
    withSourcesJar()
    withJavadocJar()
}