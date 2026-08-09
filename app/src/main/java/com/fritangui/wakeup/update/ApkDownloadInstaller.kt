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

        outputFile().delete()
        val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
            .setTitle("Wake up · ${update.releaseName}")
            .setDescription("Descargando actualización")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)

        val downloadId = manager.enqueue(request)

        while (true) {
            delay(400)
            val (status, progress, reason) = queryStatus(manager, downloadId)
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile())
                    emit(DownloadState.ReadyToInstall(uri))
                    return@flow
                }
                DownloadManager.STATUS_FAILED -> {
                    emit(DownloadState.Failed("Falló la descarga (código $reason)"))
                    return@flow
                }
                else -> emit(DownloadState.Downloading(progress))
            }
        }
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
