package cat.company.wandervault.domain.di

import cat.company.wandervault.domain.usecase.CopyImageToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.DeleteDestinationUseCase
import cat.company.wandervault.domain.usecase.DeleteImageUseCase
import cat.company.wandervault.domain.usecase.DeleteTransportUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.GetTripsUseCase
import cat.company.wandervault.domain.usecase.SaveDestinationUseCase
import cat.company.wandervault.domain.usecase.SaveTransportUseCase
import cat.company.wandervault.domain.usecase.SaveTripUseCase
import cat.company.wandervault.domain.usecase.UpdateDestinationUseCase
import cat.company.wandervault.domain.usecase.UpdateTransportUseCase
import cat.company.wandervault.domain.usecase.UpdateTripUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetTripUseCase(get()) }
    factory { GetTripsUseCase(get()) }
    factory { SaveTripUseCase(get()) }
    factory { UpdateTripUseCase(get()) }
    factory { GetDestinationsForTripUseCase(get()) }
    factory { SaveDestinationUseCase(get()) }
    factory { UpdateDestinationUseCase(get()) }
    factory { DeleteDestinationUseCase(get()) }
    factory { SaveTransportUseCase(get()) }
    factory { UpdateTransportUseCase(get()) }
    factory { DeleteTransportUseCase(get()) }
    factory { CopyImageToInternalStorageUseCase(get()) }
    factory { DeleteImageUseCase(get()) }
}
