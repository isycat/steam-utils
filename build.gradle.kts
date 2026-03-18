plugins {
    kotlin("jvm")
    id("com.isycat.maven-central-publisher") version "1.0.0"
}

group = "com.isycat"
version = properties["version"] ?: "0.0.1"

mavenCentralPublishing {
    groupId = "com.isycat"
    artifactId = "steam-utils"
    name = "Steam Utils"
    description = "Platform-agnostic utilities for locating Steam installations"
    url = "https://github.com/isycat/steam-utils"
    developerId = "isycat"

    license {
        name = "MIT License"
        url = "https://opensource.org/licenses/MIT"
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
