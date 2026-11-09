plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.futsch1.medtimer.core.scan"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
    }
}

androidComponents {
    beforeVariants(selector().all()) {
        it.androidTest.enable = false
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit4)
    testImplementation(kotlin("test-junit"))
}
