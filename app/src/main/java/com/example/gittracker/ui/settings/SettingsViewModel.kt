package com.example.gittracker.ui.settings

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.gittracker.domain.usecase.ExportRepositoriesUseCase
import com.example.gittracker.domain.usecase.ImportRepositoriesUseCase
import com.example.gittracker.worker.WorkManagerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val isDetour: Boolean = false,
    val isBatteryOptimized: Boolean = true,
    val isTrackingSelf: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduler: WorkManagerScheduler,
    private val settingsManager: com.example.gittracker.data.local.SettingsManager,
    private val toggleTrackSelfUseCase: com.example.gittracker.domain.usecase.ToggleTrackSelfUseCase,
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
        ticker,
        settingsManager.isTrackingSelf
    ) { status, currentTime, isTrackingSelf ->
        var isSyncing = false
        var isDetour = false
        val countdown = if (status != null) {
            val nextTime = status.nextTime
            val state = status.state
            isDetour = status.isDetour
            
            if (state == WorkInfo.State.RUNNING) {
                isSyncing = true
            }

            if (nextTime == Long.MAX_VALUE || nextTime == 0L) {
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

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isOptimized = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            false
        }

        SettingsUiState(
            countdown, 
            isSyncing, 
            isDetour, 
            isBatteryOptimized = isOptimized,
            isTrackingSelf = isTrackingSelf
        )
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

    fun toggleTrackSelf(enabled: Boolean) {
        viewModelScope.launch {
            try {
                toggleTrackSelfUseCase(enabled)
                val message = if (enabled) "Tracking Git Tracker" else "Untracked Git Tracker"
                _messageEvent.send(message)
            } catch (e: Exception) {
                _messageEvent.send("Failed to update tracking")
            }
        }
    }

    fun notifyMessage(message: String) {
        viewModelScope.launch {
            _messageEvent.send(message)
        }
    }
}
