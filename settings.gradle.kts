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
        maven { url = uri("https://appwrite.io/repo/maven-central") }
        maven { url = uri("https://developer.android.com/custom-builds/mlkit-repo/maven") }
    }
}

rootProject.name = "mentor"
include(":app")
