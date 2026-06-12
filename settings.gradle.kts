plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "dough"

// dough-skins (unused) and the dough-api aggregator are intentionally omitted.
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
