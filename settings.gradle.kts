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
        maven { url = uri("https://jitpack.io") }
    }
//    versionCatalogs {
//        create("versionlibs") {
//            from(files("gradle/libs.versions.toml"))
//        }
//    }
}

rootProject.name = "WellScreen"
include(":app")
