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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Fixes potential conflicts with protobuf/guava
            pickFirsts += "google/protobuf/*.proto"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.mockito.android)
    // Use consistent versions for Espresso components from Version Catalog if possible, 
    // or use the same version as espresso-core (3.7.0)
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }

    // Force a specific version of protobuf-javalite to resolve the NoSuchMethodError
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")
    androidTestImplementation("com.google.protobuf:protobuf-javalite:3.25.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")

    // ZXing for QR Code generation
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // For UI Testing
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
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
