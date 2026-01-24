package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VerticalScrollbarOrEmpty(state: LazyListState, modifier: Modifier = Modifier)
