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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Text
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme

/**
 * The [androidx.preference.CheckBoxPreference] in radio style that renders preference UI with
 * Compose.
 */
open class ComposeRadioPreference
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
  defStyleRes: Int = 0,
) : ComposeBaseCheckBoxPreference(context, attrs, defStyleAttr, defStyleRes) {

  override fun providePreferenceComposable(): @Composable () -> Unit {
    return {
      AccessibilitySuiteTheme {
        RadioButton(
          selected = isChecked,
          enabled = isEnabled,
          onSelect = { performClick() },
          modifier =
            Modifier.fillMaxWidth().semantics {
              this.role = Role.RadioButton
              this.selected = isChecked
            },
        ) {
          Text(text = title.toString(), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }
}
