package com.nuvio.core.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UpdateUiEvent {
    data class ShowToast(val message: String) : UpdateUiEvent
    data class RequirePermission(val intent: android.content.Intent) : UpdateUiEvent
}

class UpdateViewModel(
    private val repository: UpdateRepository
) : ViewModel() {

    val updateState: StateFlow<UpdateState> = repository.updateState

    private val _events = MutableSharedFlow<UpdateUiEvent>()
    val events: SharedFlow<UpdateUiEvent> = _events.asSharedFlow()

    fun checkForUpdates(manual: Boolean = true, includePrerelease: Boolean = false) {
        viewModelScope.launch {
            repository.checkForUpdates(manual = manual, includePrerelease = includePrerelease)
        }
    }

    fun downloadUpdate() {
        val info = updateState.value.updateInfo ?: return
        viewModelScope.launch {
            repository.downloadUpdate(info)
        }
    }

    fun installUpdate() {
        when (val result = repository.installDownloadedUpdate()) {
            is InstallationResult.Launched -> {
                // System installer initiated
            }
            is InstallationResult.PermissionNeeded -> {
                viewModelScope.launch {
                    _events.emit(UpdateUiEvent.RequirePermission(result.settingsIntent))
                }
            }
            is InstallationResult.Error -> {
                viewModelScope.launch {
                    _events.emit(UpdateUiEvent.ShowToast(result.message))
                }
            }
        }
    }

    fun cancelDownload() {
        repository.cancelDownload()
    }

    fun dismissDialog() {
        repository.dismissUpdateDialog()
    }
}
