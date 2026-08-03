# Build Configuration Notes

## Java Version Strategy

This project uses a two-tier Java version configuration:

- **Gradle Toolchain:** Java 25
- **Source/Target Compatibility:** Java 21

### Rationale

The Gradle toolchain is set to Java 25 to take advantage of the latest JDK improvements for build performance, garbage collection, and tooling. Gradle itself runs on Java 25.

However, the compiled bytecode targets Java 21 to maintain compatibility with the widest range of server environments. Paper 1.21.x servers run on Java 21, and setting `sourceCompatibility` and `targetCompatibility` to 21 ensures the plugin JAR can be loaded on any server running Java 21 or later.

This is a common pattern in Minecraft plugin development: build with the latest JDK for tooling benefits, but compile to the minimum server-supported bytecode version.

### Developer Setup

Set `JAVA_HOME` to a JDK 25 installation for local builds:

```bash
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"

# Linux/macOS
export JAVA_HOME=/path/to/jdk-25
```

The Gradle wrapper will download Gradle 9.5.1 automatically on first run.
