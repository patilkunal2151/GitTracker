package com.example.gittracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.gittracker.R
import com.example.gittracker.domain.model.TrackedRepo
import com.example.gittracker.ui.components.AddRepoDialog
import com.example.gittracker.ui.SearchRepo
import com.example.gittracker.util.UiUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repositories: List<TrackedRepo>,
    searchResults: List<SearchRepo>,
    isSearchingRemote: Boolean,
    showGitHubPrompt: Boolean,
    isAdding: Boolean,
    snackbarHost: @Composable () -> Unit,
    onRepoClick: (TrackedRepo) -> Unit,
    onAddRepo: (String) -> Unit,
    onDeleteRepo: (TrackedRepo) -> Unit,
    onTogglePin: (TrackedRepo) -> Unit,
    onUpdateName: (TrackedRepo, String) -> Unit,
    onSearch: (String) -> Unit,
    onSearchGitHub: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedRepoForActions by remember { mutableStateOf<TrackedRepo?>(null) }
    var repoIdToRename by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    val listState = rememberLazyListState()
    val pinnedRepos = remember(repositories) { repositories.filter { it.isPinned } }
    val otherRepos = remember(repositories) { repositories.filter { !it.isPinned } }

    LaunchedEffect(pinnedRepos.size) {
        if (pinnedRepos.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = snackbarHost,
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text("${selectedIds.size} selected")
                    } else if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                onSearch(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            placeholder = { Text("Search...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    isSearchActive = false
                                    searchQuery = ""
                                    onSearch("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            }
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            repositories.filter { it.id in selectedIds }.forEach { onDeleteRepo(it) }
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    } else if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showDialog = true }) {
                            if (isAdding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_repository)
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isSearchActive && searchQuery.isNotEmpty()) {
                if (isSearchingRemote) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                } else if (showGitHubPrompt) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No local results found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onSearchGitHub(searchQuery) }) {
                            Text("Search on GitHub")
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No repositories found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(searchResults) { repo ->
                            ExploreRepoItem(repo = repo, onClick = { 
                                if (repo.isLocal) {
                                    repositories.find { it.owner == repo.owner && it.repoName == repo.name }?.let { onRepoClick(it) }
                                } else {
                                    onAddRepo("https://github.com/${repo.fullName}")
                                }
                                isSearchActive = false
                            })
                        }
                    }
                }
            } else if (repositories.isEmpty() && !isAdding) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_repo),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_repositories),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (pinnedRepos.isNotEmpty()) {
                        item(key = "pinned_header") {
                            MainSectionHeader(title = "Pinned")
                        }
                        items(pinnedRepos, key = { it.id }) { repo ->
                            RepoItem(
                                repo = repo,
                                isSelected = repo.id in selectedIds,
                                isRenaming = repoIdToRename == repo.id,
                                onToggleSelection = {
                                    selectedIds = if (repo.id in selectedIds) selectedIds - repo.id else selectedIds + repo.id
                                },
                                onClick = { 
                                    if (isSelectionMode) {
                                        selectedIds = if (repo.id in selectedIds) selectedIds - repo.id else selectedIds + repo.id
                                    } else {
                                        onRepoClick(repo)
                                    }
                                },
                                onShowActions = {
                                    selectedRepoForActions = it
                                    showBottomSheet = true
                                },
                                onUpdateName = { r, n ->
                                    onUpdateName(r, n)
                                    repoIdToRename = null
                                },
                                onCancelRename = { repoIdToRename = null }
                            )
                        }
                        item(key = "all_header") {
                            MainSectionHeader(title = "Repositories")
                        }
                    }

                    items(otherRepos, key = { it.id }) { repo ->
                        RepoItem(
                            repo = repo,
                            isSelected = repo.id in selectedIds,
                            isRenaming = repoIdToRename == repo.id,
                            onToggleSelection = {
                                selectedIds = if (repo.id in selectedIds) selectedIds - repo.id else selectedIds + repo.id
                            },
                            onClick = { 
                                if (isSelectionMode) {
                                    selectedIds = if (repo.id in selectedIds) selectedIds - repo.id else selectedIds + repo.id
                                } else {
                                    onRepoClick(repo)
                                }
                            },
                            onShowActions = {
                                selectedRepoForActions = it
                                showBottomSheet = true
                            },
                            onUpdateName = { r, n ->
                                onUpdateName(r, n)
                                repoIdToRename = null
                            },
                            onCancelRename = { repoIdToRename = null }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AddRepoDialog(
                onDismiss = { showDialog = false },
                onConfirm = { url ->
                    onAddRepo(url)
                    showDialog = false
                }
            )
        }
        
        if (showBottomSheet && selectedRepoForActions != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showBottomSheet = false
                    selectedRepoForActions = null
                },
                sheetState = sheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                RepoActionBottomSheetContent(
                    repo = selectedRepoForActions!!,
                    onAction = { repoAction ->
                        showBottomSheet = false
                        when (repoAction) {
                            is RepoAction.Pin -> onTogglePin(selectedRepoForActions!!)
                            is RepoAction.Rename -> {
                                repoIdToRename = selectedRepoForActions!!.id
                            }
                            is RepoAction.Share -> {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "https://github.com/${selectedRepoForActions!!.owner}/${selectedRepoForActions!!.repoName}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            }
                            is RepoAction.Delete -> onDeleteRepo(selectedRepoForActions!!)
                        }
                        selectedRepoForActions = null
                    }
                )
            }
        }
    }
}

sealed class RepoAction {
    object Pin : RepoAction()
    object Rename : RepoAction()
    object Share : RepoAction()
    object Delete : RepoAction()
}

@Composable
fun RepoActionBottomSheetContent(
    repo: TrackedRepo,
    onAction: (RepoAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_repo),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = repo.name.ifBlank { repo.repoName },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        
        ListItem(
            headlineContent = { Text(if (repo.isPinned) "Unpin" else "Pin") },
            leadingContent = { 
                Icon(
                    if (repo.isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                ) 
            },
            modifier = Modifier.clickable { onAction(RepoAction.Pin) }
        )

        ListItem(
            headlineContent = { Text("Rename") },
            leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.clickable { onAction(RepoAction.Rename) }
        )
        
        ListItem(
            headlineContent = { Text("Share") },
            leadingContent = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.clickable { onAction(RepoAction.Share) }
        )
        
        ListItem(
            headlineContent = { 
                Text(
                    text = "Delete", 
                    color = MaterialTheme.colorScheme.error 
                ) 
            },
            leadingContent = { 
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                ) 
            },
            modifier = Modifier.clickable { onAction(RepoAction.Delete) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepoItem(
    repo: TrackedRepo,
    isSelected: Boolean,
    isRenaming: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit,
    onShowActions: (TrackedRepo) -> Unit,
    onUpdateName: (TrackedRepo, String) -> Unit,
    onCancelRename: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onToggleSelection
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            var nameText by remember(repo.id, repo.name) { mutableStateOf(repo.name) }
            val focusRequester = remember { FocusRequester() }

            if (isRenaming) {
                LaunchedEffect(Unit) {
                    var retries = 0
                    while (retries < 10) {
                        try {
                            focusRequester.requestFocus()
                            break
                        } catch (e: Exception) {
                            delay(100)
                            retries++
                        }
                    }
                }
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text(repo.repoName) },
                    singleLine = true,
                    trailingIcon = {
                        Row {
                            IconButton(onClick = onCancelRename) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                            IconButton(onClick = {
                                onUpdateName(repo, nameText)
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_repo),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = repo.name.ifBlank { repo.repoName },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            overflow = TextOverflow.Clip
                        )
                    }
                    
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isSelected) {
                            IconButton(
                                onClick = { onShowActions(repo) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "${repo.owner}/${repo.repoName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!repo.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!repo.language.isNullOrBlank()) {
                    Surface(
                        shape = CircleShape,
                        color = UiUtils.getLanguageColor(repo.language),
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = repo.language,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Icon(
                    painter = painterResource(R.drawable.ic_star),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = UiUtils.formatCount(repo.stargazersCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    painter = painterResource(R.drawable.ic_fork),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = UiUtils.formatCount(repo.forksCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = repo.latestVersionTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                if (repo.hasNewUpdate) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A7F37).copy(alpha = 0.1f),
                        border = BorderStroke(0.5.dp, Color(0xFF1A7F37).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Update",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A7F37),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainSectionHeader(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
