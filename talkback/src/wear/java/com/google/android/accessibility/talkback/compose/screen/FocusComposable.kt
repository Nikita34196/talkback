package com.google.android.accessibility.talkback.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun FocusOnResume(lifecycleOwner: LifecycleOwner, focusRequester: FocusRequester) {
  LaunchedEffect(Unit) {
    lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
      focusRequester.requestFocus()
    }
  }
}
