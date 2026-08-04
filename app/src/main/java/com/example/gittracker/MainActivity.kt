package com.example.gittracker

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo
import com.example.gittracker.ui.MainViewModel
import com.example.gittracker.ui.ExploreViewModel
import com.example.gittracker.ui.components.StyledSnackbarHost
import com.example.gittracker.ui.screens.DetailScreen
import com.example.gittracker.ui.screens.MainScreen
import com.example.gittracker.ui.settings.SettingsScreen
import com.example.gittracker.ui.settings.SettingsViewModel
import com.example.gittracker.ui.theme.GitTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val deepLinkRepoId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        deepLinkRepoId.value = intent.getLongExtra("EXTRA_REPO_ID", -1L).takeIf { it != -1L }
        
        enableEdgeToEdge()
        setContent {
            GitTrackerTheme {
                AppNavigation(
                    repoId = deepLinkRepoId.value,
                    onDeepLinkConsumed = { deepLinkRepoId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkRepoId.value = intent.getLongExtra("EXTRA_REPO_ID", -1L).takeIf { it != -1L }
    }
}

enum class AppScreen { Home, Settings }

@Composable
fun AppNavigation(
    repoId: Long?,
    onDeepLinkConsumed: () -> Unit
) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val exploreViewModel: ExploreViewModel = hiltViewModel()
    
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedRepoIdForNavigation by remember { mutableStateOf<Long?>(null) }
    var homeResetSignal by remember { mutableLongStateOf(0L) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permissions
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Import/Export
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
                json?.let { 
                    currentScreen = AppScreen.Home
                    settingsViewModel.importRepositories(it) 
                }
            }
        }
    )

    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                pendingExportJson?.let { json ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            context.contentResolver.openOutputStream(it)?.use { output ->
                                output.write(json.toByteArray())
                            }
                            settingsViewModel.notifyMessage("Exported successfully")
                        } catch (e: Exception) {
                            settingsViewModel.notifyMessage("Failed to save export file")
                        }
                    }
                }
            }
            pendingExportJson = null
        }
    )

    LaunchedEffect(Unit) {
        settingsViewModel.exportEvent.collect { json ->
            pendingExportJson = json
            exportLauncher.launch("gittracker_export.json")
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.messageEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.errorEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.successEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.undoDeleteEvent.collect { (repo, releases) ->
            val result = snackbarHostState.showSnackbar(
                message = "Deleted ${repo.name.ifBlank { repo.repoName }}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                mainViewModel.restoreRepo(repo, releases)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val navBarColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Home && !isSearching,
                    onClick = { 
                        if (currentScreen == AppScreen.Home && !isSearching) {
                            homeResetSignal = System.currentTimeMillis()
                        }
                        currentScreen = AppScreen.Home
                        isSearching = false
                        selectedRepoIdForNavigation = null
                    },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") },
                    colors = navBarColors
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Settings,
                    onClick = { currentScreen = AppScreen.Settings },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Settings") },
                    colors = navBarColors
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { StyledSnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                AppScreen.Home -> {
                    val searchResults by exploreViewModel.searchResults.collectAsState()
                    val isSearchingRemote by exploreViewModel.isSearching.collectAsState()
                    val showGitHubPrompt by exploreViewModel.showGitHubSearchPrompt.collectAsState()
                    
                    GitTrackerApp(
                        viewModel = mainViewModel,
                        repoId = selectedRepoIdForNavigation ?: repoId,
                        searchResults = searchResults,
                        isSearchingRemote = isSearchingRemote,
                        showGitHubPrompt = showGitHubPrompt,
                        resetSignal = homeResetSignal,
                        onSearch = exploreViewModel::search,
                        onSearchGitHub = exploreViewModel::searchGitHub,
                        onShowSnackbar = { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        onDeepLinkConsumed = { 
                            selectedRepoIdForNavigation = null
                            onDeepLinkConsumed() 
                        }
                    )
                }
                AppScreen.Settings -> {
                    val state by settingsViewModel.uiState.collectAsState()
                    SettingsScreen(
                        state = state,
                        onExportClick = settingsViewModel::exportRepositories,
                        onImportClick = { importLauncher.launch("application/json") },
                        onBack = { currentScreen = AppScreen.Home },
                        snackbarHost = { /* Handled by Scaffold */ }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GitTrackerApp(
    viewModel: MainViewModel,
    repoId: Long? = null,
    searchResults: List<com.example.gittracker.ui.SearchRepo>,
    isSearchingRemote: Boolean,
    showGitHubPrompt: Boolean,
    resetSignal: Long = 0L,
    onSearch: (String) -> Unit,
    onSearchGitHub: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    onDeepLinkConsumed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdding by viewModel.isAdding.collectAsState()
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            while (navigator.canNavigateBack()) {
                navigator.navigateBack()
            }
        }
    }

    LaunchedEffect(repoId, uiState) {
        if (repoId != null) {
            val repo = uiState.find { it.id == repoId }
            if (repo != null) {
                if (repo.hasNewUpdate) {
                    viewModel.markAsRead(repo)
                }
                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, repoId)
                onDeepLinkConsumed()
            }
        }
    }

    BackHandler(navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            MainScreen(
                repositories = uiState,
                searchResults = searchResults,
                isSearchingRemote = isSearchingRemote,
                showGitHubPrompt = showGitHubPrompt,
                isAdding = isAdding,
                snackbarHost = { /* Handled by Scaffold */ },
                onRepoClick = { repo ->
                    viewModel.markAsRead(repo)
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, repo.id)
                    }
                },
                onAddRepo = { url -> viewModel.addRepo(url) },
                onDeleteRepo = { repo -> viewModel.deleteRepo(repo) },
                onTogglePin = { repo -> viewModel.togglePin(repo) },
                onUpdateName = { repo, name -> viewModel.updateRepoName(repo, name) },
                onSearch = onSearch,
                onSearchGitHub = onSearchGitHub
            )
        },
        detailPane = {
            val selectedId = navigator.currentDestination?.contentKey
            val selectedRepo = uiState.find { it.id == selectedId }
            val isLoadingMore by viewModel.isLoadingMore.collectAsState()
            val readme by viewModel.readme.collectAsState()

            val releases by (if (selectedId != null) {
                viewModel.getReleases(selectedId)
            } else {
                flowOf(emptyList())
            }).collectAsState(initial = emptyList())

            DetailScreen(
                repo = selectedRepo,
                releases = releases,
                readme = readme,
                isLoadingMore = isLoadingMore,
                onBack = {
                    scope.launch {
                        navigator.navigateBack()
                    }
                },
                onLoadMore = { id -> viewModel.loadMoreReleases(id) },
                onFetchReadme = {
                    selectedRepo?.let { viewModel.fetchReadme(it.owner, it.repoName) }
                },
                onShowSnackbar = onShowSnackbar
            )
        }
    )
}
