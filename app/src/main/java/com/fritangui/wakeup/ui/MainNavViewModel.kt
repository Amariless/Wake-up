package com.fritangui.wakeup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import com.fritangui.wakeup.data.db.entity.FolderEntity
import com.fritangui.wakeup.data.repository.FolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Estado a nivel de la navegación raíz: hoy solo la carpeta "principal" (ver [Routes]/tab de Carpetas). */
@HiltViewModel
class MainNavViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    folderRepository: FolderRepository,
) : ViewModel() {

    val pinnedFolder: StateFlow<FolderEntity?> = settingsDataStore.pinnedFolderId
        .flatMapLatest { id -> if (id == null) flowOf(null) else folderRepository.observeById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
