package com.freno.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.freno.app.data.dao.DayStateDao
import com.freno.app.data.dao.FeatureSignatureDao
import com.freno.app.data.dao.PendingChangeDao
import com.freno.app.data.dao.RuntimeDao
import com.freno.app.data.dao.StatDao
import com.freno.app.data.dao.TargetDao
import com.freno.app.data.entity.DayState
import com.freno.app.data.entity.FeatureSignature
import com.freno.app.data.entity.MonitoredTarget
import com.freno.app.data.entity.PendingChange
import com.freno.app.data.entity.TargetDailyStat
import com.freno.app.data.entity.TargetRuntimeState

@Database(
    entities = [
        MonitoredTarget::class,
        TargetDailyStat::class,
        TargetRuntimeState::class,
        DayState::class,
        PendingChange::class,
        FeatureSignature::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun targetDao(): TargetDao
    abstract fun statDao(): StatDao
    abstract fun runtimeDao(): RuntimeDao
    abstract fun dayStateDao(): DayStateDao
    abstract fun pendingChangeDao(): PendingChangeDao
    abstract fun featureSignatureDao(): FeatureSignatureDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "freno.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
