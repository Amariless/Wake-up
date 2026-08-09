package com.fritangui.wakeup.ui.navigation

/** Rutas del grafo de navegación único de la app (patrón single-activity). */
object Routes {
    const val HOME = "home"
    const val FOLDERS = "folders"
    const val FOLDER_DETAIL = "folder/{folderId}"
    const val SUBJECT_EDITOR = "subject/{folderId}/{subjectId}"
    const val TASK_EDITOR = "task/{folderId}/{taskId}"
    const val CLOCK = "clock"
    const val ALARM_EDITOR = "alarm/{folderId}/{alarmId}"
    const val SCREEN_TIME = "screen_time"
    const val BLOCKING = "blocking"
    const val XIAOMI_ONBOARDING = "xiaomi_onboarding"
    const val SETTINGS = "settings"
    const val DEV_TOOLS = "dev_tools"
    const val UPDATE = "update"

    fun folderDetail(folderId: Long) = "folder/$folderId"

    /** subjectId = 0 significa "crear nueva materia". */
    fun subjectEditor(folderId: Long, subjectId: Long = 0L) = "subject/$folderId/$subjectId"

    /** taskId = 0 significa "crear nueva tarea". folderId puede ser 0 si se crea desde la vista global. */
    fun taskEditor(folderId: Long, taskId: Long = 0L) = "task/$folderId/$taskId"

    /** folderId = 0 significa "alarma general" (fuera de cualquier carpeta). alarmId = 0 = nueva alarma. */
    fun alarmEditor(folderId: Long = 0L, alarmId: Long = 0L) = "alarm/$folderId/$alarmId"
}
