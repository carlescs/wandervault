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

        // TODO: Replace these placeholder values with the real values from your
        //  google-services.json (Firebase console → Project settings → Your apps).
        //  Alternatively, add the google-services.json file and apply the
        //  `com.google.gms.google-services` plugin in build.gradle.kts to have
        //  Firebase configured automatically.
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
