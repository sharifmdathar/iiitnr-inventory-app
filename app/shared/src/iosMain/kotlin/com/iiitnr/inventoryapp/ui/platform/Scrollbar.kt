package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VerticalScrollbarOrEmpty(
    state: LazyListState,
    modifier: Modifier,
) {
    // iOS has native scrollbars, so we don't need to draw a custom one.
}
