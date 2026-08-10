plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/** Cantidad de commits alcanzables desde HEAD; usado como versionCode/versionName por defecto. */
fun gitCommitCount(): Int = try {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    process.waitFor()
    process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
    1
}

android {
    namespace = "com.fritangui.wakeup"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fritangui.wakeup"
        minSdk = 26
        targetSdk = 35
        // Por defecto el versionCode es el número de commits del repo (git rev-list --count
        // HEAD): así una build hecha a mano acá para entregar directo, y la build de CI del
        // mismo commit, calculan siempre el mismo número sin depender de que alguien recuerde
        // pasar -PappVersionCode. Antes había un valor fijo (2) de respaldo, que dejaba
        // cualquier build local pegada en esa versión para siempre y hacía que el updater
        // in-app pensara que ya estaba al día aunque hubiera releases nuevos en GitHub.
        // CI también lo pasa explícito (ver .github/workflows/release.yml) para que el tag del
        // release use el mismo número (comparado en update/UpdateChecker.kt).
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: gitCommitCount()
        versionName = (project.findProperty("appVersionName") as String?) ?: "1.1.${gitCommitCount()}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "GITHUB_REPO", "\"Amariless/Wake-up\"")
    }

    // Firma fija y compartida entre máquinas (en vez del debug.keystore que Android
    // Studio genera distinto por PC): así una APK compilada por CI y otra compilada en
    // tu PC se pueden instalar una encima de la otra como "actualización" sin perder los
    // datos guardados (Room/DataStore). No es para publicar en Play Store, solo para que
    // el flujo de sideload + auto-actualización de esta app sea consistente.
    signingConfigs {
        create("sideload") {
            storeFile = file("../keystore/wakeup-sideload.jks")
            storePassword = "wakeupsideload"
            keyAlias = "wakeup"
            keyPassword = "wakeupsideload"
        }
    }

    buildTypes {
        debug {
            // isDebuggable = false a propósito: esta build es la que instalas por sideload, no algo
            // que corras conectado a un debugger de Android Studio, así que no hace falta que ART
            // la trate como debuggeable — eso trae overhead real en tiempo de ejecución (impide
            // ciertas optimizaciones del runtime, deja hooks del debugger activos) que se sentía
            // como lentitud general. No confundir con el buildType "release": mantiene el mismo
            // applicationId (.debug) y firma que ya tenías instalada, así que sigue actualizando
            // en el mismo lugar sin perder datos — solo cambia esta bandera.
            isDebuggable = false
            // El sufijo del applicationId (".debug") se queda: es lo que hace que esta build se
            // instale siempre en el mismo lugar que la anterior sin perder datos — cambiarlo
            // ahora movería la app a un applicationId distinto, que Android trata como una app
            // aparte (instalación nueva, sin tus datos actuales). El sufijo del NOMBRE de versión
            // ("-debug") en cambio era solo texto — ya no tiene sentido mostrarlo ahora que esta
            // build no es debuggeable, así que se quita.
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("sideload")
            buildConfigField("boolean", "DEV_TOOLS_ENABLED", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("sideload")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "DEV_TOOLS_ENABLED", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// El nombre del archivo de salida por defecto de AGP es "app-<buildType>.apk" — como el buildType
// que de verdad usamos se sigue llamando "debug" (ver arriba, ya no es debuggeable pero el nombre
// del build type quedó igual), el .apk terminaba llamándose "app-debug.apk" aunque ya no tuviera
// nada de "debug". Se renombra acá al nombre real de la app en vez de andar renombrando el archivo
// a mano cada vez que se comparte.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            (output as com.android.build.api.variant.impl.VariantOutputImpl).outputFileName.set("wake-up.apk")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.service)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler.androidx)

    implementation(libs.work.runtime.ktx)
    implementation(libs.datastore.preferences)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
