import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.android.application)
}

val selectedCountriesAssetsDirectory = layout.buildDirectory.dir("generated/selectedCountriesAssets").get().asFile
val copySelectedCountries = tasks.register<Copy>("copySelectedCountries") {
    from(rootProject.file(".opencode/SelectedCountries.json"))
    into(selectedCountriesAssetsDirectory)
}

android {
    sourceSets {
        getByName("main") {
            assets.directories.add(selectedCountriesAssetsDirectory.absolutePath)
        }
    }

    namespace = "com.example.desarrooapps"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.desarrooapps"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.named("preBuild").configure {
    dependsOn(copySelectedCountries)
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
