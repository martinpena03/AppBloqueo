package com.freno.app.di

import android.content.Context
import com.freno.app.data.AppDatabase
import com.freno.app.data.prefs.SettingsStore
import com.freno.app.data.repo.AppRepository

/** Service locator mínimo (sin frameworks de DI) para compartir DB y repositorio. */
object Graph {
    @Volatile
    private var initialized = false

    lateinit var database: AppDatabase
        private set
    lateinit var repository: AppRepository
        private set
    lateinit var settings: SettingsStore
        private set

    val isReady: Boolean get() = initialized

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext
            database = AppDatabase.build(app)
            settings = SettingsStore(app)
            repository = AppRepository(
                appContext = app,
                targetDao = database.targetDao(),
                statDao = database.statDao(),
                runtimeDao = database.runtimeDao(),
                dayStateDao = database.dayStateDao(),
                pendingDao = database.pendingChangeDao(),
                signatureDao = database.featureSignatureDao(),
                settings = settings
            )
            initialized = true
        }
    }
}
