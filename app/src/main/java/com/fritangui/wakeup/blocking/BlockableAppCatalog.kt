package com.fritangui.wakeup.blocking

import com.fritangui.wakeup.data.db.entity.BlockSurface

/**
 * Lista fija de apps que la pantalla de Bloqueo sabe limitar. Reemplaza el flujo anterior de
 * "crear/eliminar reglas a mano": cada entrada de esta lista es una fila fija en la UI que se
 * activa/desactiva con un switch (ver [com.fritangui.wakeup.ui.blocking.BlockingViewModel]), en vez
 * de requerir que el usuario arme sus propias reglas desde cero.
 */
data class BlockableApp(
    val surface: BlockSurface,
    val packageName: String,
    val label: String,
    /** Solo Instagram y TikTok distinguen su feed de scroll infinito del resto de la app (ver [BlockSurface]). */
    val limitsWholeApp: Boolean,
    val defaultDailyLimitMinutes: Int = 30,
)

object BlockableAppCatalog {
    val apps: List<BlockableApp> = listOf(
        BlockableApp(BlockSurface.INSTAGRAM_REELS, "com.instagram.android", "Reels de Instagram", limitsWholeApp = false),
        BlockableApp(BlockSurface.TIKTOK_FOR_YOU, "com.zhiliaoapp.musically", "TikTok", limitsWholeApp = false),
        BlockableApp(BlockSurface.YOUTUBE, "com.google.android.youtube", "YouTube", limitsWholeApp = true),
        BlockableApp(BlockSurface.FACEBOOK, "com.facebook.katana", "Facebook", limitsWholeApp = true),
        BlockableApp(BlockSurface.TWITTER_X, "com.twitter.android", "X (Twitter)", limitsWholeApp = true),
        BlockableApp(BlockSurface.SNAPCHAT, "com.snapchat.android", "Snapchat", limitsWholeApp = true),
        BlockableApp(BlockSurface.REDDIT, "com.reddit.frontpage", "Reddit", limitsWholeApp = true),
    )

    /** Para TikTok, cualquiera de sus variantes de paquete cuenta como "la app está instalada". */
    fun packageAliases(app: BlockableApp): Set<String> =
        if (app.surface == BlockSurface.TIKTOK_FOR_YOU) ReelsNodeDetector.TIKTOK_PACKAGES else setOf(app.packageName)
}
