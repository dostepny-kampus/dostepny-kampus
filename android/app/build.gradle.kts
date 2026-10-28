plugins {
    id("dostepnykampus.android.application")
    id("dostepnykampus.android.compose")
}

dependencies {
    implementation(project(":feature:auth"))
    implementation(project(":feature:map"))
    implementation(project(":feature:capture"))
    implementation(project(":feature:review"))
    implementation(project(":feature:progress"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    constraints {
        implementation(libs.androidx.lifecycle.runtime)
    }
}
