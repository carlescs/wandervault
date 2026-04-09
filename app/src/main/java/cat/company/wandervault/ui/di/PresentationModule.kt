package cat.company.wandervault.ui.di

import cat.company.wandervault.ui.screens.ArchiveViewModel
import cat.company.wandervault.ui.screens.DataAdminViewModel
import cat.company.wandervault.ui.screens.DocumentChatViewModel
import cat.company.wandervault.ui.screens.DocumentInfoViewModel
import cat.company.wandervault.ui.screens.FavoritesViewModel
import cat.company.wandervault.ui.screens.HomeViewModel
import cat.company.wandervault.ui.screens.ItineraryViewModel
import cat.company.wandervault.ui.screens.LocationDetailViewModel
import cat.company.wandervault.ui.screens.SettingsViewModel
import cat.company.wandervault.ui.screens.ShareViewModel
import cat.company.wandervault.ui.screens.TransportDetailViewModel
import cat.company.wandervault.ui.screens.TripDetailViewModel
import cat.company.wandervault.ui.screens.TripDocumentsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel {
        HomeViewModel(
            getTrips = get(),
            saveTrip = get(),
            updateTrip = get(),
            copyImage = get(),
            deleteImage = get(),
            deleteTrip = get(),
            toggleFavorite = get(),
            archiveTrip = get(),
            unarchiveTrip = get(),
        )
    }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { ArchiveViewModel(get(), get(), get()) }
    viewModel { params ->
        TripDetailViewModel(
            tripId = params.get(),
            getTripUseCase = get(),
            getDestinationsForTripUseCase = get(),
            generateTripDescriptionUseCase = get(),
            saveTripDescriptionUseCase = get(),
            generateWhatsNextUseCase = get(),
            saveTripWhatsNextUseCase = get(),
            getDocumentById = get(),
        )
    }
    viewModel { params ->
        ItineraryViewModel(
            tripId = params.get(),
            getDestinationsForTrip = get(),
            saveDestination = get(),
            updateDestination = get(),
            deleteDestination = get(),
            deleteTransport = get(),
        )
    }
    viewModel {
        LocationDetailViewModel(
            getDestinationById = get(),
            getArrivalTransport = get(),
            getDestinationsForTrip = get(),
            getHotelForDestination = get(),
            saveHotel = get(),
            deleteHotel = get(),
            updateDestination = get(),
            getDocumentById = get(),
            getActivitiesForDestination = get(),
            saveActivity = get(),
            deleteActivity = get(),
        )
    }
    viewModel {
        TransportDetailViewModel(
            getDestinationById = get(),
            getOrCreateTransport = get(),
            getNextDestination = get(),
            saveTransportLeg = get(),
            updateTransportLeg = get(),
            deleteTransportLeg = get(),
            deleteTransport = get(),
            updateDestination = get(),
            getDocumentById = get(),
        )
    }
    viewModel { DataAdminViewModel(androidApplication(), get(), get()) }
    viewModel { FavoritesViewModel(get(), get()) }
    viewModel { params ->
        DocumentChatViewModel(
            documentId = params.get(),
            getDocumentById = get(),
            askDocumentQuestion = get(),
        )
    }
    viewModel { params ->
        DocumentInfoViewModel(
            documentId = params.get(),
            getDocumentById = get(),
            getAllFoldersForTrip = get(),
            summarizeDocument = get(),
            updateDocument = get(),
            getTrip = get(),
            saveTripDescription = get(),
            getDestinationsForTrip = get(),
            getHotelForDestination = get(),
            getHotelsForDestinations = get(),
            saveHotel = get(),
            updateTransportLeg = get(),
            updateDestination = get(),
            getOrCreateTransport = get(),
            saveTransportLeg = get(),
            saveActivity = get(),
        )
    }
    viewModel { params ->
        TripDocumentsViewModel(
            tripId = params.get(),
            getRootFolders = get(),
            getSubFolders = get(),
            getDocumentsInFolder = get(),
            getRootDocuments = get(),
            saveFolder = get(),
            updateFolder = get(),
            deleteFolder = get(),
            saveDocument = get(),
            updateDocument = get(),
            deleteDocument = get(),
            copyDocumentToInternalStorage = get(),
            getAllFoldersForTrip = get(),
            getAllDocumentsForTrip = get(),
            suggestDocumentName = get(),
            autoOrganizeDocuments = get(),
        )
    }
    viewModel { params ->
        ShareViewModel(
            sourceUri = params.component1(),
            mimeType = params.component2(),
            documentName = params.component3(),
            getTrips = get(),
            copyDocumentToInternalStorage = get(),
            saveDocument = get(),
            updateDocument = get(),
            getRootDocuments = get(),
            summarizeDocument = get(),
            getTrip = get(),
            saveTripDescription = get(),
            getDestinationsForTrip = get(),
            getHotelForDestination = get(),
            saveHotel = get(),
            saveActivity = get(),
            getOrCreateTransport = get(),
            saveTransportLeg = get(),
            updateTransportLeg = get(),
            updateDestination = get(),
        )
    }
}
