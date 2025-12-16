pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ДОБАВЬТЕ ЭТУ СТРОКУ:
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "FinanceManager"
include(":app")