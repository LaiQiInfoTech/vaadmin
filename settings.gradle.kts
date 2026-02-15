rootProject.name = "vaadmin"

pluginManagement {
    repositories {
        maven { url = uri("https://maven.vaadin.com/vaadin-prereleases") }
        gradlePluginPortal()
    }
    plugins {
        id("com.vaadin") version "24.9.10"
    }
}
