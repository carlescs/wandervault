package cat.company.wandervault.data.di

import androidx.room.Room
import cat.company.wandervault.data.local.WanderVaultDatabase
import cat.company.wandervault.data.mlkit.DocumentSummaryRepositoryImpl
import cat.company.wandervault.data.mlkit.TripDescriptionRepositoryImpl
import cat.company.wandervault.data.remote.firebase.FirebaseAuthRepositoryImpl
import cat.company.wandervault.data.remote.firebase.FirebaseStorageDocumentRepositoryImpl
import cat.company.wandervault.data.remote.firebase.FirestoreTripSyncRepositoryImpl
import cat.company.wandervault.data.repository.AppPreferencesRepositoryImpl
import cat.company.wandervault.data.repository.BackupRepositoryImpl
import cat.company.wandervault.data.repository.DestinationRepositoryImpl
import cat.company.wandervault.data.repository.ImageRepositoryImpl
import cat.company.wandervault.data.repository.TransportRepositoryImpl
import cat.company.wandervault.data.repository.TripDocumentRepositoryImpl
import cat.company.wandervault.data.repository.TripRepositoryImpl
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import cat.company.wandervault.domain.repository.AuthRepository
import cat.company.wandervault.domain.repository.BackupRepository
import cat.company.wandervault.domain.repository.DocumentSummaryRepository
import cat.company.wandervault.domain.repository.DestinationRepository
import cat.company.wandervault.data.repository.HotelRepositoryImpl
import cat.company.wandervault.domain.repository.HotelRepository
import cat.company.wandervault.domain.repository.ImageRepository
import cat.company.wandervault.domain.repository.RemoteDocumentRepository
import cat.company.wandervault.domain.repository.TransportRepository
import cat.company.wandervault.domain.repository.TripDescriptionRepository
import cat.company.wandervault.domain.repository.TripDocumentRepository
import cat.company.wandervault.domain.repository.TripRepository
import cat.company.wandervault.domain.repository.TripSyncRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    // ── Firebase ─────────────────────────────────────────────────────────────
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }

    // ── Room database ─────────────────────────────────────────────────────────
    single {
        Room.databaseBuilder(
            androidContext(),
            WanderVaultDatabase::class.java,
            WanderVaultDatabase.DATABASE_NAME,
        ).addMigrations(
            WanderVaultDatabase.MIGRATION_1_2,
            WanderVaultDatabase.MIGRATION_2_3,
            WanderVaultDatabase.MIGRATION_3_4,
            WanderVaultDatabase.MIGRATION_4_5,
            WanderVaultDatabase.MIGRATION_5_6,
            WanderVaultDatabase.MIGRATION_6_7,
            WanderVaultDatabase.MIGRATION_7_8,
            WanderVaultDatabase.MIGRATION_8_9,
            WanderVaultDatabase.MIGRATION_9_10,
            WanderVaultDatabase.MIGRATION_10_11,
            WanderVaultDatabase.MIGRATION_11_12,
            WanderVaultDatabase.MIGRATION_12_13,
            WanderVaultDatabase.MIGRATION_13_14,
            WanderVaultDatabase.MIGRATION_14_15,
            WanderVaultDatabase.MIGRATION_15_16,
            WanderVaultDatabase.MIGRATION_16_17,
            WanderVaultDatabase.MIGRATION_17_18,
            WanderVaultDatabase.MIGRATION_18_19,
            WanderVaultDatabase.MIGRATION_19_20,
            WanderVaultDatabase.MIGRATION_20_21,
            WanderVaultDatabase.MIGRATION_21_22,
        ).build()
    }
    single { get<WanderVaultDatabase>().tripDao() }
    single { get<WanderVaultDatabase>().destinationDao() }
    single { get<WanderVaultDatabase>().transportDao() }
    single { get<WanderVaultDatabase>().transportLegDao() }
    single { get<WanderVaultDatabase>().hotelDao() }
    single { get<WanderVaultDatabase>().tripDocumentFolderDao() }
    single { get<WanderVaultDatabase>().tripDocumentDao() }

    // ── Local repositories ────────────────────────────────────────────────────
    single<TripRepository> { TripRepositoryImpl(get(), get(), get()) }
    single<DestinationRepository> { DestinationRepositoryImpl(get(), get(), get()) }
    single<TransportRepository> { TransportRepositoryImpl(get(), get()) }
    single<HotelRepository> { HotelRepositoryImpl(get()) }
    single<TripDocumentRepository> { TripDocumentRepositoryImpl(androidContext(), get(), get()) }
    single<ImageRepository> { ImageRepositoryImpl(androidContext()) }
    single<TripDescriptionRepository> { TripDescriptionRepositoryImpl(get()) }
    single<DocumentSummaryRepository> { DocumentSummaryRepositoryImpl(androidContext(), get()) }
    single<BackupRepository> { BackupRepositoryImpl(androidContext(), get()) }
    single<AppPreferencesRepository> { AppPreferencesRepositoryImpl(androidContext()) }

    // ── Firebase / remote repositories ───────────────────────────────────────
    single<AuthRepository> {
        FirebaseAuthRepositoryImpl(
            context = androidContext(),
            auth = get(),
            // TODO: Replace "YOUR_WEB_CLIENT_ID" with the OAuth 2.0 Web Client ID from the
            //  Firebase console (Project settings → Your apps → SHA certificate fingerprints →
            //  Web API key).  In a production build this should be read from BuildConfig or
            //  injected via local.properties / environment variables, never hard-coded.
            webClientId = "YOUR_WEB_CLIENT_ID",
        )
    }
    single<TripSyncRepository> {
        FirestoreTripSyncRepositoryImpl(
            firestore = get(),
            auth = get(),
            tripRepository = get(),
            tripDao = get(),
        )
    }
    single<RemoteDocumentRepository> {
        FirebaseStorageDocumentRepositoryImpl(
            context = androidContext(),
            storage = get(),
            tripDocumentDao = get(),
        )
    }
}
