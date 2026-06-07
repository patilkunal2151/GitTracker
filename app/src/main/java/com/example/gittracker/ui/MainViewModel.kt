package com.example.gittracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gittracker.data.model.ReleaseEntity
import com.example.gittracker.data.model.TrackedRepository
import com.example.gittracker.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _isAdding = MutableStateFlow(false)
    val isAdding = _isAdding.asStateFlow()

    private val _errorEvent = Channel<String>(Channel.BUFFERED)
    val errorEvent: Flow<String> = _errorEvent.receiveAsFlow()

    private val _successEvent = Channel<String>(Channel.BUFFERED)
    val successEvent: Flow<String> = _successEvent.receiveAsFlow()

    private val _undoDeleteEvent = Channel<Pair<TrackedRepository, List<ReleaseEntity>>>(Channel.BUFFERED)
    val undoDeleteEvent: Flow<Pair<TrackedRepository, List<ReleaseEntity>>> = _undoDeleteEvent.receiveAsFlow()

    val uiState: StateFlow<List<TrackedRepository>> = repository.getAllTrackedRepositories()
        .map { repos ->
            repos.sortedWith(
                compareByDescending<TrackedRepository> { it.isPinned }
                    .thenByDescending { it.hasNewUpdate }
                    .thenBy { it.name.ifBlank { it.repoName }.lowercase() }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getReleases(repoId: Long): Flow<List<ReleaseEntity>> = 
        repository.getReleasesForRepository(repoId)

    fun togglePin(repo: TrackedRepository) {
        viewModelScope.launch {
            repository.updateRepository(repo.copy(isPinned = !repo.isPinned))
        }
    }

    fun addRepo(url: String) {
        viewModelScope.launch {
            _isAdding.value = true
            try {
                repository.addRepository(url)
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

    fun deleteRepo(repo: TrackedRepository) {
        viewModelScope.launch {
            val releases = repository.getReleasesSync(repo.id)
            repository.deleteRepository(repo)
            _undoDeleteEvent.send(repo to releases)
        }
    }

    fun restoreRepo(repo: TrackedRepository, releases: List<ReleaseEntity>) {
        viewModelScope.launch {
            repository.restoreRepository(repo, releases)
        }
    }

    fun markAsRead(repo: TrackedRepository) {
        viewModelScope.launch {
            repository.markAsRead(repo)
        }
    }

    fun updateRepoName(repo: TrackedRepository, name: String) {
        viewModelScope.launch {
            repository.updateRepository(repo.copy(name = name))
        }
    }
}
