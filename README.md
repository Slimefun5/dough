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
      <version></version>
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
      implementation 'io.github.intisy:dough:'
  }
```

## Usage in public projects

 * Gradle (inside the  or  file)
```groovy
  plugins {
      id "io.github.intisy.github-gradle" version "1.3.7"
  }
  dependencies {
      githubImplementation "intisy:dough:"
  }
```


Once you have it installed you can use it like so:

```
// Dough utilities are used across Slimefun: config, items, inventories, scheduling, reflection, ...
// Pull every module at once with the github-gradle "all" classifier (see the Modules section).
```

## License

[![Apache License 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
