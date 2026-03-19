package com.google.android.accessibility.material.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/** Custom Material3 Theme for Accessibility Suite App. */
@Composable
fun AccessibilitySuiteTheme(content: @Composable () -> Unit) {
  MaterialTheme(content = content)
}
