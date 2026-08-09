package com.fritangui.wakeup.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.update.ApkDownloadInstaller
import com.fritangui.wakeup.update.DownloadState
import com.fritangui.wakeup.update.UpdateCheckState
import com.fritangui.wakeup.update.UpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateChecker: UpdateChecker,
    private val apkDownloadInstaller: ApkDownloadInstaller,
) : ViewModel() {

    private val _checkState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val checkState: StateFlow<UpdateCheckState> = _checkState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun checkForUpdate() {
        viewModelScope.launch {
            _checkState.value = UpdateCheckState.Checking
            _checkState.value = updateChecker.checkForUpdate()
        }
    }

    fun startDownload() {
        val available = _checkState.value as? UpdateCheckState.Available ?: return
        viewModelScope.launch {
            apkDownloadInstaller.download(available.info).collect { state ->
                _downloadState.value = state
            }
        }
    }

    fun installIntent() = (downloadState.value as? DownloadState.ReadyToInstall)?.let { apkDownloadInstaller.installIntent(it.fileUri) }

    fun resetDownload() {
        _downloadState.value = DownloadState.Idle
    }
}
