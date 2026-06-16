package com.zhang.adbhub.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zhang.adbhub.desktop.utils.StringResources

/**
 * 根目录选择器组件
 * 类似 Android Studio Device Explorer 的顶层目录视图
 */
@Composable
fun RootDirectorySelector(
    onDirectorySelected: (String) -> Unit,
    isExecuting: Boolean,
    modifier: Modifier = Modifier
) {
    val rootDirectories = listOf(
        RootDirectory(
            path = "/",
            icon = Icons.Default.Folder,
            title = StringResources.get("file.manager.directory.root"),
            color = MaterialTheme.colorScheme.primary
        ),
        RootDirectory(
            path = "/sdcard",
            icon = Icons.Default.Sd,
            title = StringResources.get("file.manager.directory.sdcard"),
            color = MaterialTheme.colorScheme.tertiary
        ),
        RootDirectory(
            path = "/system",
            icon = Icons.Default.Settings,
            title = StringResources.get("file.manager.directory.system"),
            color = MaterialTheme.colorScheme.secondary
        ),
        RootDirectory(
            path = "/data",
            icon = Icons.Default.Storage,
            title = StringResources.get("file.manager.directory.data"),
            color = MaterialTheme.colorScheme.error
        ),
        RootDirectory(
            path = "/vendor",
            icon = Icons.Default.Business,
            title = StringResources.get("file.manager.directory.vendor"),
            color = MaterialTheme.colorScheme.tertiary
        ),
        RootDirectory(
            path = "/storage",
            icon = Icons.Default.SdStorage,
            title = StringResources.get("file.manager.directory.storage"),
            color = MaterialTheme.colorScheme.primary
        ),
        RootDirectory(
            path = "/cache",
            icon = Icons.Default.Cached,
            title = StringResources.get("file.manager.directory.cache"),
            color = MaterialTheme.colorScheme.secondary
        )
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = StringResources.get("file.manager.quick.access"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(rootDirectories) { directory ->
                RootDirectoryCard(
                    directory = directory,
                    onClick = { onDirectorySelected(directory.path) },
                    enabled = !isExecuting
                )
            }
        }
    }
}

private data class RootDirectory(
    val path: String,
    val icon: ImageVector,
    val title: String,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
private fun RootDirectoryCard(
    directory: RootDirectory,
    onClick: () -> Unit,
    enabled: Boolean
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = directory.icon,
                contentDescription = null,
                tint = directory.color,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = directory.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Text(
                text = directory.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
