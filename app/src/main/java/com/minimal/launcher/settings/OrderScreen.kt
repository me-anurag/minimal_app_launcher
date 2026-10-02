package com.minimal.launcher.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.launcher.apps.AppEntry
import com.minimal.launcher.ui.MinimalColors

@Composable
fun OrderScreen(
    apps: List<AppEntry>,
    onMove: (from: Int, to: Int) -> Unit,
    onBack: () -> Unit,
) {
    ScreenFrame("HOME SCREEN ORDER", onBack) {
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { i, app ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${i + 1}. ${app.label}",
                        color = MinimalColors.Text,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Arrow("↑", enabled = i > 0) { onMove(i, i - 1) }
                    Arrow("↓", enabled = i < apps.lastIndex) { onMove(i, i + 1) }
                }
            }
        }
    }
}

@Composable
private fun Arrow(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        symbol,
        color = if (enabled) MinimalColors.Text else Color_Disabled,
        fontSize = 22.sp,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(14.dp),
    )
}

private val Color_Disabled = androidx.compose.ui.graphics.Color(0xFF333333)