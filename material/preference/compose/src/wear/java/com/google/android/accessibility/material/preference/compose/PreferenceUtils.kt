/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.accessibility.material.preference.compose

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

internal fun TwoStatePreference.toSummaryOnOff(): CharSequence? {
  return if (isChecked && !summaryOn.isNullOrEmpty()) {
    summaryOn
  } else if (!isChecked && !summaryOff.isNullOrEmpty()) {
    summaryOff
  } else if (!summary.isNullOrEmpty()) {
    summary
  } else {
    null
  }
}

internal fun TwoStatePreference.toSecondaryLabelForSummaryOnOff():
  @Composable (RowScope.() -> Unit)? {
  val summary = toSummaryOnOff()
  val secondaryLabel: @Composable (RowScope.() -> Unit)? =
    if (summary != null) {
      { Text(summary.toString(), maxLines = 2, overflow = TextOverflow.Ellipsis) }
    } else {
      null
    }
  return secondaryLabel
}

internal fun Preference.toSecondaryLabel(): @Composable (RowScope.() -> Unit)? {
  val secondaryLabel: @Composable (RowScope.() -> Unit)? =
    if (summary != null) {
      { Text(summary.toString(), maxLines = 2, overflow = TextOverflow.Ellipsis) }
    } else {
      null
    }
  return secondaryLabel
}

internal const val TEST_TAG_SPLIT_SUMMARY = "TEST_TAG_SPLIT_SUMMARY"

@Composable
internal fun SummaryBlock(summary: CharSequence) {
  Text(
    text = summary.toString(),
    modifier =
      Modifier.padding(ButtonDefaults.ContentPadding)
        .fillMaxWidth()
        .testTag(TEST_TAG_SPLIT_SUMMARY)
        .clearAndSetSemantics {},
    style =
      MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
      ),
  )
}

@VisibleForTesting internal const val PERIOD_SEPARATOR = ". "

internal fun Preference.splitSummaryToContentDescription(): String {
  return title?.toString() + PERIOD_SEPARATOR + summary
}

internal fun TwoStatePreference.splitSummaryToContentDescription(): String {
  return title?.toString() + PERIOD_SEPARATOR + toSummaryOnOff().toString()
}
