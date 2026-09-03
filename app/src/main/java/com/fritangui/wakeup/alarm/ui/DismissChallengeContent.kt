package com.fritangui.wakeup.alarm.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.accessibility.AccessibilityManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.core.content.getSystemService
import com.fritangui.wakeup.alarm.ui.challenge.DrawPatternChallenge
import com.fritangui.wakeup.alarm.ui.challenge.MathChallenge
import com.fritangui.wakeup.alarm.ui.challenge.ShakeChallenge
import com.fritangui.wakeup.alarm.ui.challenge.TraceCurvyPathChallenge
import com.fritangui.wakeup.alarm.ui.challenge.TypePhraseChallenge
import com.fritangui.wakeup.data.db.entity.DismissChallengeType

/**
 * Despacha al composable del reto correspondiente. Compartido entre la pantalla
 * de alarma sonando y la de temporizador terminado, para no duplicar la lógica
 * de cada reto en dos sitios.
 */
@Composable
fun DismissChallengeContent(type: DismissChallengeType, difficulty: Int, onCompleted: () -> Unit) {
    when (type) {
        DismissChallengeType.SHAKE -> ShakeChallenge(difficulty, onCompleted)
        DismissChallengeType.MATH_PROBLEM -> MathChallenge(difficulty, onCompleted)
        DismissChallengeType.DRAW_GESTURE -> DrawPatternChallenge(difficulty, onCompleted)
        DismissChallengeType.TRACE_PATH -> TraceCurvyPathChallenge(difficulty, onCompleted)
        DismissChallengeType.TYPE_PHRASE -> TypePhraseChallenge(difficulty, onCompleted)
        DismissChallengeType.NONE -> Text(
            "Toca \"Apagar\" para detener",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/**
 * Los retos SHAKE/DRAW_GESTURE/TRACE_PATH dependen 100% de un gesto de precisión o de un sensor
 * físico, sin ninguna alternativa accesible: sin esto, un usuario con TalkBack activo (o con
 * movilidad reducida en las manos) o un dispositivo sin acelerómetro se quedaba con la alarma o el
 * temporizador sonando indefinidamente, sin forma de completarlos. Acá se degradan a NONE (el botón
 * "Apagar" normal) en esos casos — igual de válido que forzar al usuario a demostrar que despertó,
 * ya que en ninguno de los dos casos podría completar el reto de todas formas.
 *
 * Llamar SIEMPRE con el mismo resultado tanto para elegir qué dibuja [DismissChallengeContent] como
 * para decidir si la pantalla muestra el botón "Apagar" — si no, quedan desincronizados.
 */
fun effectiveDismissChallengeType(context: Context, type: DismissChallengeType): DismissChallengeType {
    val isGestureOrSensorBased = type == DismissChallengeType.SHAKE ||
        type == DismissChallengeType.DRAW_GESTURE ||
        type == DismissChallengeType.TRACE_PATH
    if (!isGestureOrSensorBased) return type

    val touchExplorationOn = context.getSystemService<AccessibilityManager>()?.isTouchExplorationEnabled == true
    if (touchExplorationOn) return DismissChallengeType.NONE

    if (type == DismissChallengeType.SHAKE) {
        val hasAccelerometer = context.getSystemService<SensorManager>()?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        if (!hasAccelerometer) return DismissChallengeType.NONE
    }
    return type
}
