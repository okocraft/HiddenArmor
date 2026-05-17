plugins {
    `java-library`
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.jcommon)
}

group = "me.kteq"
version = "1.1.0"
val mcVersion = libs.versions.paper.get().replaceAfter(".build", "").removeSuffix(".build")
val fullVersion = "${version}-mc${mcVersion}"

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.get())
}

jcommon {
    javaVersion = JavaVersion.VERSION_25
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml")) {
            expand("projectVersion" to fullVersion)
        }
    }

    jar {
        archiveFileName = "HiddenArmor-${fullVersion}.jar"
    }

    runServer {
        minecraftVersion(mcVersion)
        systemProperty("com.mojang.eula.agree", "true")
        systemProperty("paper.disablePluginRemapping", "true")
    }
}
