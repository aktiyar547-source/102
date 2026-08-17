package com.middleeastcontainer.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.middleeastcontainer.data.export.SweepExporter
import com.middleeastcontainer.domain.model.Sweep
import com.middleeastcontainer.domain.repository.InventoryRepository
import com.middleeastcontainer.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Outcome of writing a sheet to the phone. */
data class ExportedSheet(
    val uri: Uri?,
    val fileName: String?,
    val rows: Int,
    val error: String?,
)

/** Lists past sweeps and starts new ones. */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val exporter: SweepExporter,
) : ViewModel() {

    /** Result of the last export, so the UI can offer to open or share it. */
    private val _lastExport = MutableStateFlow<ExportedSheet?>(null)
    val lastExport = _lastExport.asStateFlow()

    fun dismissExport() { _lastExport.value = null }

    /**
     * Writes the sheet to the phone. Deliberately independent of the server: a
     * yard often has no signal, and the count is wanted the moment the walk ends.
     */
    fun exportToPhone(sweep: Sweep) {
        viewModelScope.launch {
            runCatching {
                val units = repository.sightingsOnce(sweep.id)
                val gaps = repository.unreadOnce(sweep.id)
                exporter.export(sweep, units, gaps)
            }.onSuccess { result ->
                _lastExport.value = ExportedSheet(
                    uri = result.uri, fileName = result.fileName,
                    rows = result.rows, error = null,
                )
            }.onFailure { e ->
                Timber.e(e, "Export failed")
                _lastExport.value = ExportedSheet(
                    uri = null, fileName = null, rows = 0,
                    error = e.message ?: "Could not write the file",
                )
            }
        }
    }

    val state = repository.observeSweeps()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Content(it) as UiState<List<Sweep>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun startSweep(zone: String, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            onStarted(repository.startSweep(zone))
        }
    }

    fun delete(sweep: Sweep) {
        viewModelScope.launch { repository.deleteSweep(sweep.id) }
    }
}
