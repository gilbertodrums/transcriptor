pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Vosk offline ASR — descarga de modelos de IA local (sin datos de usuario)
        maven { url = uri("https://alphacephei.com/maven/") }
    }
}
rootProject.name = "Transcriptor"
include(":app")
