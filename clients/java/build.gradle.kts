import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("io.freefair.lombok") version "8.6" apply false
    id("idea")
    id("org.sonarqube") version "6.2.0.5505"
}

// Configure IDEA plugin for root project
idea {
    project {
        setLanguageLevel("JDK_21")
        vcs = "Git"
    }
    module {
        name = rootProject.name
    }
}

// Configure SonarQube
sonar {
    properties {
        property("sonar.projectKey", "petrmac_crushlog-data-format-spec")
        property("sonar.organization", "petrmacek")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectName", "CrushLog Data Format Spec")
        property("sonar.projectVersion", version.toString())
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.java.source", "21")
        property("sonar.java.target", "21")
        property("sonar.coverage.jacoco.xmlReportPaths", "**/build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.junit.reportPaths", "**/build/test-results/test")
    }
}

// Configure SonarQube for subprojects
subprojects {
    sonar {
        properties {
            property("sonar.sources", "src/main/java")
            property("sonar.tests", "src/test")
            property("sonar.java.binaries", "build/classes/java/main")
            property("sonar.java.test.binaries", fileTree(layout.buildDirectory) {
                include("classes/java/test/**", "classes/groovy/test/**")
            }.files.joinToString(","))
        }
    }
}

// Ensure compilation happens before SonarQube analysis
tasks.named("sonar") {
    dependsOn(":cldf-java:build", ":cldf-tool:build")
}

allprojects {
    group = "io.cldf"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "jacoco")
    apply(plugin = "org.sonarqube")
    
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    repositories {
        mavenCentral()
    }
    
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
        options.encoding = "UTF-8"
    }
    
    // Configure IDEA for better Lombok support
    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }
    
    // Ensure Lombok version consistency
    configure<io.freefair.gradle.plugins.lombok.LombokExtension> {
        version.set("1.18.32")
    }
    
    // Configure annotation processing
    configurations {
        named("annotationProcessor") {
            extendsFrom(configurations["compileOnly"])
        }
    }
    
    // Configure JaCoCo for code coverage
    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }
    
    tasks.withType<JacocoReport> {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
    
    // Configure JaCoCo coverage verification
    tasks.withType<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                    minimum = 0.40.toBigDecimal()
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}