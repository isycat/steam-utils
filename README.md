# Steam Utils

Platform-agnostic utilities for locating Steam installations.

## Features

- Automatically detect Steam installation directory on Windows, ~~macOS~~ (soon), and ~~Linux~~ (soon)
- Locate Steam library folders
- Find game installation paths
- Pure Kotlin implementation with no platform-specific dependencies

## Installation

### Gradle

Add JitPack repository to your `build.gradle.kts`:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.isycat:steam-utils:v1.0.0")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.isycat</groupId>
        <artifactId>steam-utils</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

## Usage

```kotlin
import com.isycat.dotahalp.steamutils.*

// Find Steam installation directory
val steamDir = SteamInstallLocator.findSteamDirectory()
println("Steam installed at: $steamDir")

// Find a specific game
val dotaDir = SteamInstallLocator.findGameDirectory(570) // Dota 2 App ID
println("Dota 2 installed at: $dotaDir")
```

## Building

```bash
./gradlew build
./gradlew test
```

## Requirements

- JDK 21+
- Kotlin 2.1.21+
- Gradle 8.5+

## License

MIT License - see LICENSE file for details

## Contributing

This module is part of the [DotaHALP](https://github.com/isycat/dota-halp) project but is maintained as a standalone library for reusability.

Contributions are welcome! Please open an issue or pull request.
