plugins {
    `java-library`
    `maven-publish`
}

// Repositories hosting the protection-plugin APIs (added to the inherited mavenCentral + papermc).
repositories {
    maven("https://jitpack.io/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://maven.playpro.com/")                                  // CoreProtect
    maven("https://raw.githubusercontent.com/FabioZumbi12/RedProtect/mvn-repo/")
    maven("https://ci.ender.zone/plugin/repository/everything/")         // FactionsUUID
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")                           // WorldEdit/WorldGuard
    maven("https://repo.panda-lang.org/releases")                        // FunnyGuilds
    maven("https://www.iani.de/nexus/content/repositories/snapshots/")   // LogBlock
    maven("https://repo.william278.net/snapshots/")                      // HuskTowns/HuskClaims
}

configurations.configureEach {
    // sk89q poms cross-reference each other with the "latest.integration" metaversion, which Gradle can't
    // resolve; pin it. mypet is an unpublished transitive; bukkit conflicts with the paper-api on classpath.
    resolutionStrategy.eachDependency {
        if (requested.version == "latest.integration") {
            when (requested.group) {
                "com.sk89q.worldedit" -> useVersion("7.2.17")
                "com.sk89q.worldguard" -> useVersion("7.0.9")
            }
        }
    }
    exclude(module = "bukkit")
    exclude(group = "de.keyle", module = "mypet")
}

// Optional protection plugins: compile-only soft-dependencies, never bundled.
dependencies {
    compileOnly(project(":dough-common"))

    val softDeps = listOf(
        "com.sk89q.worldedit:worldedit-core:7.2.17",
        "com.sk89q.worldedit:worldedit-bukkit:7.2.17",
        "com.sk89q.worldguard:worldguard-bukkit:7.0.9",
        "com.github.elBukkit:PreciousStones:1.17.2",
        "net.coreprotect:coreprotect:21.3",
        "de.diddiz:logblock:1.17.0.0-SNAPSHOT",
        "com.github.marcelo-mason:SimpleClans:7c3db52796",
        "com.github.GriefPrevention:GriefPrevention:16.18.2",
        "com.github.dmulloy2.LWC:lwc:master-SNAPSHOT",
        "me.lucko:helper:5.6.14",
        "com.massivecraft:Factions:1.6.9.5-4.1.4-STABLE",
        "com.github.LlmDl:Towny:1b86d017c5",
        "com.github.fubira:Lockette:9dac96e8f8",
        "com.intellectualsites.plotsquared:plotsquared-core:7.3.6",
        "com.intellectualsites.plotsquared:plotsquared-bukkit:7.3.6",
        "br.net.fabiozumbi12.RedProtect:RedProtect-Core:7.7.3",
        "br.net.fabiozumbi12.RedProtect:RedProtect-Spigot:7.7.3",
        "world.bentobox:bentobox:1.20.1-SNAPSHOT",
        "nl.rutgerkok:blocklocker:1.10.4",
        "com.github.angeschossen:LandsAPI:6.29.12",
        "com.github.angeschossen:ChestProtectAPI:3.9.1",
        "net.dzikoysk.funnyguilds:plugin:4.12.0",
        "net.william278.husktowns:husktowns-bukkit:3.0-988161b",
        "net.william278.huskclaims:huskclaims-bukkit:1.0.2-e60150d",
        "de.epiceric:ShopChest:1.13-SNAPSHOT",
        "org.popcraft:bolt-bukkit:1.0.580"
    )
    for (dep in softDeps) {
        compileOnly(dep)
    }
}
