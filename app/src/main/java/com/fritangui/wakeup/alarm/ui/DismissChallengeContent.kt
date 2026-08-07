package com.fritangui.wakeup.alarm.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
