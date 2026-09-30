package com.qingjizhang.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.qingjizhang.app.data.AppDatabase
import com.qingjizhang.app.data.BackupManager
import com.qingjizhang.app.data.FinanceRepository
import com.qingjizhang.app.data.ReceiptStore
import com.qingjizhang.app.data.Seeder
import com.qingjizhang.app.data.SettingsStore
import com.qingjizhang.app.ui.i18n.LocaleHelper
import com.qingjizhang.app.widget.MonthBalanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class AppContainer(val application: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db = AppDatabase.create(application)
    val settings = SettingsStore(application)
    val receipts = ReceiptStore(application)
    val repo = FinanceRepository(db, receipts) { MonthBalanceWidget.refresh(application) }
    val seeder = Seeder(db, settings)
    val backup = BackupManager(repo, settings)
}

class QingJiZhangApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        LocaleHelper.syncFromBlocking(this)
        container = AppContainer(this)
        runBlocking {
            if (LocaleHelper.usesSystemLocaleAsSourceOfTruth()) {
                LocaleHelper.syncSystemLocalesIntoAppStorage(this@QingJiZhangApp, container.settings)
            } else {
                val language = SettingsStore.readLanguageBlocking(this@QingJiZhangApp)
                LocaleHelper.persistForBoot(this@QingJiZhangApp, language)
                LocaleHelper.applyAppLanguage(language)
            }
        }
        container.scope.launch {
            container.seeder.seedIfNeeded()
            container.repo.generateDueRecurring(LocalDate.now())
            MonthBalanceWidget.refresh(this@QingJiZhangApp)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                container.scope.launch {
                    if (LocaleHelper.usesSystemLocaleAsSourceOfTruth()) {
                        LocaleHelper.syncSystemLocalesIntoAppStorage(
                            this@QingJiZhangApp,
                            container.settings,
                        )
                    }
                    container.repo.generateDueRecurring(LocalDate.now())
                    MonthBalanceWidget.refresh(this@QingJiZhangApp)
                }
            }
        })
    }
}
