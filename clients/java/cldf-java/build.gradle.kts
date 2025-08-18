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

// Access parent's version catalog
val libVersions: Map<String, String> by rootProject.extra

dependencies {
    // ===== IMPLEMENTATION DEPENDENCIES =====
    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind:${libVersions["jackson"]}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${libVersions["jackson"]}")
    
    // JSON Schema Validation
    implementation("com.networknt:json-schema-validator:${libVersions["jsonSchemaValidator"]}")
    
    // MapStruct
    implementation("org.mapstruct:mapstruct:${libVersions["mapstruct"]}")
    
    // Archive handling
    implementation("org.apache.commons:commons-compress:${libVersions["commonsCompress"]}")
    
    // QR Code generation (core only, no AWT dependencies)
    implementation("com.google.zxing:core:${libVersions["zxing"]}")
    
    // ===== COMPILE-ONLY DEPENDENCIES =====
    // Lombok
    compileOnly("org.projectlombok:lombok")
    
    // ===== ANNOTATION PROCESSORS =====
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:${libVersions["mapstruct"]}")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:${libVersions["lombokMapstructBinding"]}")
    
    // ===== TEST IMPLEMENTATION DEPENDENCIES =====
    // Spock Testing
    testImplementation("org.spockframework:spock-core:${libVersions["spock"]}")
    testImplementation("org.apache.groovy:groovy:${libVersions["groovy"]}")
    testImplementation("org.mockito:mockito-core:${libVersions["mockito"]}")
    
    // ===== TEST COMPILE-ONLY DEPENDENCIES =====
    // Lombok for tests
    testCompileOnly("org.projectlombok:lombok")
    
    // ===== TEST ANNOTATION PROCESSORS =====
    testAnnotationProcessor("org.projectlombok:lombok")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:${libVersions["mapstruct"]}")
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
    
    repositories {
        // GitHub Packages repository
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/petrmac/crushlog-data-format-spec")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.token") as String?
            }
        }
    }
}

// Configure the java-library plugin's built-in tasks
java {
    withSourcesJar()
    withJavadocJar()
}

signing {
    // Get signing credentials from environment or project properties
    val signingKey: String? = System.getenv("MAVEN_GPG_PRIVATE_KEY") 
        ?: project.findProperty("signingKey") as String?
    val signingPassword: String? = System.getenv("MAVEN_GPG_PASSPHRASE") 
        ?: project.findProperty("signingPassword") as String?
    
    // Only configure signing if credentials are available
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}

// Make signing required only when publishing to Sonatype (not for local publishing)
tasks.withType<Sign> {
    onlyIf {
        // Only sign when actually publishing to Sonatype, not for publishToMavenLocal
        !gradle.taskGraph.hasTask(":publishToMavenLocal") &&
        !gradle.taskGraph.hasTask(":cldf-java:publishToMavenLocal") &&
        (gradle.taskGraph.hasTask(":publishToSonatype") || 
         gradle.taskGraph.hasTask(":cldf-java:publishMavenPublicationToSonatypeRepository") ||
         gradle.taskGraph.hasTask(":cldf-java:publishAllPublicationsToSonatypeRepository"))
    }
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