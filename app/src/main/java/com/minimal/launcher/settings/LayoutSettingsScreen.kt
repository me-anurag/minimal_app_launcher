package com.minimal.launcher.settings

import androidx.compose.runtime.Composable
import com.minimal.launcher.data.HomeLayout
import com.minimal.launcher.ui.MinimalTextButton

@Composable
fun LayoutSettingsScreen(
    selected: HomeLayout,
    onSelect: (HomeLayout) -> Unit,
    onBack: () -> Unit,
) {
    ScreenFrame("HOME SCREEN LAYOUT", onBack) {
        LayoutOption("Vertical", selected == HomeLayout.VERTICAL) { onSelect(HomeLayout.VERTICAL) }
        LayoutOption("Two Columns", selected == HomeLayout.TWO_COLUMNS) { onSelect(HomeLayout.TWO_COLUMNS) }
    }
}

@Composable
private fun LayoutOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    MinimalTextButton(
        text = (if (isSelected) "●  " else "○  ") + label,
        onClick = onClick,
        dim = !isSelected,   // selected option is bright, the other is dimmed
    )
}