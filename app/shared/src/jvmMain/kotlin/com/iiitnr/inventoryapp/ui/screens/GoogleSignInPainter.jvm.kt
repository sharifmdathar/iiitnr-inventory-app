package com.iiitnr.inventoryapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.iiitnr.inventoryapp.shared.Res
import com.iiitnr.inventoryapp.shared.google_g_logo
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun googleSignInPainter(): Painter = painterResource(Res.drawable.google_g_logo)
