package com.freno.app.data

import com.freno.app.data.entity.MonitoredTarget
import com.freno.app.domain.model.QuotaWindow
import com.freno.app.domain.model.TargetType
import org.json.JSONObject

/** Serialización simple de MonitoredTarget para guardar cambios diferidos (usa org.json de Android). */
object TargetJson {

    fun encode(t: MonitoredTarget): String = JSONObject().apply {
        put("targetId", t.targetId)
        put("type", t.type.name)
        put("packageName", t.packageName)
        put("featureKey", t.featureKey ?: JSONObject.NULL)
        put("displayName", t.displayName)
        put("enabled", t.enabled)
        put("openCostTokens", t.openCostTokens)
        put("perMinuteCostTokens", t.perMinuteCostTokens)
        put("perScrollCost", t.perScrollCost)
        put("dailyTimeLimitMin", t.dailyTimeLimitMin ?: JSONObject.NULL)
        put("dailyOpenLimit", t.dailyOpenLimit ?: JSONObject.NULL)
        put("cooldownMin", t.cooldownMin ?: JSONObject.NULL)
        put("scheduleStart", t.scheduleStart ?: JSONObject.NULL)
        put("scheduleEnd", t.scheduleEnd ?: JSONObject.NULL)
        put("scrollQuota", t.scrollQuota ?: JSONObject.NULL)
        put("quotaWindow", t.quotaWindow.name)
        put("quotaWindowHours", t.quotaWindowHours)
    }.toString()

    fun decode(s: String): MonitoredTarget {
        val o = JSONObject(s)
        fun intOrNull(k: String): Int? = if (o.isNull(k)) null else o.getInt(k)
        return MonitoredTarget(
            targetId = o.getString("targetId"),
            type = TargetType.valueOf(o.getString("type")),
            packageName = o.getString("packageName"),
            featureKey = if (o.isNull("featureKey")) null else o.getString("featureKey"),
            displayName = o.getString("displayName"),
            enabled = o.getBoolean("enabled"),
            openCostTokens = o.getInt("openCostTokens"),
            perMinuteCostTokens = o.getInt("perMinuteCostTokens"),
            perScrollCost = o.getInt("perScrollCost"),
            dailyTimeLimitMin = intOrNull("dailyTimeLimitMin"),
            dailyOpenLimit = intOrNull("dailyOpenLimit"),
            cooldownMin = intOrNull("cooldownMin"),
            scheduleStart = intOrNull("scheduleStart"),
            scheduleEnd = intOrNull("scheduleEnd"),
            scrollQuota = intOrNull("scrollQuota"),
            quotaWindow = QuotaWindow.valueOf(o.getString("quotaWindow")),
            quotaWindowHours = o.getInt("quotaWindowHours")
        )
    }
}
