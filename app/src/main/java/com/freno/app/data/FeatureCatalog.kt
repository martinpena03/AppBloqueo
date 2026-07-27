package com.freno.app.data

import com.freno.app.data.entity.FeatureSignature

/**
 * Catálogo de funciones "scroll infinito" detectables dentro de apps, con sus firmas por defecto.
 * Las firmas se comparan por "contains" contra viewId (resource-name) o contentDescription/text de los
 * nodos de accesibilidad; toleran cambios menores de versión. Editables/ampliables desde la app.
 */
object FeatureCatalog {

    data class FeatureDef(
        val featureKey: String,
        val packageName: String,
        val displayName: String
    )

    val YOUTUBE_SHORTS = FeatureDef("youtube_shorts", "com.google.android.youtube", "YouTube Shorts")
    val INSTAGRAM_REELS = FeatureDef("instagram_reels", "com.instagram.android", "Instagram Reels")
    val TIKTOK = FeatureDef("tiktok_feed", "com.zhiliaoapp.musically", "TikTok (feed)")
    val FACEBOOK_REELS = FeatureDef("facebook_reels", "com.facebook.katana", "Facebook Reels")

    val all = listOf(YOUTUBE_SHORTS, INSTAGRAM_REELS, TIKTOK, FACEBOOK_REELS)

    fun byKey(key: String): FeatureDef? = all.firstOrNull { it.featureKey == key }

    private fun sig(key: String, pkg: String, type: String, pattern: String) =
        FeatureSignature(featureKey = key, packageName = pkg, matchType = type, pattern = pattern)

    fun defaultSignatures(): List<FeatureSignature> = listOf(
        // YouTube Shorts
        sig("youtube_shorts", "com.google.android.youtube", "viewId", "reel_recycler"),
        sig("youtube_shorts", "com.google.android.youtube", "viewId", "reel_watch"),
        sig("youtube_shorts", "com.google.android.youtube", "viewId", "shorts"),
        sig("youtube_shorts", "com.google.android.youtube", "contentDesc", "Shorts"),
        // Instagram Reels
        sig("instagram_reels", "com.instagram.android", "viewId", "clips_viewer"),
        sig("instagram_reels", "com.instagram.android", "viewId", "clips_swipe"),
        sig("instagram_reels", "com.instagram.android", "viewId", "reel_viewer"),
        sig("instagram_reels", "com.instagram.android", "contentDesc", "Reels"),
        // TikTok (todo el feed principal es scroll vertical)
        sig("tiktok_feed", "com.zhiliaoapp.musically", "viewId", "feed"),
        sig("tiktok_feed", "com.zhiliaoapp.musically", "viewId", "vertical_view_pager"),
        // Facebook Reels
        sig("facebook_reels", "com.facebook.katana", "viewId", "reels"),
        sig("facebook_reels", "com.facebook.katana", "contentDesc", "Reels")
    )
}
