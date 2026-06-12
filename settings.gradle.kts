plugins {
    // Auto-provisions the JDK used by the Java toolchain (so a Java 8 build needs no preinstalled JDK).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "dough"

// dough-skins (dead: unused + java.net.http) and the dough-api aggregator are intentionally omitted.
include(
    "dough-common",
    "dough-reflection",
    "dough-config",
    "dough-chat",
    "dough-data",
    "dough-items",
    "dough-inventories",
    "dough-protection",
    "dough-recipes",
    "dough-updater",
    "dough-scheduling"
)
