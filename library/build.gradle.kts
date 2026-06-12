plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
    signing
}

group = "io.github.kvarun701"
version = "1.0.0"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "icorp"
            isStatic = true
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.activity:activity-compose:1.8.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "icorp.library"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("icorp")
            description.set("Compose Multiplatform Image Cropper")
            url.set("https://github.com/kvarun701/icorp-library-for-composemultiplateform")
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("kvarun701")
                    name.set("Varun")
                    email.set("varunpandit.net@gmail.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/kvarun701/icorp-library-for-composemultiplateform.git")
                developerConnection.set("scm:git:ssh://github.com/kvarun701/icorp-library-for-composemultiplateform.git")
                url.set("https://github.com/kvarun701/icorp-library-for-composemultiplateform")
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername")?.toString() ?: ""
                password = project.findProperty("ossrhPassword")?.toString() ?: ""
            }
        }
    }
}

signing {
    val signingKeyId = project.findProperty("signingKeyId")?.toString() ?: project.findProperty("signing.keyId")?.toString()
    val signingKey = (project.findProperty("signingKey")?.toString() ?: project.findProperty("signing.key")?.toString())?.replace("\\n", "\n")
    val signingPassword = project.findProperty("signingPassword")?.toString() ?: project.findProperty("signing.password")?.toString()

    if (!signingKey.isNullOrEmpty()) {
        useInMemoryPgpKeys(signingKeyId ?: "", signingKey, signingPassword ?: "")
        sign(publishing.publications)
    } else if (project.hasProperty("signing.secretKeyRingFile")) {
        sign(publishing.publications)
    }
}
