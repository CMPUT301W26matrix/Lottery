plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.lottery"
    compileSdk = 36

    defaultConfig {
        manifestPlaceholders += mapOf("mapsApiKey" to "AIzaSyBQ7iqhDezLQYBFeZf6nwlaqifkHkrrvGA")
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.ext.junit)
    implementation(libs.espresso.core)
    implementation(libs.firebase.installations)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.0.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.2")

    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")

    // ZXing for QR Code generation
    implementation("com.google.zxing:core:3.5.3")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // Glide for image loading (Optional but useful for local URIs too)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
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
