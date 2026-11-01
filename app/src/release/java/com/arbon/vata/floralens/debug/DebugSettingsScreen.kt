package com.arbon.vata.floralens.debug

import androidx.compose.runtime.Composable

/** Release builds do not register a settings route or include debug tools. */
@Composable
fun DebugSettingsScreen(onBack: () -> Unit, onHistory: () -> Unit) = Unit
