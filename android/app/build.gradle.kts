plugins {
    id("dostepnykampus.android.application")
    id("dostepnykampus.android.compose")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    constraints {
        implementation(libs.androidx.lifecycle.runtime)
    }
}
