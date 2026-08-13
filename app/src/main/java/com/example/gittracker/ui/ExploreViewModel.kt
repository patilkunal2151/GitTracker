package com.example.gittracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gittracker.data.model.GitHubRepo
import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.domain.model.TrackedRepo
import com.example.gittracker.domain.usecase.GetTrackedRepositoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class SearchRepo(
    val owner: String,
    val name: String,
    val fullName: String,
    val description: String?,
    val stargazersCount: Int,
    val language: String?,
    val isLocal: Boolean
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: AppRepository,
    private val getTrackedRepositoriesUseCase: GetTrackedRepositoriesUseCase
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<SearchRepo>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _showGitHubSearchPrompt = MutableStateFlow(false)
    val showGitHubSearchPrompt = _showGitHubSearchPrompt.asStateFlow()

    private var allLocalRepos: List<TrackedRepo> = emptyList()
    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            getTrackedRepositoriesUseCase().collect {
                allLocalRepos = it
            }
        }

        viewModelScope.launch {
            _searchQuery
                .debounce(300.milliseconds)
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _showGitHubSearchPrompt.value = false
            return
        }

        val localResults = allLocalRepos.filter {
            it.repoName.contains(query, ignoreCase = true) || 
            it.owner.contains(query, ignoreCase = true) ||
            it.name.contains(query, ignoreCase = true)
        }.map { it.toSearchRepo() }

        _searchResults.value = localResults
        _showGitHubSearchPrompt.value = localResults.isEmpty()
    }

    fun searchGitHub(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val remoteResults = repository.searchRepositories(query).map { it.toSearchRepo() }
            _searchResults.value = remoteResults
            _showGitHubSearchPrompt.value = false
            _isSearching.value = false
        }
    }

    private fun TrackedRepo.toSearchRepo() = SearchRepo(
        owner = owner,
        name = repoName,
        fullName = "$owner/$repoName",
        description = description,
        stargazersCount = stargazersCount,
        language = language,
        isLocal = true
    )

    private fun GitHubRepo.toSearchRepo() = SearchRepo(
        owner = owner.login,
        name = name,
        fullName = fullName,
        description = description,
        stargazersCount = stargazersCount,
        language = language,
        isLocal = false
    )
}
