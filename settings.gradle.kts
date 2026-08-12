rootProject.name = "s-reports"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }
    }
}

include(":s-reports-common")
include(":s-reports-paper")
include(":s-reports-velocity")

project(":s-reports-common").projectDir = file("s-reports-common")
project(":s-reports-paper").projectDir = file("s-reports-paper")
project(":s-reports-velocity").projectDir = file("s-reports-velocity")
