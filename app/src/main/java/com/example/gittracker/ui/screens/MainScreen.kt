package com.example.gittracker.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.gittracker.R
import com.example.gittracker.data.model.TrackedRepository
import com.example.gittracker.ui.components.AddRepoDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repositories: List<TrackedRepository>,
    isAdding: Boolean,
    snackbarHost: @Composable () -> Unit,
    onRepoClick: (TrackedRepository) -> Unit,
    onAddRepo: (String) -> Unit,
    onDeleteRepo: (TrackedRepository) -> Unit,
    onTogglePin: (TrackedRepository) -> Unit,
    onUpdateName: (TrackedRepository, String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    
    val listState = rememberLazyListState()
    var previousRepoIds by remember { mutableStateOf(repositories.map { it.id }.toSet()) }
    val pinnedRepos = remember(repositories) { repositories.filter { it.isPinned } }
    val otherRepos = remember(repositories) { repositories.filter { !it.isPinned } }
    
    LaunchedEffect(repositories) {
        val currentIds = repositories.map { it.id }.toSet()
        val newId = (currentIds - previousRepoIds).firstOrNull()
        
        if (newId != null) {
            val pinnedIndex = pinnedRepos.indexOfFirst { it.id == newId }
            if (pinnedIndex != -1) {
                // It's in pinned. Index 0 is header, so scroll to pinnedIndex + 1
                listState.animateScrollToItem(pinnedIndex + 1)
            } else {
                val otherIndex = otherRepos.indexOfFirst { it.id == newId }
                if (otherIndex != -1) {
                    val baseIndex = if (pinnedRepos.isNotEmpty()) {
                        1 + pinnedRepos.size + 1 // Pinned Header + Pinned Items + Repositories Header
                    } else {
                        0 // No headers before other repos if pinned is empty (wait, is there a header if pinned is empty?)
                    }
                    // Looking at the code below, if pinnedRepos is empty, it just shows otherRepos items directly (no header)
                    listState.animateScrollToItem(baseIndex + otherIndex)
                }
            }
        }
        previousRepoIds = currentIds
    }

    Box(modifier = modifier.fillMaxSize()) {
        GradientBlurBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost,
            topBar = {
                TopAppBar(
                    title = { 
                        if (isSelectionMode) {
                            Text("${selectedIds.size} selected")
                        } else {
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "monitor git releases.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
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
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    actions = {
                        if (isSelectionMode) {
                            IconButton(onClick = {
                                repositories.filter { it.id in selectedIds }.forEach { onDeleteRepo(it) }
                                selectedIds = emptySet()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                            }
                        } else {
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
                                        contentDescription = stringResource(R.string.add_repository),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
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
                if (repositories.isEmpty() && !isAdding) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pinnedRepos.isNotEmpty()) {
                            item(key = "pinned_header") {
                                SectionHeader(
                                    title = "Pinned",
                                    modifier = Modifier.animateItem()
                                )
                            }
                            items(pinnedRepos, key = { it.id }) { repo ->
                                RepoItem(
                                    modifier = Modifier.animateItem(),
                                    repo = repo,
                                    isSelected = repo.id in selectedIds,
                                    onToggleSelection = {
                                        selectedIds = if (repo.id in selectedIds) {
                                            selectedIds - repo.id
                                        } else {
                                            selectedIds + repo.id
                                        }
                                    },
                                    onClick = { 
                                        if (isSelectionMode) {
                                            selectedIds = if (repo.id in selectedIds) {
                                                selectedIds - repo.id
                                            } else {
                                                selectedIds + repo.id
                                            }
                                        } else {
                                            onRepoClick(repo)
                                        }
                                    },
                                    onDeleteRepo = onDeleteRepo,
                                    onTogglePin = onTogglePin,
                                    onUpdateName = onUpdateName
                                )
                            }
                            if (otherRepos.isNotEmpty()) {
                                item(key = "all_header") {
                                    SectionHeader(
                                        title = "Repositories",
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }

                        items(otherRepos, key = { it.id }) { repo ->
                            RepoItem(
                                modifier = Modifier.animateItem(),
                                repo = repo,
                                isSelected = repo.id in selectedIds,
                                onToggleSelection = {
                                    selectedIds = if (repo.id in selectedIds) {
                                        selectedIds - repo.id
                                    } else {
                                        selectedIds + repo.id
                                    }
                                },
                                onClick = { 
                                    if (isSelectionMode) {
                                        selectedIds = if (repo.id in selectedIds) {
                                            selectedIds - repo.id
                                        } else {
                                            selectedIds + repo.id
                                        }
                                    } else {
                                        onRepoClick(repo)
                                    }
                                },
                                onDeleteRepo = onDeleteRepo,
                                onTogglePin = onTogglePin,
                                onUpdateName = onUpdateName
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
        }
    }
}

@Composable
fun GradientBlurBackground() {
    val isDark = isSystemInDarkTheme()
    val brandPurple = Color(0xFF6750A4)
    val cornerColor = if (isDark) Color.Black else Color.White

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Themed radial gradient from the top right corner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            cornerColor.copy(alpha = if (isDark) 0.8f else 0.9f),
                            brandPurple.copy(alpha = if (isDark) 0.3f else 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(1000f, -200f),
                        radius = 2500f
                    )
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepoItem(
    repo: TrackedRepository,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit,
    onDeleteRepo: (TrackedRepository) -> Unit,
    onTogglePin: (TrackedRepository) -> Unit,
    onUpdateName: (TrackedRepository, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current
    val cardColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onToggleSelection
            ),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        border = when {
            isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            repo.hasNewUpdate -> BorderStroke(2.dp, Color(0xFF4CAF50))
            else -> BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
        },
        color = cardColor
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
                var isEditing by remember { mutableStateOf(false) }
                var menuExpanded by remember { mutableStateOf(false) }
                var nameText by remember(repo.id, repo.name) { mutableStateOf(repo.name) }
                val focusRequester = remember { FocusRequester() }

                if (isEditing) {
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text(repo.repoName) },
                        textStyle = MaterialTheme.typography.titleMedium,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                onUpdateName(repo, nameText)
                                isEditing = false
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = repo.name.ifBlank { repo.repoName },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        
                        // Fixed-size container for trailing actions to prevent dimension shifts
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(32.dp) 
                        ) {
                            if (!isSelected) {
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(if (repo.isPinned) "Unpin" else "Pin to Top") },
                                            onClick = {
                                                onTogglePin(repo)
                                                menuExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    if (repo.isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Edit Name") },
                                            onClick = {
                                                isEditing = true
                                                menuExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Share") },
                                            onClick = {
                                                val sendIntent: Intent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "Check out this repository: https://github.com/${repo.owner}/${repo.repoName}")
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, null)
                                                context.startActivity(shareIntent)
                                                menuExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Share,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                onDeleteRepo(repo)
                                                menuExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp).padding(4.dp) // Padded to align with 32dp container
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${repo.owner}/${repo.repoName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    ) {
                        Text(
                            text = repo.latestVersionTag,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (repo.hasNewUpdate) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "Update Available",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
    )
}

