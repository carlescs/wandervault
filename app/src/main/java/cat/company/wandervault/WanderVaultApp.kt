package cat.company.wandervault

import android.app.Application
import cat.company.wandervault.data.di.dataModule
import cat.company.wandervault.data.notification.TripNotificationWorker
import cat.company.wandervault.domain.di.domainModule
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import cat.company.wandervault.ui.di.presentationModule
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WanderVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WanderVaultApplication)
            modules(dataModule, domainModule, presentationModule)
        }
        TripNotificationWorker.createNotificationChannel(this)
        val appPreferences: AppPreferencesRepository = get()
        if (appPreferences.getNotificationsEnabled()) {
            TripNotificationWorker.schedule(this)
        }
    }
}
