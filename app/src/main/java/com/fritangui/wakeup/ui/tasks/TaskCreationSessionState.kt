package com.fritangui.wakeup.ui.tasks

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado efímero (solo en memoria, se pierde al cerrar la app) compartido entre todas las
 * pantallas de crear/editar tarea de una misma carpeta:
 * - Qué materia se usó por última vez, para preseleccionarla en la siguiente tarea nueva de esa
 *   MISMA carpeta (si cambias de carpeta, ya no aplica — ver [lastSubjectFor]).
 * - Si el usuario ya dijo "no preguntar de nuevo" al guardar una tarea sin materia.
 */
@Singleton
class TaskCreationSessionState @Inject constructor() {
    private var lastFolderId: Long? = null
    private var lastSubjectId: Long? = null

    var skipNoSubjectConfirmation: Boolean = false
        private set

    fun lastSubjectFor(folderId: Long): Long? = if (lastFolderId == folderId) lastSubjectId else null

    fun remember(folderId: Long, subjectId: Long?) {
        lastFolderId = folderId
        lastSubjectId = subjectId
    }

    fun dontAskAgainThisSession() {
        skipNoSubjectConfirmation = true
    }
}
