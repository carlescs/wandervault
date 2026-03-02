package cat.company.wandervault.data.di

import androidx.room.Room
import cat.company.wandervault.data.local.WanderVaultDatabase
import cat.company.wandervault.data.repository.DestinationRepositoryImpl
import cat.company.wandervault.data.repository.TripRepositoryImpl
import cat.company.wandervault.domain.repository.DestinationRepository
import cat.company.wandervault.domain.repository.TripRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            WanderVaultDatabase::class.java,
            WanderVaultDatabase.DATABASE_NAME,
        ).addMigrations(WanderVaultDatabase.MIGRATION_1_2, WanderVaultDatabase.MIGRATION_2_3).build()
    }
    single { get<WanderVaultDatabase>().tripDao() }
    single { get<WanderVaultDatabase>().destinationDao() }
    single<TripRepository> { TripRepositoryImpl(get()) }
    single<DestinationRepository> { DestinationRepositoryImpl(get()) }
}
