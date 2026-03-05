package cat.company.wandervault.data.di

import androidx.room.Room
import cat.company.wandervault.data.local.WanderVaultDatabase
import cat.company.wandervault.data.repository.DestinationRepositoryImpl
import cat.company.wandervault.data.repository.HotelRepositoryImpl
import cat.company.wandervault.data.repository.ImageRepositoryImpl
import cat.company.wandervault.data.repository.TransportRepositoryImpl
import cat.company.wandervault.data.repository.TripRepositoryImpl
import cat.company.wandervault.domain.repository.DestinationRepository
import cat.company.wandervault.domain.repository.HotelRepository
import cat.company.wandervault.domain.repository.ImageRepository
import cat.company.wandervault.domain.repository.TransportRepository
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
        ).build()
    }
    single { get<WanderVaultDatabase>().tripDao() }
    single { get<WanderVaultDatabase>().destinationDao() }
    single { get<WanderVaultDatabase>().transportDao() }
    single { get<WanderVaultDatabase>().hotelDao() }
    single<TripRepository> { TripRepositoryImpl(get(), get()) }
    single<DestinationRepository> { DestinationRepositoryImpl(get(), get()) }
    single<TransportRepository> { TransportRepositoryImpl(get()) }
    single<HotelRepository> { HotelRepositoryImpl(get()) }
    single<ImageRepository> { ImageRepositoryImpl(androidContext()) }
}
