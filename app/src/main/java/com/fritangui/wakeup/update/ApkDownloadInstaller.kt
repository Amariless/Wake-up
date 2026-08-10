package com.fritangui.wakeup.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val APK_FILE_NAME = "wakeup-update.apk"
private const val PREFS_NAME = "wakeup_apk_download"
private const val KEY_LAST_DOWNLOAD_ID = "last_download_id"

/**
 * Descarga el APK del release con [android.app.DownloadManager] (maneja la
 * descarga en segundo plano, reintentos y la notificación del sistema solo)
 * y, al terminar, arma el Intent para instalarlo vía FileProvider.
 */
@Singleton
class ApkDownloadInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val downloadManager get() = context.getSystemService<DownloadManager>()

    fun download(update: UpdateInfo): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))
        val manager = downloadManager ?: run {
            emit(DownloadState.Failed("No hay servicio de descargas en este dispositivo"))
            return@flow
        }

        // Antes de empezar: borra el archivo de la vez anterior y también su registro viejo en
        // DownloadManager (si no, aunque el archivo se reemplace, cada descarga deja su propia fila
        // en el content provider de DownloadManager — eso es lo que se iba "acumulando" con el
        // tiempo, no el archivo en sí, que siempre tuvo un nombre fijo y se borraba antes de cada
        // descarga nueva).
        outputFile().delete()
        removeStalePreviousDownload(manager)

        val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
            .setTitle("Wake up · ${update.releaseName}")
            .setDescription("Descargando actualización")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            // La app ya tiene su propia UI de progreso (UpdateScreen); no hace falta que además el
            // sistema muestre su propia notificación de descarga.
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setAllowedOverMetered(true)

        val downloadId = manager.enqueue(request)
        rememberDownloadId(downloadId)

        while (true) {
            delay(400)
            val (status, progress, reason) = queryStatus(manager, downloadId)
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    // OJO: NO llamar a manager.remove(downloadId) acá. DownloadManager.remove()
                    // no solo borra su registro interno — también borra el archivo en disco que
                    // acaba de descargar. Hacerlo justo aquí borraba el APK antes de que el
                    // instalador del sistema llegara a leerlo, y por eso Android mostraba "se
                    // produjo un error en el análisis del paquete" (el FileProvider apuntaba a un
                    // archivo que ya no existía). El registro viejo de DownloadManager se limpia
                    // solo, de forma segura, al empezar la SIGUIENTE descarga (ver
                    // removeStalePreviousDownload) — para entonces ya no hace falta el archivo.
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile())
                    emit(DownloadState.ReadyToInstall(uri))
                    return@flow
                }
                DownloadManager.STATUS_FAILED -> {
                    manager.remove(downloadId)
                    forgetDownloadId()
                    emit(DownloadState.Failed("Falló la descarga (código $reason)"))
                    return@flow
                }
                else -> emit(DownloadState.Downloading(progress))
            }
        }
    }

    /** Por si una descarga anterior quedó a medias (p.ej. la app se cerró antes de terminar). */
    private fun removeStalePreviousDownload(manager: DownloadManager) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val staleId = prefs.getLong(KEY_LAST_DOWNLOAD_ID, -1L)
        if (staleId != -1L) runCatching { manager.remove(staleId) }
        prefs.edit().remove(KEY_LAST_DOWNLOAD_ID).apply()
    }

    private fun rememberDownloadId(id: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_DOWNLOAD_ID, id).apply()
    }

    private fun forgetDownloadId() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_LAST_DOWNLOAD_ID).apply()
    }

    private fun queryStatus(manager: DownloadManager, downloadId: Long): Triple<Int, Int, Int> {
        var cursor: Cursor? = null
        try {
            cursor = manager.query(DownloadManager.Query().setFilterById(downloadId))
            if (cursor == null || !cursor.moveToFirst()) return Triple(DownloadManager.STATUS_RUNNING, 0, 0)
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
            return Triple(status, progress, reason)
        } finally {
            cursor?.close()
        }
    }

    fun installIntent(fileUri: Uri): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun outputFile(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME)
}
