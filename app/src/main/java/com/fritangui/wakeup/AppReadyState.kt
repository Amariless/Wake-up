package com.fritangui.wakeup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Si la app ya terminó su precarga inicial (hoy: abrir la conexión a la base de datos Room — ver
 * [WakeUpApp.warmUpDatabase]). [MainActivity] mantiene la pantalla de carga (splash) visible hasta
 * que esto se pone en `true`, para que la primera pestaña que se abra ya encuentre todo listo en
 * vez de sentirse lenta o parpadear mientras carga.
 */
object AppReadyState {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun markReady() {
        _isReady.value = true
    }
}
