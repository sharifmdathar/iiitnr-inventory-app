package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VerticalScrollbarOrEmpty(
    state: LazyListState,
    modifier: Modifier,
) {
    // Web handles scrollbars natively or via CSS, so we can leave this empty for now
}
