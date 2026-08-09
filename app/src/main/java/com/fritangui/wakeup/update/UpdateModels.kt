package com.fritangui.wakeup.update

import android.net.Uri

data class UpdateInfo(
    val versionCode: Int,
    val releaseName: String,
    val downloadUrl: String,
    val fileSizeBytes: Long,
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Available(val info: UpdateInfo) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progressPercent: Int) : DownloadState
    data class ReadyToInstall(val fileUri: Uri) : DownloadState
    data class Failed(val message: String) : DownloadState
}
