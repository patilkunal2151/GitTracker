package com.example.gittracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
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
    val nextSyncCountdown: String? = null
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
        scheduler.nextCheckTime,
        ticker
    ) { frequency, nextTime, currentTime ->
        val countdown = if (nextTime != null) {
            val diff = nextTime - currentTime
            if (diff > 0) {
                val hours = (diff / (1000 * 60 * 60)) % 24
                val minutes = (diff / (1000 * 60)) % 60
                val seconds = (diff / 1000) % 60
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                "Syncing..."
            }
        } else {
            null
        }
        SettingsUiState(frequency, countdown)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setSyncFrequency(hours: Int) {
        viewModelScope.launch {
            settingsManager.setSyncFrequency(hours)
            scheduler.schedulePeriodicUpdateCheck(ExistingPeriodicWorkPolicy.UPDATE)
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
