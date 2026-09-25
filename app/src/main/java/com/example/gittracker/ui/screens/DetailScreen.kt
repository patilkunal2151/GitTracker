package com.example.gittracker.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gittracker.R
import com.example.gittracker.data.model.ReleaseAsset
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo
import com.example.gittracker.util.DateUtils
import com.example.gittracker.util.DownloadUtils
import com.example.gittracker.util.UiUtils
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    repo: TrackedRepo?,
    releases: List<Release>,
    readme: String?,
    isLoadingMore: Boolean,
    onBack: () -> Unit,
    onLoadMore: (Long) -> Unit,
    onFetchReadme: (Long, String, String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Releases", "README")

    LaunchedEffect(repo?.id) {
        selectedTab = 0
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.release_history),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (repo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.select_repository),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header card
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    tonalElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_repo),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = repo.owner,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = " / ",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = repo.repoName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val repoUrl = "https://github.com/${repo.owner}/${repo.repoName}"
                        val context = LocalContext.current
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Surface(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl))
                                    context.startActivity(intent)
                                },
                                onLongClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("GitHub Link", repoUrl)
                                    clipboard.setPrimaryClip(clip)
                                    onShowSnackbar("Link copied to clipboard")
                                }
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = repoUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!repo.language.isNullOrBlank()) {
                                Surface(
                                    shape = CircleShape,
                                    color = UiUtils.getLanguageColor(repo.language),
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = repo.language,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Icon(
                                painter = painterResource(R.drawable.ic_star),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = UiUtils.formatCount(repo.stargazersCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            Icon(
                                painter = painterResource(R.drawable.ic_fork),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = UiUtils.formatCount(repo.forksCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { },
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 2.dp
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { 
                                selectedTab = index 
                                if (index == 1) repo?.let { onFetchReadme(it.id, it.owner, it.repoName) }
                            },
                            text = { 
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(releases, key = { it.id }) { release ->
                                ReleaseItem(release = release)
                            }
                            
                            if (!repo.reachedEndOfReleases) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoadingMore) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 3.dp
                                            )
                                        } else {
                                            TextButton(
                                                onClick = { onLoadMore(repo.id) },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                                            ) {
                                                Text(
                                                    text = "Load Previous Releases",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (readme == null) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    item {
                                        MarkdownText(
                                            markdown = readme,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReleaseItem(release: Release) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { expanded = !expanded },
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = release.tagName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (release.isPrerelease) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Pre-release",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (release.createdAt > 0) {
                        val formattedDate = remember(release.createdAt) {
                            DateUtils.formatTimestamp(release.createdAt)
                        }
                        Text(
                            text = "Published $formattedDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded) {
                android.util.Log.d("DetailScreen", "Release ${release.tagName} changelog len=${release.changelog.length}, preview=${release.changelog.take(100)}")
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (release.changelog.isNotBlank()) {
                        MarkdownText(
                            markdown = release.changelog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "No changelog provided for this release.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (release.assets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.assets),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        release.assets.forEach { asset ->
                            AssetRow(asset = asset, onDownload = {
                                DownloadUtils.downloadFile(context, asset.downloadUrl, asset.name)
                            })
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = stringResource(R.string.view_on_github),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssetRow(asset: ReleaseAsset, onDownload: () -> Unit) {
    Surface(
        onClick = onDownload,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 0.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${asset.size / 1024} KB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = "Download ${asset.name}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
