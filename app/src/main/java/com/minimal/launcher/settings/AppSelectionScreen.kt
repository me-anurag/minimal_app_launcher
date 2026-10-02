package com.minimal.launcher.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.launcher.apps.AppEntry
import com.minimal.launcher.ui.MinimalTextButton

@Composable
fun AppSelectionScreen(
    allApps: List<AppEntry>,
    selectedPackages: Set<String>,
    onSave: (Set<String>) -> Unit,
    onBack: () -> Unit,
) {
    var selected by remember(selectedPackages) { mutableStateOf(selectedPackages) }
    // Selected apps first (based on what was saved), then the rest alphabetically
    val sorted = remember(allApps, selectedPackages) {
        allApps.sortedBy { it.packageName !in selectedPackages }
    }

    ScreenFrame("SELECT HOME APPS", onBack) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(sorted, key = { it.packageName }) { app ->
                val on = app.packageName in selected
                MinimalTextButton(
                    text = app.label,
                    onClick = {
                        selected = if (on) selected - app.packageName else selected + app.packageName
                    },
                    trailing = if (on) "✓" else "□",
                    fontSize = 20.sp,
                )
            }
        }
        MinimalTextButton(
            "[ Save ]",
            onClick = { onSave(selected) },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
