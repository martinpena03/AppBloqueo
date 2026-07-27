package com.freno.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.freno.app.data.entity.DayState
import com.freno.app.data.entity.FeatureSignature
import com.freno.app.data.entity.MonitoredTarget
import com.freno.app.data.entity.PendingChange
import com.freno.app.data.entity.TargetDailyStat
import com.freno.app.data.entity.TargetRuntimeState
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetDao {
    @Query("SELECT * FROM targets ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<MonitoredTarget>>

    @Query("SELECT * FROM targets ORDER BY displayName COLLATE NOCASE")
    suspend fun getAllSorted(): List<MonitoredTarget>

    @Query("SELECT * FROM targets WHERE enabled = 1")
    suspend fun getEnabled(): List<MonitoredTarget>

    @Query("SELECT * FROM targets WHERE targetId = :id")
    suspend fun getById(id: String): MonitoredTarget?

    @Query("SELECT * FROM targets WHERE packageName = :pkg")
    suspend fun getByPackage(pkg: String): List<MonitoredTarget>

    @Upsert
    suspend fun upsert(target: MonitoredTarget)

    @Query("DELETE FROM targets WHERE targetId = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE targets SET enabled = :enabled WHERE targetId = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}

@Dao
interface StatDao {
    @Query("SELECT * FROM daily_stats WHERE targetId = :id AND dateKey = :date")
    suspend fun get(id: String, date: String): TargetDailyStat?

    @Query("SELECT * FROM daily_stats WHERE dateKey = :date")
    fun observeForDate(date: String): Flow<List<TargetDailyStat>>

    @Upsert
    suspend fun upsert(stat: TargetDailyStat)

    @Query("DELETE FROM daily_stats WHERE dateKey != :keepDate")
    suspend fun deleteOtherDays(keepDate: String)
}

@Dao
interface RuntimeDao {
    @Query("SELECT * FROM runtime_state WHERE targetId = :id")
    suspend fun get(id: String): TargetRuntimeState?

    @Query("SELECT * FROM runtime_state")
    fun observeAll(): Flow<List<TargetRuntimeState>>

    @Upsert
    suspend fun upsert(state: TargetRuntimeState)

    /** Reinicio diario: libera los bloqueos por cuota y cierra cualquier sesión en curso. */
    @Query("UPDATE runtime_state SET quotaBlockedUntil = 0, sessionSeconds = 0")
    suspend fun clearAllQuotaBlocks()

    @Query("UPDATE runtime_state SET isForeground = 0")
    suspend fun clearForeground()
}

@Dao
interface DayStateDao {
    @Query("SELECT * FROM day_state WHERE id = 0")
    suspend fun get(): DayState?

    @Query("SELECT * FROM day_state WHERE id = 0")
    fun observe(): Flow<DayState?>

    @Upsert
    suspend fun upsert(state: DayState)
}

@Dao
interface PendingChangeDao {
    @Query("SELECT * FROM pending_changes ORDER BY applyAt ASC")
    fun observeAll(): Flow<List<PendingChange>>

    @Query("SELECT * FROM pending_changes WHERE applyAt <= :now ORDER BY applyAt ASC")
    suspend fun getDue(now: Long): List<PendingChange>

    @Insert
    suspend fun insert(change: PendingChange)

    @Delete
    suspend fun delete(change: PendingChange)

    @Query("DELETE FROM pending_changes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FeatureSignatureDao {
    @Query("SELECT * FROM feature_signatures")
    suspend fun getAll(): List<FeatureSignature>

    @Query("SELECT * FROM feature_signatures")
    fun observeAll(): Flow<List<FeatureSignature>>

    @Insert
    suspend fun insertAll(list: List<FeatureSignature>)

    @Query("SELECT COUNT(*) FROM feature_signatures")
    suspend fun count(): Int
}
