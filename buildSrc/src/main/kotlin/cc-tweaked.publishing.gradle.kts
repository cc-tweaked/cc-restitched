import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            artifactId = base.archivesName.get()
            from(components["java"])

            pom {
                name.set("CC: Tweaked")
                description.set("CC: Tweaked is a fork of ComputerCraft, adding programmable computers, turtles and more to Minecraft.")
                url.set("https://github.com/cc-tweaked/CC-Tweaked")

                scm {
                    url.set("https://github.com/cc-tweaked/CC-Tweaked.git")
                }

                issueManagement {
                    system.set("github")
                    url.set("https://github.com/cc-tweaked/CC-Tweaked/issues")
                }

                licenses {
                    license {
                        name.set("ComputerCraft Public License, Version 1.0")
                        url.set("https://github.com/cc-tweaked/CC-Tweaked/blob/HEAD/LICENSE")
                    }
                }
            }
        }
    }
}
