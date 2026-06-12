// `java` (not just `base`) because github-gradle 1.8.3 eagerly reads JavaPluginExtension on the project it
// is applied to; the root only needs it for that, the actual library code lives in the subprojects.
plugins {
    java
    id("io.github.intisy.github-gradle") version "1.8.3"
}

// The release tag drives the version in CI (-Partifact_version=<tag>); 8.0.0-j8 is the local default.
version = (project.findProperty("artifact_version") ?: "8.0.0-j8").toString()

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "io.github.baked-libs"
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        // Fallback for dough-protection's volatile soft-dep plugin APIs whose remote artifacts drift or
        // get pruned upstream (the old pom even flags "bring down this repo count"); everything resolvable
        // comes from the remotes above.
        mavenLocal()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    dependencies {
        add("compileOnly", "io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
        add("compileOnly", "com.google.code.findbugs:jsr305:3.0.2")
        // Older Paper (1.18) exposed commons-lang transitively; 1.20.4 no longer does. Server/core provide
        // it at runtime (core relocates org.apache.commons.lang), so it's compile-only here.
        add("compileOnly", "commons-lang:commons-lang:2.6")
    }

    // JDK 17 toolchain + --release 8: Java-8 bytecode validated against the Java-8 API.
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(8)
        exclude("**/package-info.java")
    }

    // release 8 makes Gradle reject the (Java-17) paper-api as too new for the compile classpath. The
    // emitted bytecode stays Java 8; we only need the API at compile time (as Maven did), so accept it.
    configurations.matching { it.name.endsWith("ompileClasspath") }.configureEach {
        attributes {
            attribute(org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
        }
    }

    tasks.matching { it.name == "test" || it.name == "compileTestJava" }.configureEach {
        enabled = false
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}

// The plugin reads the API token only from this extension, so feed it the CI-provided GITHUB_TOKEN.
github {
    accessToken = System.getenv("GITHUB_TOKEN") ?: ""
}

// Publishes each subproject as a <dough-module>.jar asset on the release (owner/repo auto-detected from
// the git remote). Consume the whole set with githubImplementation("Slimefun5:dough:<tag>:all").
publishGithub {
    releaseName = "Release ${project.version}"
    artifacts {
        artifact { isModules = true }
    }
}
