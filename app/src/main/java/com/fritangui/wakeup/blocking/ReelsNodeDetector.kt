package com.fritangui.wakeup.blocking

import android.view.accessibility.AccessibilityNodeInfo
import com.fritangui.wakeup.data.db.entity.BlockSurface

/**
 * Heurística de detección de "estoy viendo Reels/un feed de scroll infinito"
 * basada en resource-id de los nodos de accesibilidad. Es inherentemente
 * frágil: Instagram/TikTok cambian estos ids entre versiones sin avisar. Por
 * eso las listas están centralizadas aquí (fáciles de actualizar) y hay una
 * pantalla de depuración ([com.fritangui.wakeup.blocking.NodeInspectorScreen])
 * para volcar los ids reales del dispositivo del usuario cuando dejen de
 * coincidir.
 */
object ReelsNodeDetector {

    private val INSTAGRAM_REELS_HINTS = listOf(
        "clips_tab", "clips_viewer", "reels_tab", "clips_swipe_refresh_layout",
        "reel_viewer_root", "clips_video_container", "creation_camera_reel_tab",
        "clips_tab_avatar", "reels_player",
    )

    /** Si aparece cualquiera de estos ids, se considera que NO es Reels (p.ej. está en los DMs) y nunca se bloquea. */
    private val EXCLUDED_HINTS = listOf(
        "direct_inbox", "direct_thread", "action_bar_inbox_button", "direct_text_view",
        "direct_visual_message", "inbox_search_bar", "direct_thread_recycler_view",
    )

    val TIKTOK_PACKAGES = setOf(
        "com.zhiliaoapp.musically", "com.zhiliaoapp.musically.go", "com.ss.android.ugc.trill",
    )

    /**
     * A diferencia de Instagram (donde "estoy en Reels" es una pestaña concreta y fácil de marcar
     * con ids positivos), en TikTok el feed "Para ti" ES la pantalla principal por defecto — y sus
     * ids internos cambian tan seguido entre versiones que depender solo de coincidencias positivas
     * dejaba de detectar el feed en cuanto TikTok actualizaba su UI (el bug reportado: "no reconoce
     * el tiempo de uso de TikTok"). Por eso acá se invierte la lógica: se asume que CUALQUIER
     * pantalla de TikTok es el feed, A MENOS que se reconozca como otra cosa (mensajes, perfil,
     * buscar, en vivo, comentarios, cámara/creación) — más robusto ante esos cambios de ids, a
     * costa de ser un poco más permisivo con pantallas nuevas que todavía no estén en esta lista.
     */
    private val TIKTOK_EXCLUDED_HINTS = listOf(
        "inbox", "im_fragment", "chat_", "dm_", "direct_message", "message_list",
        "profile_", "user_profile", "personal_page", "mine_fragment",
        "comment_list", "comment_input", "comment_fragment",
        "search_", "discover_fragment", "hot_search",
        "live_room", "live_fragment", "anchor_",
        "creation_", "camera_", "record_",
        "settings_", "setting_fragment",
        "notification_fragment", "inbox_fragment",
    )

    private const val MAX_NODES_VISITED = 500

    data class DetectionResult(val surface: BlockSurface?, val matchedIds: Set<String>)

    fun detect(root: AccessibilityNodeInfo?, packageName: String): DetectionResult {
        if (root == null) return DetectionResult(null, emptySet())
        val collectedIds = collectResourceIds(root)

        if (EXCLUDED_HINTS.any { hint -> collectedIds.any { it.contains(hint, ignoreCase = true) } }) {
            return DetectionResult(null, collectedIds)
        }

        val surface = when {
            packageName == "com.instagram.android" &&
                INSTAGRAM_REELS_HINTS.any { hint -> collectedIds.any { it.contains(hint, ignoreCase = true) } } ->
                BlockSurface.INSTAGRAM_REELS
            packageName in TIKTOK_PACKAGES &&
                TIKTOK_EXCLUDED_HINTS.none { hint -> collectedIds.any { it.contains(hint, ignoreCase = true) } } ->
                BlockSurface.TIKTOK_FOR_YOU
            else -> null
        }
        return DetectionResult(surface, collectedIds)
    }

    private fun collectResourceIds(root: AccessibilityNodeInfo): Set<String> {
        val ids = mutableSetOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES_VISITED) {
            val node = queue.removeFirst()
            visited++
            node.viewIdResourceName?.let { ids.add(it) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return ids
    }
}
