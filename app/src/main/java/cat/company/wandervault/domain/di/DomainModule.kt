package cat.company.wandervault.domain.di

import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.GetTripsUseCase
import cat.company.wandervault.domain.usecase.SaveTripUseCase
import cat.company.wandervault.domain.usecase.UpdateTripUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetTripUseCase(get()) }
    factory { GetTripsUseCase(get()) }
    factory { SaveTripUseCase(get()) }
    factory { UpdateTripUseCase(get()) }
}
