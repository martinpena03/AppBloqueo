package com.freno.app.data

import androidx.room.TypeConverter
import com.freno.app.domain.model.QuotaWindow
import com.freno.app.domain.model.TargetType

class Converters {
    @TypeConverter fun toTargetType(v: String): TargetType = TargetType.valueOf(v)
    @TypeConverter fun fromTargetType(t: TargetType): String = t.name

    @TypeConverter fun toQuotaWindow(v: String): QuotaWindow = QuotaWindow.valueOf(v)
    @TypeConverter fun fromQuotaWindow(q: QuotaWindow): String = q.name
}
