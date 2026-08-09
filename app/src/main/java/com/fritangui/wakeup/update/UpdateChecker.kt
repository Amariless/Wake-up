package com.fritangui.wakeup.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.fritangui.wakeup.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Revisa el último release público de GitHub del repo de la app. No necesita
 * ningún backend propio: usa directamente la API pública de GitHub, y el tag
 * de cada release (`build-<numero>`) lleva el mismo número que se compiló
 * como `versionCode` de esa build (ver .github/workflows/release.yml), así
 * que comparar versiones es una simple comparación de enteros.
 */
@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(): UpdateCheckState = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext UpdateCheckState.Error("GitHub respondió $code (¿ya existe algún release?)")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(body).jsonObject

            val tagName = root["tag_name"]?.jsonPrimitive?.content
                ?: return@withContext UpdateCheckState.Error("Release sin tag_name")
            val remoteVersionCode = tagName.substringAfterLast('-').toIntOrNull()
                ?: return@withContext UpdateCheckState.Error("No se pudo leer la versión del tag \"$tagName\"")

            val apkAsset = root["assets"]?.jsonArray
                ?.map { it.jsonObject }
                ?.firstOrNull { asset -> asset["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true }
                ?: return@withContext UpdateCheckState.Error("El release más reciente no tiene un .apk adjunto")

            val downloadUrl = apkAsset["browser_download_url"]?.jsonPrimitive?.content
                ?: return@withContext UpdateCheckState.Error("Falta la URL de descarga del APK")

            if (remoteVersionCode <= currentVersionCode()) {
                return@withContext UpdateCheckState.UpToDate
            }

            UpdateCheckState.Available(
                UpdateInfo(
                    versionCode = remoteVersionCode,
                    releaseName = root["name"]?.jsonPrimitive?.content ?: tagName,
                    downloadUrl = downloadUrl,
                    fileSizeBytes = apkAsset["size"]?.jsonPrimitive?.longOrNull ?: 0L,
                ),
            )
        }.getOrElse { e ->
            UpdateCheckState.Error(e.message ?: "No se pudo conectar con GitHub")
        }
    }

    private fun currentVersionCode(): Int = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    } catch (_: PackageManager.NameNotFoundException) {
        Int.MAX_VALUE // si no se puede leer, mejor no ofrecer "actualizar" a lo mismo en bucle
    }
}
