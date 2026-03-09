package cat.company.wandervault.data.di

import androidx.room.Room
import cat.company.wandervault.data.local.WanderVaultDatabase
import cat.company.wandervault.data.mlkit.DocumentSummaryRepositoryImpl
import cat.company.wandervault.data.mlkit.TripDescriptionRepositoryImpl
import cat.company.wandervault.data.repository.BackupRepositoryImpl
import cat.company.wandervault.data.repository.DestinationRepositoryImpl
import cat.company.wandervault.data.repository.ImageRepositoryImpl
import cat.company.wandervault.data.repository.TransportRepositoryImpl
import cat.company.wandervault.data.repository.TripDocumentRepositoryImpl
import cat.company.wandervault.data.repository.TripRepositoryImpl
import cat.company.wandervault.domain.repository.BackupRepository
import cat.company.wandervault.domain.repository.DocumentSummaryRepository
import cat.company.wandervault.domain.repository.DestinationRepository
import cat.company.wandervault.data.repository.HotelRepositoryImpl
import cat.company.wandervault.domain.repository.HotelRepository
import cat.company.wandervault.domain.repository.ImageRepository
import cat.company.wandervault.domain.repository.TransportRepository
import cat.company.wandervault.domain.repository.TripDescriptionRepository
import cat.company.wandervault.domain.repository.TripDocumentRepository
import cat.company.wandervault.domain.repository.TripRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
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
        ).build()
    }
    single { get<WanderVaultDatabase>().tripDao() }
    single { get<WanderVaultDatabase>().destinationDao() }
    single { get<WanderVaultDatabase>().transportDao() }
    single { get<WanderVaultDatabase>().transportLegDao() }
    single { get<WanderVaultDatabase>().hotelDao() }
    single { get<WanderVaultDatabase>().tripDocumentFolderDao() }
    single { get<WanderVaultDatabase>().tripDocumentDao() }
    single<TripRepository> { TripRepositoryImpl(get(), get(), get()) }
    single<DestinationRepository> { DestinationRepositoryImpl(get(), get(), get()) }
    single<TransportRepository> { TransportRepositoryImpl(get(), get()) }
    single<HotelRepository> { HotelRepositoryImpl(get()) }
    single<TripDocumentRepository> { TripDocumentRepositoryImpl(androidContext(), get(), get()) }
    single<ImageRepository> { ImageRepositoryImpl(androidContext()) }
    single<TripDescriptionRepository> { TripDescriptionRepositoryImpl() }
    single<DocumentSummaryRepository> { DocumentSummaryRepositoryImpl(androidContext()) }
    single<BackupRepository> { BackupRepositoryImpl(androidContext(), get()) }
}
