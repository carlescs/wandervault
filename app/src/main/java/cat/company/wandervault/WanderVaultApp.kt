package cat.company.wandervault

import android.app.Application
import cat.company.wandervault.data.di.dataModule
import cat.company.wandervault.domain.di.domainModule
import cat.company.wandervault.ui.di.presentationModule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WanderVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // TODO: Replace the placeholder values below with real values from your Firebase project.
        //  The recommended approach is to add your google-services.json file and apply the
        //  `com.google.gms.google-services` Gradle plugin, which replaces the manual init below.
        //  Until then the app will start but all Firebase calls will fail at runtime.
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(
                this,
                FirebaseOptions.Builder()
                    .setProjectId("your-project-id")
                    .setApplicationId("1:000000000000:android:placeholder")
                    .setApiKey("placeholder-api-key")
                    .setStorageBucket("your-project-id.appspot.com")
                    .build(),
            )
        }

        startKoin {
            androidContext(this@WanderVaultApplication)
            modules(dataModule, domainModule, presentationModule)
        }
    }
}
