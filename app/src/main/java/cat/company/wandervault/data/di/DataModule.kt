package cat.company.wandervault.data.di

import androidx.room.Room
import cat.company.wandervault.data.local.WanderVaultDatabase
import cat.company.wandervault.data.repository.TripRepositoryImpl
import cat.company.wandervault.domain.repository.TripRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            WanderVaultDatabase::class.java,
            WanderVaultDatabase.DATABASE_NAME,
        ).build()
    }
    single { get<WanderVaultDatabase>().tripDao() }
    single<TripRepository> { TripRepositoryImpl(get()) }
}
