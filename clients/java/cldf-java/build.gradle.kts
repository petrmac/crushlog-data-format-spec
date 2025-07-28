plugins {
    `java-library`
    groovy
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "6.25.0"
    id("io.freefair.lombok") version "8.6"
    id("idea")
}

// Group and version inherited from root project
description = "Java client library for CrushLog Data Format (CLDF)"

java {
    // Source/target compatibility inherited from root project
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

// MapStruct version
val mapstructVersion = "1.5.5.Final"

dependencies {
    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    
    // JSON Schema Validation
    implementation("com.networknt:json-schema-validator:1.3.3")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // MapStruct
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    
    // Archive handling
    implementation("org.apache.commons:commons-compress:1.26.1")
    
    // Spock Testing
    testImplementation("org.spockframework:spock-core:2.4-M1-groovy-4.0")
    testImplementation("org.apache.groovy:groovy:4.0.21")
    testImplementation("org.mockito:mockito-core:5.11.0")
    
    // Lombok for tests
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    
    // MapStruct for tests
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
    options.encoding = "UTF-8"
    
    // Configure annotation processing for MapStruct with Lombok
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.defaultComponentModel=default",
        "-Amapstruct.unmappedTargetPolicy=ERROR"
    ))
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all", "-quiet")
        if (JavaVersion.current().isJava9Compatible) {
            addBooleanOption("html5", true)
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("CLDF Java Client")
                description.set("Java client library for CrushLog Data Format (CLDF)")
                url.set("https://github.com/cldf/cldf-java")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        name.set("CLDF Team")
                        email.set("info@crushlog.app")
                        organization.set("CLDF")
                        organizationUrl.set("https://cldf.io")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/cldf/cldf-java.git")
                    developerConnection.set("scm:git:ssh://github.com:cldf/cldf-java.git")
                    url.set("https://github.com/cldf/cldf-java")
                }
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat("1.22.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        importOrder("java", "javax", "", "\\#")
    }
    groovy {
        target("src/*/groovy/**/*.groovy")
        greclipse()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Configure IDEA plugin for better module naming
idea {
    module {
        name = "cldf-java"
    }
}