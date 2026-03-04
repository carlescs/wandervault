package cat.company.wandervault.ui.di

import cat.company.wandervault.ui.screens.HomeViewModel
import cat.company.wandervault.ui.screens.ItineraryViewModel
import cat.company.wandervault.ui.screens.TripDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { params -> TripDetailViewModel(params.get(), get()) }
    viewModel { params -> ItineraryViewModel(params.get(), get(), get(), get(), get()) }
}
