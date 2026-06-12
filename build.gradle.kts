subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "io.github.baked-libs"
    version = "8.0.0-j8"

    repositories {
        mavenLocal() // paper-api 1.18.2-R0.1-SNAPSHOT is pruned from the remote; resolve it from cache
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    dependencies {
        add("compileOnly", "io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
        add("compileOnly", "com.google.code.findbugs:jsr305:3.0.2")
    }

    // JDK 17 toolchain + --release 8: Java-8 bytecode validated against the Java-8 API.
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(8)
        exclude("**/package-info.java")
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
