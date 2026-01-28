package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VerticalScrollbarOrEmpty(
    state: LazyListState,
    modifier: Modifier,
) {
    VerticalScrollbar(
        modifier = modifier.fillMaxHeight(),
        adapter = rememberScrollbarAdapter(scrollState = state),
    )
}
