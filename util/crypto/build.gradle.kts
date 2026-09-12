import com.artemchep.keyguard.buildplugins.testing.registerJvmBenchmark
import org.gradle.api.tasks.testing.Test

plugins {
    id("keyguard.quality")
    id("keyguard.kotlin-multiplatform-library")
    alias(libs.plugins.kotlin.plugin.serialization)
    id("keyguard.rust-multiplatform-library")
    id("keyguard.native-crypto-consumer")
}

keyguardRust {
    extraSourceInputs.from(
        layout.projectDirectory.dir("schema"),
        rootProject.layout.projectDirectory.dir("thirdParty/rust"),
    )
    androidCmakeToolchainFile.set(layout.projectDirectory.file("cmake/android.toolchain.cmake"))
    appleInterop(
        packageName = "com.artemchep.keyguard.nativecrypto.ffi",
        requireTargetMapping = true,
    )
}

kotlin {
    android {
        namespace = "com.artemchep.keyguard.nativecrypto"

        packaging {
            jniLibs.useLegacyPackaging = false
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.protobuf)
            }
        }

        val jvmCommonMain by creating {
            dependsOn(commonMain)
        }
        val androidMain by getting {
            dependsOn(jvmCommonMain)
        }
        val desktopMain by getting {
            dependsOn(jvmCommonMain)
            dependencies {
                implementation(libs.java.jna)
            }
        }

        val iosArm64Main by getting {
            dependsOn(commonMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(commonMain)
        }
        val macosArm64Main by getting {
            dependsOn(commonMain)
        }
    }
}

tasks.named<Test>("desktopTest") {
    filter {
        excludeTestsMatching("com.artemchep.keyguard.nativecrypto.benchmark.*")
    }
}

registerJvmBenchmark(
    name = "nativeCryptoLayerBenchmark",
    description = "Runs the layered Native Crypto JVM overhead benchmark suite from desktopTest.",
    testPattern = "com.artemchep.keyguard.nativecrypto.benchmark.*",
)
