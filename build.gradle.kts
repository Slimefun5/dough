// Gradle build for the Java-8 dough fork (was Maven). Each module publishes
// io.github.baked-libs:dough-<module>:8.0.0-j8 to mavenLocal, which Slimefun core consumes + shades.
//
// Compiled with a Java 17 toolchain but `--release 8`, which emits Java-8 bytecode AND validates against
// the Java-8 API (so Java 9+ methods that would NoSuchMethodError on a legacy server are caught at compile
// time) - the same intent as the old pom's maven.compiler.release=8, without needing a JDK 8 installed.

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "io.github.baked-libs"
    version = "8.0.0-j8"

    repositories {
        // First: old Paper snapshots (e.g. paper-api 1.18.2-R0.1-SNAPSHOT) get pruned from the remote
        // but remain cached in ~/.m2, so resolve those (and any other cached APIs) locally.
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    dependencies {
        // provided-scope in the old poms (parent): present at compile, never bundled/transitive.
        add("compileOnly", "io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
        add("compileOnly", "com.google.code.findbugs:jsr305:3.0.2")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(8)
        // The old pom excluded package-info.java from compilation.
        exclude("**/package-info.java")
    }

    // The fork's tests use MockBukkit/JUnit which we don't wire up here (matches the old `-Dmaven.test.skip`).
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
