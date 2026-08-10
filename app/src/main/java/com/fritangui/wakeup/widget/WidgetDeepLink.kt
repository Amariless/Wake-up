package com.fritangui.wakeup.widget

import android.content.Context
import android.content.Intent
import com.fritangui.wakeup.MainActivity

/**
 * Adónde debe navegar la app al abrirse desde un toque en uno de los widgets de home screen, en
 * vez de solo abrir la pantalla de Inicio genérica. Se codifica como extras planos de [Intent]
 * (no un objeto serializado) porque cruza el límite proceso-widget/actividad.
 */
sealed class WidgetDeepLink {
    data class Subject(val folderId: Long, val subjectId: Long) : WidgetDeepLink()
    data class Task(val folderId: Long, val taskId: Long) : WidgetDeepLink()
    data object ScreenTime : WidgetDeepLink()

    companion object {
        private const val EXTRA_TYPE = "widget_deeplink_type"
        private const val EXTRA_FOLDER_ID = "widget_deeplink_folder_id"
        private const val EXTRA_ITEM_ID = "widget_deeplink_item_id"
        private const val TYPE_SUBJECT = "subject"
        private const val TYPE_TASK = "task"
        private const val TYPE_SCREEN_TIME = "screen_time"

        fun subjectIntent(context: Context, folderId: Long, subjectId: Long): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_TYPE, TYPE_SUBJECT)
                putExtra(EXTRA_FOLDER_ID, folderId)
                putExtra(EXTRA_ITEM_ID, subjectId)
            }

        fun taskIntent(context: Context, folderId: Long, taskId: Long): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_TYPE, TYPE_TASK)
                putExtra(EXTRA_FOLDER_ID, folderId)
                putExtra(EXTRA_ITEM_ID, taskId)
            }

        fun screenTimeIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_TYPE, TYPE_SCREEN_TIME)
            }

        fun from(intent: Intent?): WidgetDeepLink? {
            val type = intent?.getStringExtra(EXTRA_TYPE) ?: return null
            if (type == TYPE_SCREEN_TIME) return ScreenTime
            val folderId = intent.getLongExtra(EXTRA_FOLDER_ID, -1L)
            val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
            if (folderId < 0 || itemId < 0) return null
            return when (type) {
                TYPE_SUBJECT -> Subject(folderId, itemId)
                TYPE_TASK -> Task(folderId, itemId)
                else -> null
            }
        }
    }
}
