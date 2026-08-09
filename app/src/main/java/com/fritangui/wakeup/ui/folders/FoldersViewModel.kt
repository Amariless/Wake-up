package com.fritangui.wakeup.ui.folders

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.data.db.entity.FolderEntity
import com.fritangui.wakeup.data.repository.FolderRepository
import com.fritangui.wakeup.ui.theme.FolderColorPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val alarmController: AlarmController,
) : ViewModel() {

    val folders: StateFlow<List<FolderEntity>> = folderRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createFolder(name: String, colorArgb: Int = FolderColorPalette.random().toArgb()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            folderRepository.create(name.trim(), colorArgb)
        }
    }

    fun renameFolder(folder: FolderEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { folderRepository.rename(folder, newName.trim(), folder.colorArgb) }
    }

    /** "Terminar" una carpeta: apaga todas sus alarmas y recordatorios, la deja en solo lectura. */
    fun terminateFolder(folderId: Long) {
        viewModelScope.launch { alarmController.terminateFolder(folderId) }
    }

    fun reactivateFolder(folderId: Long) {
        viewModelScope.launch { alarmController.reactivateFolder(folderId) }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch { folderRepository.delete(folder) }
    }
}
