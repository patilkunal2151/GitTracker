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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo
import com.example.gittracker.ui.MainViewModel
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val deepLinkRepoId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        deepLinkRepoId.value = intent.getLongExtra("EXTRA_REPO_ID", -1L).takeIf { it != -1L }
        
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()

            GitTrackerTheme {
                GitTrackerAppWithPermissions(
                    repoId = deepLinkRepoId.value,
                    settingsViewModel = settingsViewModel,
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

@Composable
fun GitTrackerAppWithPermissions(
    repoId: Long?,
    settingsViewModel: SettingsViewModel,
    onDeepLinkConsumed: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
                json?.let { settingsViewModel.importRepositories(it) }
            }
        }
    )

    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                            e.printStackTrace()
                            settingsViewModel.notifyMessage("Failed to save export file")
                        }
                    }
                }
            }
            pendingExportJson = null
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.exportEvent.collect { json ->
            pendingExportJson = json
            exportLauncher.launch("gittracker_export.json")
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    val settingsSnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        settingsViewModel.messageEvent.collect { message ->
            settingsSnackbarHostState.showSnackbar(message)
        }
    }

    if (showSettings) {
        val state by settingsViewModel.uiState.collectAsState()
        SettingsScreen(
            state = state,
            onExportClick = settingsViewModel::exportRepositories,
            onImportClick = { importLauncher.launch("application/json") },
            onBack = { showSettings = false },
            snackbarHost = { StyledSnackbarHost(settingsSnackbarHostState) }
        )
        BackHandler {
            showSettings = false
        }
    } else {
        GitTrackerApp(
            repoId = repoId,
            onSettingsClick = { showSettings = true },
            onDeepLinkConsumed = onDeepLinkConsumed
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GitTrackerApp(
    viewModel: MainViewModel = hiltViewModel(),
    repoId: Long? = null,
    onSettingsClick: () -> Unit,
    onDeepLinkConsumed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdding by viewModel.isAdding.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.successEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.undoDeleteEvent.collect { (repo, releases) ->
            val result = snackbarHostState.showSnackbar(
                message = "Deleted ${repo.name.ifBlank { repo.repoName }}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreRepo(repo, releases)
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
                isAdding = isAdding,
                snackbarHost = { StyledSnackbarHost(snackbarHostState) },
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
                onSettingsClick = onSettingsClick
            )
        },
        detailPane = {
            val selectedId = navigator.currentDestination?.contentKey
            val selectedRepo = uiState.find { it.id == selectedId }
            val isLoadingMore by viewModel.isLoadingMore.collectAsState()

            val releases by (if (selectedId != null) {
                viewModel.getReleases(selectedId)
            } else {
                flowOf(emptyList())
            }).collectAsState(initial = emptyList())

            DetailScreen(
                repo = selectedRepo,
                releases = releases,
                isLoadingMore = isLoadingMore,
                onBack = {
                    scope.launch {
                        navigator.navigateBack()
                    }
                },
                onLoadMore = { id -> viewModel.loadMoreReleases(id) }
            )
        }
    )
}
