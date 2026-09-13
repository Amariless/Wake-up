package com.fritangui.wakeup.ui.clock.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.alarm.sound.AlarmSoundPreviewPlayer
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.FolderEntity
import com.fritangui.wakeup.data.repository.AlarmRepository
import com.fritangui.wakeup.data.repository.FolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Qué lista de alarmas se está mirando en la pestaña "Alarmas" de Reloj (#161): las del reloj
 *  general, o las de la carpeta marcada como principal. */
enum class AlarmsScope { GENERAL, ACTIVE_FOLDER }

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmController: AlarmController,
    private val folderRepository: FolderRepository,
    settingsDataStore: SettingsDataStore,
    val previewPlayer: AlarmSoundPreviewPlayer,
) : ViewModel() {

    private val generalAlarms: StateFlow<List<AlarmEntity>> = alarmRepository.observeGeneralAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** La carpeta marcada como principal ("fijada"), o null si ninguna lo está. */
    val activeFolder: StateFlow<FolderEntity?> = settingsDataStore.pinnedFolderId
        .flatMapLatest { id -> if (id == null) flowOf(null) else folderRepository.observeById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val activeFolderAlarms: StateFlow<List<AlarmEntity>> = activeFolder
        .flatMapLatest { folder -> if (folder == null) flowOf(emptyList()) else alarmRepository.observeByFolder(folder.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _scope = MutableStateFlow(AlarmsScope.GENERAL)
    val scope: StateFlow<AlarmsScope> = _scope
    fun setScope(newScope: AlarmsScope) { _scope.value = newScope }

    val alarms: StateFlow<List<AlarmEntity>> = combine(_scope, generalAlarms, activeFolderAlarms) { scope, general, folder ->
        if (scope == AlarmsScope.GENERAL) general else folder
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Generales + de la carpeta activa juntas, sin importar cuál esté eligiendo ver el toggle de
     *  arriba — para calcular cuándo suena la PRÓXIMA alarma entre cualquiera de las dos (#161):
     *  antes ese cálculo solo miraba las generales, así que una alarma de la carpeta principal que
     *  fuera a sonar antes no se reflejaba en el "Próxima alarma en..." de arriba de Reloj. */
    val allConsideredAlarms: StateFlow<List<AlarmEntity>> = combine(generalAlarms, activeFolderAlarms) { general, folder -> general + folder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch { alarmController.setEnabledAndReschedule(alarmId, enabled) }
    }

    override fun onCleared() {
        previewPlayer.stop()
        super.onCleared()
    }
}
