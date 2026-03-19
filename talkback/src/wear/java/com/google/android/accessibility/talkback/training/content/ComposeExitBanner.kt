/*
 * Copyright (C) 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.accessibility.talkback.training.content

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient
import com.google.android.accessibility.talkback.trainingcommon.content.ExitBanner

class ComposeExitBanner : ExitBanner() {

  override fun createView(
    inflater: LayoutInflater,
    container: ViewGroup,
    context: Context,
    data: TrainingIpcClient.ServiceData,
  ): View? {
    return (inflater.inflate(R.layout.training_compose, container, false) as ComposeView).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent { AccessibilitySuiteTheme { ExitBanner() } }
    }
  }

  @Composable
  private fun ExitBanner() {
    val talkBackOnString = stringResource(R.string.turn_off_talkback)
    val tapTalkBackAgainString = stringResource(R.string.tap_again_to_turn_off)

    var displayedText by rememberSaveable { mutableStateOf(talkBackOnString) }
    var lastHoverEnterTimestamp by remember { mutableLongStateOf(0L) }

    Box(
      modifier =
        Modifier.wrapContentHeight()
          .fillMaxWidth()
          .padding(
            bottom = dimensionResource(R.dimen.training_banner_padding_bottom),
            start = dimensionResource(R.dimen.training_text_padding_horizontal),
            end = dimensionResource(R.dimen.training_text_padding_horizontal),
          )
          // We call it to setImportantForAccessibility as no to make the initial focus on the
          // description of the training content.
          .clearAndSetSemantics {},
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = stringResource(R.string.talkback_on),
          style =
            MaterialTheme.typography.bodyMedium.copy(
              textAlign = TextAlign.Center,
              lineHeight = dimensionResource(R.dimen.training_text_height).value.sp,
            ),
        )

        FilledTonalButton(
          modifier =
            Modifier.fillMaxWidth()
              .padding(top = dimensionResource(R.dimen.training_text_padding_top))
              .pointerInteropFilter { motionEvent ->
                when (motionEvent.action) {
                  MotionEvent.ACTION_DOWN,
                  MotionEvent.ACTION_HOVER_ENTER -> {
                    lastHoverEnterTimestamp = System.currentTimeMillis()
                  }

                  MotionEvent.ACTION_UP,
                  MotionEvent.ACTION_HOVER_EXIT -> {
                    val currentTime = System.currentTimeMillis()
                    val timeDifference = currentTime - lastHoverEnterTimestamp
                    if (timeDifference < TAP_TIMEOUT_MS && lastHoverEnterTimestamp != 0L) {
                      // If the time difference is less than the threshold
                      displayedText = tapTalkBackAgainString
                      clickListener.onClick(/* v= */ null)
                    }
                  }
                }
                true
              },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
          onClick = {
            displayedText = tapTalkBackAgainString
            clickListener.onClick(/* v= */ null)
          },
        ) {
          Text(
            text = displayedText,
            modifier = Modifier.fillMaxSize(),
            style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center),
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

val TAP_TIMEOUT_MS = ViewConfiguration.getJumpTapTimeout()
