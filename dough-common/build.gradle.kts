plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    // compile-scope in the old pom -> api so consumers (incl. Slimefun core) get PaperLib transitively;
    // core relocates io.papermc.lib when shading.
    api("io.papermc:paperlib:1.0.7")
}
