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

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.res.TypedArrayUtils
import androidx.core.content.withStyledAttributes
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Text
import com.google.android.accessibility.material.flags.MaterialFlags
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme

/** The [androidx.preference.Preference] that renders preference UI with Compose. */
open class ComposePreference
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
  defStyleRes: Int = 0,
) : ComposeBasePreference(context, attrs, defStyleAttr, defStyleRes) {

  @VisibleForTesting internal var splitSummary: Boolean = false

  init {
    context.withStyledAttributes(attrs, R.styleable.ComposePreference, defStyleAttr, defStyleRes) {
      splitSummary =
        TypedArrayUtils.getBoolean(
          this,
          R.styleable.ComposePreference_splitSummary,
          /* fallbackIndex= */ 0,
          /* defaultValue= */ false,
        ) && MaterialFlags.ENABLE_PREFERENCE_SPLIT_SUMMARY
    }
  }

  override fun providePreferenceComposable(): @Composable () -> Unit {
    return {
      AccessibilitySuiteTheme {
        if (splitSummary) {
          Column(
            modifier =
              Modifier.clearAndSetSemantics {
                contentDescription = splitSummaryToContentDescription()
                this.onClick(
                  action = {
                    performClick()
                    true
                  }
                )
                role = Role.Button
              }
          ) {
            FilledTonalButton(
              onClick = { performClick() },
              label = { title?.let { Text(it.toString(), textAlign = TextAlign.Start) } },
              modifier = Modifier.fillMaxWidth(),
            )
            summary?.let { SummaryBlock(it) }
          }
        } else {
          FilledTonalButton(
            label = {
              Text(
                title.toString(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
              )
            },
            onClick = { performClick() },
            secondaryLabel = toSecondaryLabel(),
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}
