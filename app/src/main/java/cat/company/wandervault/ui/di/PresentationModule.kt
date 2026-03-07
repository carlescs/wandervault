package cat.company.wandervault.ui.di

import cat.company.wandervault.ui.screens.DataAdminViewModel
import cat.company.wandervault.ui.screens.FavoritesViewModel
import cat.company.wandervault.ui.screens.HomeViewModel
import cat.company.wandervault.ui.screens.ItineraryViewModel
import cat.company.wandervault.ui.screens.LocationDetailViewModel
import cat.company.wandervault.ui.screens.TransportDetailViewModel
import cat.company.wandervault.ui.screens.TripDetailViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> TripDetailViewModel(params.get(), get(), get(), get(), get()) }
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
        )
    }
    viewModel { DataAdminViewModel(androidApplication(), get(), get()) }
    viewModel { FavoritesViewModel(get(), get()) }
}
