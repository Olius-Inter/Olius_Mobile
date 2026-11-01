import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Sem nenhuma lib do Firebase: o plugin fica só para gerar o
    // R.string.default_web_client_id (login com Google) a partir do google-services.json.
    alias(libs.plugins.google.gms.google.services)
    // Gera o código do Room (cache do perfil em SQLite).
    alias(libs.plugins.ksp)
}

// URLs das APIs vindas do local.properties (não versionado). Sem a chave, usa o
// servidor no PC visto do emulador (10.0.2.2). Nenhuma chave de API vai no app.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun apiUrl(key: String, default: String): String =
    localProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() } ?: default

android {
    namespace = "com.olius.app"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.olius.app"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Core API (porta 8080) e API Redis de ranking (8081). O chat também vai
        // pela Core (POST /api/v1/chat): o app não chama a IA direto.
        buildConfigField("String", "OLIUS_CORE_BASE_URL", "\"${apiUrl("OLIUS_CORE_BASE_URL", "http://10.0.2.2:8080/")}\"")
        buildConfigField("String", "OLIUS_RANKING_BASE_URL", "\"${apiUrl("OLIUS_RANKING_BASE_URL", "http://10.0.2.2:8081/")}\"")
    }

    buildTypes {
        debug {
            // Gera o relatório JaCoCo dos testes unitários
            // (task createDebugUnitTestCoverageReport), lido pelo Sonar.
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // Gera o test_config.properties com a pasta de assets mesclados: os
        // testes leem dali os metadados da libphonenumber (ver TestPhoneNumbers).
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.googleid)
    // SQLite (Room): cache do perfil da Core (GET /users/me) — ver MOBILE_EXPLICACAO_AUTH.md.
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    // Gera o QR Code de identificação a partir do token (user_qr_code). Só o
    // encoder, sem câmera — ver API_07.
    implementation(libs.zxing.core)
    // Mapa real com OpenStreetMap: sem API key nem cartão (ver MAPA_PENDENCIAS.md).
    implementation(libs.osmdroid.android)
    // DDI, máscara e validação do telefone por país (offline, sem chave) — ver MOBILE_00 seção 3.6.
    implementation(libs.libphonenumber.android)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // implementação da extensão do lottie
    implementation("com.airbnb.android:lottie-compose:6.4.1")

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    // org.json de verdade nos testes: o android.jar só tem stubs que lançam exceção.
    testImplementation(libs.org.json)
}