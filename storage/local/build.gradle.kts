plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:capabilities"))
    api(project(":storage:api"))
    testImplementation(libs.junit)
}
