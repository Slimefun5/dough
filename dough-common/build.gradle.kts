plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    // api so consumers get PaperLib transitively (Slimefun core relocates io.papermc.lib when shading).
    api("io.papermc:paperlib:1.0.7")
}
