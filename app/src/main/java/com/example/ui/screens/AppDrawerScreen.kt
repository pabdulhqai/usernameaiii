package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.EssentialAppSlot
import com.example.model.InstalledAppInfo
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

@Composable
fun AppDrawerScreen(
    installedApps: List<InstalledAppInfo>,
    searchQuery: String,
    slotSelectionTarget: EssentialAppSlot?,
    onSearchQueryChange: (String) -> Unit,
    onLaunchApp: (InstalledAppInfo) -> Unit,
    onAssignAppToSlot: (Int, InstalledAppInfo) -> Unit,
    onResetSlotToDefault: (Int) -> Unit,
    onCancelSlotAssignment: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (slotSelectionTarget != null) {
            onCancelSlotAssignment()
        }
        onBackToHome()
    }

    val accent = MaterialTheme.colorScheme.primary
    val dimAccent = MaterialTheme.colorScheme.primaryContainer

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            installedApps.filter {
                it.label.lowercase(Locale.getDefault()).contains(q) ||
                    it.packageName.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("app_drawer_screen")
    ) {
        // If assigning an app to a Home Dock slot, show an explicit banner
        if (slotSelectionTarget != null) {
            Surface(
                color = dimAccent.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, accent, RoundedCornerShape(14.dp))
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ASSIGN HOME DOCK SLOT #${slotSelectionTarget.slotIndex + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent
                        )
                        Text(
                            text = "Select any installed app below or restore default (${slotSelectionTarget.role.defaultTitle})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    Row {
                        IconButton(
                            onClick = { onResetSlotToDefault(slotSelectionTarget.slotIndex) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = "Reset Slot to Default",
                                tint = accent
                            )
                        }
                        TextButton(onClick = onCancelSlotAssignment) {
                            Text("DONE", color = accent)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    "Filter ${installedApps.size} installed apps...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search apps",
                    tint = accent
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = dimAccent,
                focusedContainerColor = Color(0xFF060B09),
                unfocusedContainerColor = Color(0xFF060B09),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_drawer_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredApps.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = "No matching applications found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredApps,
                    key = { "${it.packageName}/${it.activityName}" }
                ) { app ->
                    MinimalAppRow(
                        app = app,
                        isAssignMode = slotSelectionTarget != null,
                        accent = accent,
                        dimAccent = dimAccent,
                        onClick = {
                            if (slotSelectionTarget != null) {
                                onAssignAppToSlot(slotSelectionTarget.slotIndex, app)
                            } else {
                                onLaunchApp(app)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalAppRow(
    app: InstalledAppInfo,
    isAssignMode: Boolean,
    accent: Color,
    dimAccent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF050A08))
            .border(1.dp, dimAccent.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Minimal monochrome initial glyph instead of heavy full-color bitmap icons to save OLED power
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(dimAccent.copy(alpha = 0.35f))
                    .border(1.dp, accent.copy(alpha = 0.8f), CircleShape)
            ) {
                Text(
                    text = app.label.take(1).uppercase(Locale.getDefault()),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = if (isAssignMode) Icons.Filled.PushPin else Icons.AutoMirrored.Filled.Launch,
            contentDescription = if (isAssignMode) "Pin to slot" else "Launch ${app.label}",
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
    }
}
