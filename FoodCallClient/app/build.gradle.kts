plugins {
    id("com.google.gms.google-services")
    id("com.android.application")
}

android {
    namespace = "com.cyberfox.foodcaller.client"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cyberfox.foodcaller.client"

        minSdk = 24
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "FOODCALL_API_BASE_URL", "\"\"")
    }

    buildFeatures { buildConfig = true }
}

dependencies {
    // 1.15 works with compileSdk 35; newer Core versions require API 36.
    implementation("androidx.core:core:1.15.0")
    implementation("com.google.firebase:firebase-messaging")
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("androidx.work:work-runtime:2.10.0")
}
