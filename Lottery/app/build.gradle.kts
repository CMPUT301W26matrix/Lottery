plugins {
    alias(libs.plugins.android.application)

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.lottery"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.lottery"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.ext.junit)
    implementation(libs.espresso.core)
    implementation(libs.firebase.installations)
    testImplementation(libs.junit)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.0.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")

}

tasks.register<Javadoc>("generateJavadoc") {
    dependsOn("compileReleaseJavaWithJavac")
    source(android.sourceSets["main"].java.directories)
    doFirst {
        val javaCompile = tasks.named("compileReleaseJavaWithJavac", JavaCompile::class).get()
        classpath = javaCompile.classpath
    }
    destinationDir = file("${rootProject.rootDir}/javadocs")
    isFailOnError = false
}