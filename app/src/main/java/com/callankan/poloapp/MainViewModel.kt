package com.callankan.poloapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callankan.poloapp.data.repository.VehicleRepository
import com.callankan.poloapp.data.settings.AppSettings
import com.callankan.poloapp.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainState {
    data object Loading : MainState
    data class Ready(val hasVehicle: Boolean, val settings: AppSettings) : MainState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settings: SettingsRepository,
    vehicles: VehicleRepository,
) : ViewModel() {
    val state: StateFlow<MainState> = combine(settings.settings, vehicles.observeAll()) { s, list ->
        // Si el vehículo seleccionado ya no existe, se elige el primero disponible.
        if (list.isNotEmpty() && list.none { it.id == s.selectedVehicleId }) {
            viewModelScope.launch { settings.selectVehicle(list.first().id) }
        }
        MainState.Ready(hasVehicle = list.isNotEmpty(), settings = s)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainState.Loading)
}
