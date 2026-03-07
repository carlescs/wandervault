package cat.company.wandervault.domain.di

import cat.company.wandervault.domain.usecase.GetFavoriteTripsUseCase
import cat.company.wandervault.domain.usecase.ToggleFavoriteTripUseCase
import cat.company.wandervault.domain.usecase.CopyImageToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.CreateBackupUseCase
import cat.company.wandervault.domain.usecase.DeleteDestinationUseCase
import cat.company.wandervault.domain.usecase.DeleteImageUseCase
import cat.company.wandervault.domain.usecase.DeleteTransportLegUseCase
import cat.company.wandervault.domain.usecase.DeleteTransportUseCase
import cat.company.wandervault.domain.usecase.DeleteTripUseCase
import cat.company.wandervault.domain.usecase.GenerateTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.GetArrivalTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetNextDestinationUseCase
import cat.company.wandervault.domain.usecase.GetOrCreateTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.DeleteHotelUseCase
import cat.company.wandervault.domain.usecase.GetHotelForDestinationUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.GetTripsUseCase
import cat.company.wandervault.domain.usecase.RestoreBackupUseCase
import cat.company.wandervault.domain.usecase.SaveDestinationUseCase
import cat.company.wandervault.domain.usecase.SaveTransportLegUseCase
import cat.company.wandervault.domain.usecase.SaveTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.SaveTripUseCase
import cat.company.wandervault.domain.usecase.UpdateDestinationUseCase
import cat.company.wandervault.domain.usecase.UpdateTransportLegUseCase
import cat.company.wandervault.domain.usecase.UpdateTripUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetTripUseCase(get()) }
    factory { GetTripsUseCase(get()) }
    factory { GetFavoriteTripsUseCase(get()) }
    factory { ToggleFavoriteTripUseCase(get()) }
    factory { SaveTripUseCase(get()) }
    factory { UpdateTripUseCase(get()) }
    factory { GenerateTripDescriptionUseCase(get()) }
    factory { SaveTripDescriptionUseCase(get()) }
    factory { GetDestinationByIdUseCase(get()) }
    factory { GetNextDestinationUseCase(get()) }
    factory { GetArrivalTransportForDestinationUseCase(get()) }
    factory { GetDestinationsForTripUseCase(get()) }
    factory { SaveDestinationUseCase(get()) }
    factory { UpdateDestinationUseCase(get()) }
    factory { DeleteDestinationUseCase(get()) }
    factory { GetOrCreateTransportForDestinationUseCase(get()) }
    factory { SaveTransportLegUseCase(get()) }
    factory { UpdateTransportLegUseCase(get()) }
    factory { DeleteTransportLegUseCase(get()) }
    factory { DeleteTransportUseCase(get()) }
    factory { DeleteTripUseCase(get()) }
    factory { CopyImageToInternalStorageUseCase(get()) }
    factory { DeleteImageUseCase(get()) }
    factory { CreateBackupUseCase(get()) }
    factory { RestoreBackupUseCase(get()) }
    factory { GetHotelForDestinationUseCase(get()) }
    factory { SaveHotelUseCase(get()) }
    factory { DeleteHotelUseCase(get()) }
}
