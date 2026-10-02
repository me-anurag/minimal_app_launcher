package com.minimal.launcher.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.launcher.apps.AppEntry
import com.minimal.launcher.ui.MinimalColors
import com.minimal.launcher.ui.MinimalTextButton

/** Full list of launchable apps + search. Launching here does NOT add the app to the home screen. */
@Composable
fun AllAppsScreen(
    allApps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(allApps, query) {
        val q = query.trim()
        if (q.isEmpty()) allApps else allApps.filter { it.label.contains(q, ignoreCase = true) }
    }

    ScreenFrame("ALL APPS", onBack) {
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = MinimalColors.Text, fontSize = 20.sp),
            cursorBrush = SolidColor(MinimalColors.Text),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { filtered.firstOrNull()?.let(onLaunch) }),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text("[ Search apps... ]", color = MinimalColors.Dim, fontSize = 20.sp)
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        LazyColumn(Modifier.weight(1f)) {
            items(filtered, key = { it.packageName }) { app ->
                MinimalTextButton(app.label, onClick = { onLaunch(app) })
            }
        }
    }
}