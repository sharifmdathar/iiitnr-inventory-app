package com.iiitnr.inventoryapp.ui.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

@Composable
fun ProfilePicture(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    if (imageUrl != null) {
        val sanitizedUrl = imageUrl.replace("http://", "https://")
        SubcomposeAsyncImage(
            model = sanitizedUrl,
            contentDescription = "Profile picture",
            modifier =
                modifier
                    .size(size)
                    .clip(CircleShape),
            contentScale = ContentScale.Crop,
            loading = { ProfilePicturePlaceholder(size = size) },
            error = { ProfilePicturePlaceholder(size = size) },
        )
    } else {
        ProfilePicturePlaceholder(size = size, modifier = modifier)
    }
}

@Composable
private fun ProfilePicturePlaceholder(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(size / 2),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
