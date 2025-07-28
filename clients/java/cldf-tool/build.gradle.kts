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
    // Internal dependency on cldf-java
    implementation(project(":cldf-java"))
    
    // Micronaut
    implementation("io.micronaut:micronaut-runtime")
    implementation("io.micronaut:micronaut-inject")
    implementation("io.micronaut.picocli:micronaut-picocli")
    implementation("jakarta.annotation:jakarta.annotation-api")
    
    // Picocli
    implementation("info.picocli:picocli")
    annotationProcessor("info.picocli:picocli-codegen")
    
    // Jackson (for JsonUtils - these should come transitively from cldf-java but let's be explicit)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // MapStruct
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    
    // Logging
    runtimeOnly("ch.qos.logback:logback-classic")
    
    // Neo4j embedded database
    implementation("org.neo4j:neo4j:$neo4jVersion")
    implementation("org.neo4j:neo4j-cypher-dsl:2024.0.0")
    
    // Annotation processors
    annotationProcessor("io.micronaut:micronaut-inject-java")
    
    // Testing
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("io.micronaut.test:micronaut-test-spock")
    testImplementation("org.spockframework:spock-core:2.4-M1-groovy-4.0")
    testImplementation("org.apache.groovy:groovy-json:4.0.21")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    
    // Lombok for tests
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    
    // MapStruct for tests
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    
    // Integration testing
    testImplementation("org.neo4j.test:neo4j-harness:$neo4jVersion")
    testImplementation("org.assertj:assertj-core:3.25.1")
    
    // JMH for microbenchmarks
    testImplementation("org.openjdk.jmh:jmh-core:$jmhVersion")
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