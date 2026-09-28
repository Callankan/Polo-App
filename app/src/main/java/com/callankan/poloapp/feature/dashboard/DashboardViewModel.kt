package com.callankan.poloapp.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callankan.poloapp.data.repository.ActiveVehicle
import com.callankan.poloapp.data.repository.OverviewRepository
import com.callankan.poloapp.data.repository.VehicleOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    active: ActiveVehicle,
    overviewRepository: OverviewRepository,
) : ViewModel() {
    val overview: StateFlow<VehicleOverview?> = active.id
        .flatMapLatest { overviewRepository.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
