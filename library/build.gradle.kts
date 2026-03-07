import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.grainql"
version = "0.0.1"

kotlin {
    jvm()

    androidLibrary {
        namespace = "com.grainql.sdk"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {}

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentnegotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.touchlab.kermit)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "grain-sdk", version.toString())

    pom {
        name = "Grain Kotlin SDK"
        description = "Kotlin Multiplatform SDK for Grain Analytics"
        inceptionYear = "2026"
        url = "https://grainql.com"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "threos"
                name = "Eray Ocak"
                url = "https://github.com/threos/"
            }
        }
        scm {
            url = "https://github.com/grainql/grain-kotlin-sdk"
            connection = "scm:git:git://github.com/grainql/grain-kotlin-sdk.git"
            developerConnection = "scm:git:ssh://git@github.com/grainql/grain-kotlin-sdk.git"
        }
    }
}
