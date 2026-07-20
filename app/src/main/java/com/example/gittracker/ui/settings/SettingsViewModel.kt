package com.example.gittracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.gittracker.domain.usecase.ExportRepositoriesUseCase
import com.example.gittracker.domain.usecase.ImportRepositoriesUseCase
import com.example.gittracker.worker.WorkManagerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class SettingsUiState(
    val nextSyncCountdown: String? = null,
    val isSyncing: Boolean = false,
    val isDetour: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val scheduler: WorkManagerScheduler,
    private val exportRepositoriesUseCase: ExportRepositoriesUseCase,
    private val importRepositoriesUseCase: ImportRepositoriesUseCase
) : ViewModel() {

    private val _exportEvent = Channel<String>(Channel.BUFFERED)
    val exportEvent: Flow<String> = _exportEvent.receiveAsFlow()

    private val _messageEvent = Channel<String>(Channel.BUFFERED)
    val messageEvent: Flow<String> = _messageEvent.receiveAsFlow()

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1.seconds)
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        scheduler.workStatus,
        ticker
    ) { status, currentTime ->
        var isSyncing = false
        var isDetour = false
        val countdown = if (status != null) {
            val nextTime = status.nextTime
            val state = status.state
            isDetour = status.isDetour
            
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
                    "Pending..."
                }
            }
        } else {
            null
        }
        SettingsUiState(countdown, isSyncing, isDetour)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun exportRepositories() {
        viewModelScope.launch {
            val json = exportRepositoriesUseCase()
            _exportEvent.send(json)
        }
    }

    fun importRepositories(json: String) {
        viewModelScope.launch {
            try {
                importRepositoriesUseCase(json)
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
