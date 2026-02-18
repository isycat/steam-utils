plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "com.isycat.dotahalp"
version = "1.0.0"

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.isycat"
            artifactId = "steam-utils"
            version = "1.0.0"
            
            from(components["java"])
            
            pom {
                name.set("Steam Utils")
                description.set("Platform-agnostic utilities for locating Steam installations")
                url.set("https://github.com/isycat/steam-utils")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("isycat")
                        name.set("isycat")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/isycat/steam-utils.git")
                    developerConnection.set("scm:git:ssh://github.com/isycat/steam-utils.git")
                    url.set("https://github.com/isycat/steam-utils")
                }
            }
        }
    }
}
