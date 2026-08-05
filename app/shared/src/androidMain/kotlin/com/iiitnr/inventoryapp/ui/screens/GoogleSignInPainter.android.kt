package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

@Composable
actual fun googleSignInPainter(): Painter {
    val imageVector =
        ImageVector
            .Builder(
                name = "Google",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color(0xFF4285F4))) {
                    moveTo(22.56f, 12.25f)
                    curveTo(22.56f, 11.47f, 22.49f, 10.72f, 22.36f, 10.0f)
                    lineTo(12.0f, 10.0f)
                    lineTo(12.0f, 14.26f)
                    lineTo(17.92f, 14.26f)
                    curveTo(17.66f, 15.63f, 16.88f, 16.79f, 15.71f, 17.57f)
                    lineTo(15.71f, 20.34f)
                    lineTo(19.28f, 20.34f)
                    curveTo(21.36f, 18.42f, 22.56f, 15.6f, 22.56f, 12.25f)
                    close()
                }
                path(fill = SolidColor(Color(0xFF34A853))) {
                    moveTo(12.0f, 23.0f)
                    curveTo(14.97f, 23.0f, 17.46f, 22.02f, 19.28f, 20.34f)
                    lineTo(15.71f, 17.57f)
                    curveTo(14.73f, 18.23f, 13.48f, 18.63f, 12.0f, 18.63f)
                    curveTo(9.14f, 18.63f, 6.71f, 16.7f, 5.84f, 14.09f)
                    lineTo(2.18f, 14.09f)
                    lineTo(2.18f, 16.93f)
                    curveTo(3.99f, 20.53f, 7.7f, 23.0f, 12.0f, 23.0f)
                    close()
                }
                path(fill = SolidColor(Color(0xFFFBBC05))) {
                    moveTo(5.84f, 14.09f)
                    curveTo(5.62f, 13.43f, 5.49f, 12.73f, 5.49f, 12.0f)
                    curveTo(5.49f, 11.27f, 5.62f, 10.57f, 5.84f, 9.91f)
                    lineTo(5.84f, 7.07f)
                    lineTo(2.18f, 7.07f)
                    curveTo(1.43f, 8.55f, 1.0f, 10.22f, 1.0f, 12.0f)
                    curveTo(1.0f, 13.78f, 1.43f, 15.45f, 2.18f, 16.93f)
                    lineTo(5.84f, 14.09f)
                    close()
                }
                path(fill = SolidColor(Color(0xFFEA4335))) {
                    moveTo(12.0f, 5.38f)
                    curveTo(13.62f, 5.38f, 15.06f, 5.94f, 16.21f, 7.02f)
                    lineTo(19.36f, 3.87f)
                    curveTo(17.46f, 2.09f, 14.97f, 1.0f, 12.0f, 1.0f)
                    curveTo(7.7f, 1.0f, 3.99f, 3.47f, 2.18f, 7.07f)
                    lineTo(5.84f, 9.91f)
                    curveTo(6.71f, 7.3f, 9.14f, 5.38f, 12.0f, 5.38f)
                    close()
                }
            }.build()

    return rememberVectorPainter(imageVector)
}
