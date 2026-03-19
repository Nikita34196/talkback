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
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient
import com.google.android.accessibility.talkback.trainingcommon.content.PageContentConfig

class ComposePageIconButton(
  @DrawableRes private val drawableResId: Int,
  @StringRes private val contentDescriptionResId: Int,
  private val clickListener: OnClickListener,
) : PageContentConfig() {

  override fun createView(
    inflater: LayoutInflater,
    container: ViewGroup,
    context: Context,
    data: TrainingIpcClient.ServiceData,
  ): View? {
    return ComposeView(context).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AccessibilitySuiteTheme {
          FilledTonalIconButton(
            onClick = { clickListener.onClick(this@apply) },
            modifier =
              Modifier.padding(top = topPadding, bottom = bottomPadding)
                .size(iconButtonSize, iconButtonSize),
            colors = IconButtonDefaults.filledIconButtonColors(),
            shapes = IconButtonDefaults.shapes(CircleShape),
          ) {
            PageIcon()
          }
        }
      }
    }
  }

  @Composable
  private fun PageIcon() {
    Row {
      Icon(
        painter = painterResource(id = drawableResId),
        contentDescription = stringResource(contentDescriptionResId),
        modifier = Modifier.size(iconSize).align(Alignment.CenterVertically).testTag("PageIcon"),
      )
    }
  }
}

private val iconButtonSize = 60.dp

private val iconSize = 32.dp
private val topPadding = 16.dp
private val bottomPadding = 8.dp
