package com.example.gittracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo
import com.example.gittracker.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getTrackedRepositoriesUseCase: GetTrackedRepositoriesUseCase,
    private val addRepositoryUseCase: AddRepositoryUseCase,
    private val deleteRepositoryUseCase: DeleteRepositoryUseCase,
    private val togglePinUseCase: TogglePinUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val updateRepositoryNameUseCase: UpdateRepositoryNameUseCase,
    private val restoreRepositoryUseCase: RestoreRepositoryUseCase,
    private val fetchMoreReleasesUseCase: FetchMoreReleasesUseCase,
    private val getReleasesUseCase: GetReleasesUseCase
) : ViewModel() {

    private val _isAdding = MutableStateFlow(false)
    val isAdding = _isAdding.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private val _errorEvent = Channel<String>(Channel.BUFFERED)
    val errorEvent: Flow<String> = _errorEvent.receiveAsFlow()

    private val _successEvent = Channel<String>(Channel.BUFFERED)
    val successEvent: Flow<String> = _successEvent.receiveAsFlow()

    private val _undoDeleteEvent = Channel<Pair<TrackedRepo, List<Release>>>(Channel.BUFFERED)
    val undoDeleteEvent: Flow<Pair<TrackedRepo, List<Release>>> = _undoDeleteEvent.receiveAsFlow()

    val uiState: StateFlow<List<TrackedRepo>> = getTrackedRepositoriesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getReleases(repoId: Long): Flow<List<Release>> = getReleasesUseCase(repoId)

    fun loadMoreReleases(repoId: Long) {
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                fetchMoreReleasesUseCase(repoId)
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 403) {
                    _errorEvent.send("GitHub Rate Limit Reached. Try again later.")
                } else {
                    _errorEvent.send("Server error: ${e.code()}")
                }
            } catch (e: Exception) {
                _errorEvent.send("Failed to load more releases")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun togglePin(repo: TrackedRepo) {
        viewModelScope.launch {
            togglePinUseCase(repo)
        }
    }

    fun addRepo(url: String) {
        viewModelScope.launch {
            _isAdding.value = true
            try {
                addRepositoryUseCase(url)
                _successEvent.send("Repository added successfully")
            } catch (e: IllegalStateException) {
                _errorEvent.send(e.message ?: "Unknown error")
            } catch (e: Exception) {
                _errorEvent.send("Failed to add repository")
                e.printStackTrace()
            } finally {
                _isAdding.value = false
            }
        }
    }

    fun deleteRepo(repo: TrackedRepo) {
        viewModelScope.launch {
            val result = deleteRepositoryUseCase(repo)
            _undoDeleteEvent.send(result)
        }
    }

    fun restoreRepo(repo: TrackedRepo, releases: List<Release>) {
        viewModelScope.launch {
            restoreRepositoryUseCase(repo, releases)
        }
    }

    fun markAsRead(repo: TrackedRepo) {
        viewModelScope.launch {
            markAsReadUseCase(repo)
        }
    }

    fun updateRepoName(repo: TrackedRepo, name: String) {
        viewModelScope.launch {
            updateRepositoryNameUseCase(repo, name)
        }
    }
}
