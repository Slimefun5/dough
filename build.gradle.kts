plugins {
    // Aggregator root: `base` provides the lifecycle tasks (build/assemble/clean) that publishGithub
    // depends on. The actual library code lives in the subprojects.
    base
    id("io.github.intisy.github-gradle") version "1.8.3.1"
}

version = (project.findProperty("artifact_version") ?: "8.0.0-j8").toString()

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "io.github.baked-libs"
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
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

github {
    accessToken = System.getenv("GITHUB_TOKEN") ?: ""
}

publishGithub {
    releaseName = "Release ${project.version}"
    artifacts {
        artifact { isModules = true }
    }
}
