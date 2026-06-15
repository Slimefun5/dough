# Dough

Archives containing JAR files are available as [releases](https://github.com/Slimefun5/dough/releases).

## What is dough?

Dough is Baked Libs' core utility library, forked and lowered to Java 8 for the Slimefun multi-version universal jar. It is published as separate modules (see below).

## Usage in private projects

 * Maven (inside the  file)
```xml
  <repository>
      <id>github</id>
      <url>https://maven.pkg.github.com/Slimefun5/dough</url>
      <snapshots><enabled>true</enabled></snapshots>
  </repository>
  <dependency>
      <groupId>io.github.intisy</groupId>
      <artifactId>dough</artifactId>
      <version>4.0.6</version>
  </dependency>
```

 * Maven (inside the  file)
```xml
  <servers>
      <server>
          <id>github</id>
          <username>your-username</username>
          <password>your-access-token</password>
      </server>
  </servers>
```

 * Gradle (inside the  or  file)
```groovy
  repositories {
      maven {
          url "https://maven.pkg.github.com/Slimefun5/dough"
          credentials {
              username = "<your-username>"
              password = "<your-access-token>"
          }
      }
  }
  dependencies {
      implementation 'io.github.intisy:dough:4.0.6'
  }
```

## Usage in public projects

 * Gradle (inside the  or  file)
```groovy
  plugins {
      id "io.github.intisy.github-gradle" version "1.3.7"
  }
  dependencies {
      githubImplementation "intisy:dough:4.0.6"
  }
```

## Modules

`dough` is published as separate modules. Pull every module at once with the `all` classifier:

```groovy
dependencies {
    githubImplementation "Slimefun5:dough:4.0.6:all"
}
```

Or depend on individual modules:

```groovy
dependencies {
    githubImplementation "Slimefun5:dough:4.0.6:chat"
    githubImplementation "Slimefun5:dough:4.0.6:common"
    githubImplementation "Slimefun5:dough:4.0.6:config"
    githubImplementation "Slimefun5:dough:4.0.6:data"
    githubImplementation "Slimefun5:dough:4.0.6:inventories"
    githubImplementation "Slimefun5:dough:4.0.6:items"
    githubImplementation "Slimefun5:dough:4.0.6:protection"
    githubImplementation "Slimefun5:dough:4.0.6:recipes"
    githubImplementation "Slimefun5:dough:4.0.6:reflection"
    githubImplementation "Slimefun5:dough:4.0.6:scheduling"
    githubImplementation "Slimefun5:dough:4.0.6:updater"
}
```

Once you have it installed you can use it like so:

```
// Dough utilities are used across Slimefun: config, items, inventories, scheduling, reflection, ...
// Pull every module at once with the github-gradle "all" classifier (see the Modules section).
```

## License

[![Apache License 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
