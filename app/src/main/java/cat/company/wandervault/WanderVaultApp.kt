package cat.company.wandervault

import android.app.Application
import cat.company.wandervault.data.di.dataModule
import cat.company.wandervault.domain.di.domainModule
import cat.company.wandervault.ui.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WanderVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WanderVaultApp)
            modules(dataModule, domainModule, presentationModule)
        }
    }
}
