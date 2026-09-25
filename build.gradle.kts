// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    id("org.sonarqube") version "7.5.0.8588"
}

sonar {
    properties {
        property("sonar.projectKey", "Olius-Inter_Olius_Mobile")
        property("sonar.organization", "olius-inter")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            layout.projectDirectory.file("app/build/reports/coverage/test/debug/report.xml").asFile.absolutePath
        )
        // Código de UI (Compose), tema, navegação e a ponte com o Credential
        // Manager dependem de Activity/Context e não rodam em teste unitário.
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/MainActivity.kt",
                "**/presentation/theme/**",
                "**/presentation/navigation/**",
                "**/presentation/auth/GoogleIdentityClient.kt",
                "**/*Screen.kt",
                "**/presentation/screen/PerfilType/PerfilType.kt"
            ).joinToString(",")
        )
    }
}