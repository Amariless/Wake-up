package com.fritangui.wakeup.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ALARM: pantalla completa + sonido en bucle + reto para apagar (el comportamiento de siempre).
 * REMINDER: una notificación normal con su propio sonido, sin pantalla completa ni reto — para
 * avisos que no necesitan "despertar" a nadie, solo recordar algo.
 */
enum class AlarmKind { ALARM, REMINDER }

/** Reto que el usuario debe resolver para poder apagar la alarma o el temporizador. */
enum class DismissChallengeType {
    NONE,
    SHAKE,
    MATH_PROBLEM,
    /** Conectar N puntos numerados en orden, en un solo trazo (como un patrón de desbloqueo). */
    DRAW_GESTURE,
    /** Seguir con el dedo una línea que se curva varias veces, sin salirse ni soltar. */
    TRACE_PATH,
    /** Escribir exactamente una palabra o frase corta generada al azar. */
    TYPE_PHRASE,
}

/**
 * Una alarma. [folderId] nulo significa que es una alarma del "reloj general"
 * (fuera de cualquier carpeta/semestre); si tiene folderId, se cancela
 * automáticamente cuando esa carpeta se marca como terminada.
 *
 * [repeatDaysBitmask] usa bit0=lunes … bit6=domingo; 0 = alarma de una sola vez
 * (para ese caso se usa [oneShotDateEpochDay], la fecha en días desde epoch).
 */
@Entity(
    tableName = "alarms",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("folderId")],
)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long?,
    val label: String,
    val hour: Int,
    val minute: Int,
    val repeatDaysBitmask: Int = 0,
    val oneShotDateEpochDay: Long? = null,
    val isEnabled: Boolean = true,
    val dismissChallenge: DismissChallengeType = DismissChallengeType.NONE,
    val challengeDifficulty: Int = 1,
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val preAlarmNotificationMinutesBefore: Int = 60,
    /** Se activa desde la notificación previa: "apagar solo esta vez". Se limpia sola tras usarse. */
    val skipNextOccurrence: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    /** Se actualiza cada vez que la alarma efectivamente suena; usado para ordenar la lista por uso reciente. */
    val lastTriggeredAtEpochMillis: Long? = null,
    val kind: AlarmKind = AlarmKind.ALARM,
    /** Si está activo, se borra sola (y se cancela cualquier programación futura) justo después de sonar. */
    val deleteAfterRing: Boolean = false,
) {
    companion object {
        fun dayBit(isoDayOfWeek: Int): Int = 1 shl (isoDayOfWeek - 1)
    }
}
