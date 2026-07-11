package com.example.gittracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkInfo
import com.example.gittracker.data.local.SettingsManager
import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.worker.WorkManagerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class SettingsUiState(
    val syncFrequency: Int = 8,
    val nextSyncCountdown: String? = null,
    val isSyncing: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val scheduler: WorkManagerScheduler,
    private val repository: AppRepository
) : ViewModel() {

    private val _exportEvent = Channel<String>(Channel.BUFFERED)
    val exportEvent: Flow<String> = _exportEvent.receiveAsFlow()

    private val _messageEvent = Channel<String>(Channel.BUFFERED)
    val messageEvent: Flow<String> = _messageEvent.receiveAsFlow()

    // Ticker flow that emits every second
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1.seconds)
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsManager.syncFrequency,
        scheduler.workStatus,
        ticker
    ) { frequency, status, currentTime ->
        var isSyncing = false
        val countdown = if (status != null) {
            val nextTime = status.nextTime
            val state = status.state
            
            if (state == WorkInfo.State.RUNNING) {
                isSyncing = true
                "Syncing..."
            } else if (nextTime == Long.MAX_VALUE) {
                "Calculating..."
            } else {
                val diff = nextTime - currentTime
                if (diff > 0) {
                    val hours = (diff / (1000 * 60 * 60))
                    val minutes = (diff / (1000 * 60)) % 60
                    val seconds = (diff / 1000) % 60
                    String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    // Time passed but hasn't started RUNNING yet (system delay)
                    "Pending..."
                }
            }
        } else {
            null
        }
        SettingsUiState(frequency, countdown, isSyncing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setSyncFrequency(hours: Int) {
        viewModelScope.launch {
            settingsManager.setSyncFrequency(hours)
            // Use CANCEL_AND_REENQUEUE to restart the timer with the new frequency immediately
            scheduler.scheduleUpdateCheck(ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, overrideHours = hours)
        }
    }

    fun exportRepositories() {
        viewModelScope.launch {
            val json = repository.exportRepositories()
            _exportEvent.send(json)
        }
    }

    fun importRepositories(json: String) {
        viewModelScope.launch {
            try {
                repository.importRepositories(json)
                _messageEvent.send("Import completed successfully")
            } catch (e: Exception) {
                _messageEvent.send("Failed to import repositories")
            }
        }
    }

    fun notifyMessage(message: String) {
        viewModelScope.launch {
            _messageEvent.send(message)
        }
    }
}
