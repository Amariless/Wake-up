package com.fritangui.wakeup.ui.folders

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import com.fritangui.wakeup.data.db.entity.FolderEntity
import com.fritangui.wakeup.data.repository.FolderRepository
import com.fritangui.wakeup.ui.theme.FolderColorPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val alarmController: AlarmController,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val pinnedFolderId: StateFlow<Long?> = settingsDataStore.pinnedFolderId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // La carpeta "principal" (pineada) siempre primero, sin importar su fecha de creación: si es
    // vieja, el orden normal (más reciente primero) la puede enterrar debajo de carpetas nuevas y
    // dar la sensación de que "desapareció" al volver a esta lista desde su propio detalle.
    val folders: StateFlow<List<FolderEntity>> = combine(
        folderRepository.observeAll(),
        settingsDataStore.pinnedFolderId,
    ) { all, pinnedId ->
        if (pinnedId == null) {
            all
        } else {
            val pinned = all.filter { it.id == pinnedId }
            val rest = all.filterNot { it.id == pinnedId }
            pinned + rest
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
