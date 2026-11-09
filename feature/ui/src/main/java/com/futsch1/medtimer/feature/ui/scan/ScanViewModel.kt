package com.futsch1.medtimer.feature.ui.scan

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import com.futsch1.medtimer.core.ui.R as CoreUiR

sealed interface ScanUiState {
    data object Scanning : ScanUiState
    data class Found(val outcome: ScanOutcome.Known) : ScanUiState
    data class Unassigned(val outcome: ScanOutcome.Unknown, val medicines: List<Medicine>) : ScanUiState
    data class Done(@field:StringRes val messageRes: Int) : ScanUiState
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val handler: ScanHandler,
    private val medicineRepository: MedicineRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    // Camera frames arrive on the analysis thread; only one code is looked up until the user has answered.
    private val busy = AtomicBoolean(false)

    fun onRaw(raw: String) {
        if (!busy.compareAndSet(false, true)) {
            return
        }
        viewModelScope.launch {
            when (val outcome = handler.lookup(raw)) {
                ScanOutcome.NotAPackage -> busy.set(false)
                is ScanOutcome.Known -> _state.value = ScanUiState.Found(outcome)
                is ScanOutcome.Unknown -> _state.value = ScanUiState.Unassigned(outcome, medicineRepository.getAll())
            }
        }
    }

    fun confirmRefill(takeExpiry: Boolean) {
        val found = _state.value as? ScanUiState.Found ?: return
        viewModelScope.launch {
            handler.refill(found.outcome.medicine.id, found.outcome.code.expiry.takeIf { takeExpiry })
            _state.value = ScanUiState.Done(CoreUiR.string.scan_refilled)
        }
    }

    fun assignTo(medicineId: Int) {
        val unassigned = _state.value as? ScanUiState.Unassigned ?: return
        viewModelScope.launch {
            handler.assign(medicineId, unassigned.outcome.code.pzn)
            _state.value = ScanUiState.Done(CoreUiR.string.scan_assigned)
        }
    }

    fun dismiss() {
        _state.value = ScanUiState.Scanning
        busy.set(false)
    }
}
