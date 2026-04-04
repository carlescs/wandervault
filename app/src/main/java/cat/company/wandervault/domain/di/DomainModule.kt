package cat.company.wandervault.domain.di

import cat.company.wandervault.domain.usecase.AskDocumentQuestionUseCase
import cat.company.wandervault.domain.usecase.AutoOrganizeDocumentsUseCase
import cat.company.wandervault.domain.usecase.CopyDocumentToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.ArchiveTripUseCase
import cat.company.wandervault.domain.usecase.DeleteActivityUseCase
import cat.company.wandervault.domain.usecase.GenerateWhatsNextUseCase
import cat.company.wandervault.domain.usecase.GetArchivedTripsUseCase
import cat.company.wandervault.domain.usecase.GetFavoriteTripsUseCase
import cat.company.wandervault.domain.usecase.ToggleFavoriteTripUseCase
import cat.company.wandervault.domain.usecase.CopyImageToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.CreateBackupUseCase
import cat.company.wandervault.domain.usecase.DeleteDestinationUseCase
import cat.company.wandervault.domain.usecase.DeleteDocumentUseCase
import cat.company.wandervault.domain.usecase.DeleteFolderUseCase
import cat.company.wandervault.domain.usecase.DeleteHotelUseCase
import cat.company.wandervault.domain.usecase.DeleteImageUseCase
import cat.company.wandervault.domain.usecase.DeleteTransportLegUseCase
import cat.company.wandervault.domain.usecase.DeleteTransportUseCase
import cat.company.wandervault.domain.usecase.DeleteTripUseCase
import cat.company.wandervault.domain.usecase.GenerateTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.GetActivitiesForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetArrivalTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetDocumentsInFolderUseCase
import cat.company.wandervault.domain.usecase.GetAllFoldersForTripUseCase
import cat.company.wandervault.domain.usecase.GetAllDocumentsForTripUseCase
import cat.company.wandervault.domain.usecase.GetDocumentByIdUseCase
import cat.company.wandervault.domain.usecase.GetNextDestinationUseCase
import cat.company.wandervault.domain.usecase.GetOrCreateTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetHotelForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetHotelsForDestinationsUseCase
import cat.company.wandervault.domain.usecase.GetRootDocumentsUseCase
import cat.company.wandervault.domain.usecase.GetRootFoldersUseCase
import cat.company.wandervault.domain.usecase.GetSubFoldersUseCase
import cat.company.wandervault.domain.usecase.SaveDocumentUseCase
import cat.company.wandervault.domain.usecase.SaveFolderUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.GetTripsUseCase
import cat.company.wandervault.domain.usecase.RestoreBackupUseCase
import cat.company.wandervault.domain.usecase.SaveActivityUseCase
import cat.company.wandervault.domain.usecase.SaveDestinationUseCase
import cat.company.wandervault.domain.usecase.SaveTransportLegUseCase
import cat.company.wandervault.domain.usecase.SaveTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.SaveTripWhatsNextUseCase
import cat.company.wandervault.domain.usecase.SaveTripUseCase
import cat.company.wandervault.domain.usecase.SummarizeDocumentUseCase
import cat.company.wandervault.domain.usecase.SuggestDocumentNameUseCase
import cat.company.wandervault.domain.usecase.UpdateDestinationUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateFolderUseCase
import cat.company.wandervault.domain.usecase.UpdateTransportLegUseCase
import cat.company.wandervault.domain.usecase.UpdateTripUseCase
import cat.company.wandervault.domain.usecase.UnarchiveTripUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetTripUseCase(get()) }
    factory { GetTripsUseCase(get()) }
    factory { GetFavoriteTripsUseCase(get()) }
    factory { GetArchivedTripsUseCase(get()) }
    factory { ToggleFavoriteTripUseCase(get()) }
    factory { ArchiveTripUseCase(get()) }
    factory { UnarchiveTripUseCase(get()) }
    factory { SaveTripUseCase(get()) }
    factory { UpdateTripUseCase(get()) }
    factory { GenerateTripDescriptionUseCase(get()) }
    factory { SaveTripDescriptionUseCase(get()) }
    factory { GenerateWhatsNextUseCase(get()) }
    factory { SaveTripWhatsNextUseCase(get()) }
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
    factory { DeleteTripUseCase(get(), get(), get()) }
    factory { CopyImageToInternalStorageUseCase(get()) }
    factory { DeleteImageUseCase(get()) }
    factory { CreateBackupUseCase(get()) }
    factory { RestoreBackupUseCase(get()) }
    factory { GetHotelForDestinationUseCase(get()) }
    factory { GetHotelsForDestinationsUseCase(get()) }
    factory { SaveHotelUseCase(get()) }
    factory { DeleteHotelUseCase(get()) }
    factory { GetActivitiesForDestinationUseCase(get()) }
    factory { SaveActivityUseCase(get()) }
    factory { DeleteActivityUseCase(get()) }
    factory { GetRootFoldersUseCase(get()) }
    factory { GetSubFoldersUseCase(get()) }
    factory { GetAllFoldersForTripUseCase(get()) }
    factory { GetAllDocumentsForTripUseCase(get()) }
    factory { GetDocumentsInFolderUseCase(get()) }
    factory { GetRootDocumentsUseCase(get()) }
    factory { SaveFolderUseCase(get()) }
    factory { UpdateFolderUseCase(get()) }
    factory { DeleteFolderUseCase(get()) }
    factory { SaveDocumentUseCase(get()) }
    factory { UpdateDocumentUseCase(get()) }
    factory { DeleteDocumentUseCase(get()) }
    factory { CopyDocumentToInternalStorageUseCase(get()) }
    factory { SummarizeDocumentUseCase(get()) }
    factory { SuggestDocumentNameUseCase(get()) }
    factory { AskDocumentQuestionUseCase(get()) }
    factory { AutoOrganizeDocumentsUseCase(get()) }
    factory { GetDocumentByIdUseCase(get()) }
}
