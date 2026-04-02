package cat.company.wandervault

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
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
        if (appPreferences.getNotificationsEnabled() && hasNotificationPermission()) {
            TripNotificationWorker.schedule(this)
        }
    }

    /**
     * Returns `true` when the app is permitted to post notifications.
     * On Android < 13 (TIRAMISU) this is always true since no runtime permission is required.
     */
    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
