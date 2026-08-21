package com.fritangui.wakeup.ui.navigation

/**
 * Puente entre el editor que esté abierto (materia/tarea/alarma/horario) y la barra de navegación
 * inferior: antes, tocar otra pestaña mientras se editaba algo con cambios sin guardar los
 * descartaba sin avisar — solo el botón de atrás propio de cada editor mostraba una confirmación.
 * Cada editor se registra acá mientras está en pantalla; la barra de abajo pasa CUALQUIER
 * navegación por [navigateOrConfirm] antes de ejecutarla, y si hay algo pendiente, deja que el
 * editor muestre su propio diálogo (le pasa la navegación real para que la dispare si el usuario
 * confirma salir sin guardar) en vez de navegar de una.
 *
 * Objeto simple (no ViewModel/Hilt) a propósito: es puro estado efímero en memoria del proceso,
 * sin nada que persistir, y necesita ser alcanzable tanto desde cualquier editor como desde
 * [com.fritangui.wakeup.ui.WakeUpNavHost] sin acoplarlos entre sí.
 */
object UnsavedChangesGuard {
    private var isDirty = false
    private var onRequestLeave: ((action: () -> Unit) -> Unit)? = null

    /** Llamado por el editor activo en cada recomposición relevante (ver [register] de conveniencia en cada pantalla). */
    fun register(isDirty: Boolean, onRequestLeave: (action: () -> Unit) -> Unit) {
        this.isDirty = isDirty
        this.onRequestLeave = onRequestLeave
    }

    /** El editor debe llamar esto al salir de composición (DisposableEffect), para no dejar estado fantasma. */
    fun clear() {
        isDirty = false
        onRequestLeave = null
    }

    /** Ejecuta [action] (típicamente una navegación) directo si no hay cambios sin guardar; si los hay, deja que el editor confirme primero. */
    fun navigateOrConfirm(action: () -> Unit) {
        if (isDirty) {
            onRequestLeave?.invoke(action) ?: action()
        } else {
            action()
        }
    }
}
