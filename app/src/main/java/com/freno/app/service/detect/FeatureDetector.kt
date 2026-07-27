package com.freno.app.service.detect

import android.view.accessibility.AccessibilityNodeInfo
import com.freno.app.data.entity.FeatureSignature

/**
 * Recorre (BFS acotado) el árbol de nodos de la ventana activa y devuelve el featureKey de la primera
 * firma que coincida. Compara por "contains" contra viewId (resource-name) o contentDescription/text.
 */
object FeatureDetector {

    private const val MAX_NODES = 500

    fun detect(root: AccessibilityNodeInfo?, signatures: List<FeatureSignature>): String? {
        if (root == null || signatures.isEmpty()) return null

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++

            val viewId = node.viewIdResourceName?.lowercase()
            val desc = node.contentDescription?.toString()?.lowercase()
            val text = node.text?.toString()?.lowercase()

            for (s in signatures) {
                val pat = s.pattern.lowercase()
                val match = when (s.matchType) {
                    "viewId" -> viewId != null && viewId.contains(pat)
                    "contentDesc" ->
                        (desc != null && desc.contains(pat)) || (text != null && text.contains(pat))
                    else -> false
                }
                if (match) return s.featureKey
            }

            val count = node.childCount
            for (i in 0 until count) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }
}
