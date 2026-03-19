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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme
import kotlin.math.ceil

/** The [androidx.preference.PreferenceCategory] that renders preference UI with Compose. */
open class ComposePreferenceCategory
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
  defStyleRes: Int = 0,
) : ComposeBasePreferenceCategory(context, attrs, defStyleAttr, defStyleRes) {

  private val categoryComposable: @Composable () -> Unit = {
    AccessibilitySuiteTheme {
      val horizontalPadding =
        ceil(PREF_CATEGORY_HORIZONTAL_PADDING * LocalConfiguration.current.screenWidthDp).toInt().dp

      Box(
        modifier =
          Modifier.wrapContentHeight()
            .fillMaxWidth()
            .wrapContentWidth(Alignment.Start)
            .padding(top = 12.dp, bottom = 4.dp, start = horizontalPadding, end = horizontalPadding)
      ) {
        Text(
          text = title.toString(),
          style =
            MaterialTheme.typography.titleSmall.copy(
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Start,
            ),
          modifier = Modifier.semantics { heading() },
        )
      }
    }
  }

  override fun providePreferenceComposable(): @Composable () -> Unit {
    return if (title.isNullOrEmpty()) {
      {}
    } else {
      categoryComposable
    }
  }

  private companion object {
    // We copy the value from wear_preference_category_horizontal_padding_percent.
    const val PREF_CATEGORY_HORIZONTAL_PADDING = 0.075f
  }
}
