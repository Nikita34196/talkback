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
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient
import com.google.android.accessibility.talkback.trainingcommon.content.TextList

class ComposeTextList(titlesResId: Int, summariesResId: Int) :
  TextList(titlesResId, summariesResId) {
  override fun createView(
    inflater: LayoutInflater,
    container: ViewGroup,
    context: Context,
    data: TrainingIpcClient.ServiceData?,
  ): View {
    val titles = getTitles(context)
    val summaries = getSummaries(context)

    return (inflater.inflate(R.layout.training_compose, container, false) as ComposeView).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent { AccessibilitySuiteTheme { TitleList(titles, summaries) } }
    }
  }

  @Composable
  fun TitleList(titles: Array<String>, summaries: Array<String>) {

    val textListHeight =
      dimensionResource(R.dimen.wear_alertdialog_menuitem_height).value.dp +
        dimensionResource(R.dimen.training_list_item_padding).value.dp

    Column(
      modifier =
        Modifier.wrapContentHeight()
          .fillMaxWidth()
          .padding(
            top = dimensionResource(R.dimen.training_list_margin_top).value.dp,
            start = dimensionResource(R.dimen.training_list_margin_horizontal).value.dp,
            end = dimensionResource(R.dimen.training_list_margin_horizontal).value.dp,
          )
    ) {
      for (index in titles.indices) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(height = textListHeight)
              .padding(bottom = dimensionResource(R.dimen.training_list_item_padding).value.dp)
              .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape =
                  RoundedCornerShape(
                    size = dimensionResource(R.dimen.training_list_item_corner_radius).value.dp
                  ),
              )
              .semantics {
                this.contentDescription = titles[index] + PERIOD_SEPARATOR + summaries[index]
              }
              .focusable(),
          contentAlignment = Alignment.CenterStart,
        ) {
          Text(
            text = titles[index],
            modifier =
              Modifier.padding(
                  start =
                    dimensionResource(R.dimen.wear_alertdialog_menuitem_padding_start).value.dp
                )
                .clearAndSetSemantics {},
            style =
              MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = dimensionResource(R.dimen.training_text_height).value.sp,
              ),
          )
        }
      }
    }
  }
}

@VisibleForTesting internal const val PERIOD_SEPARATOR = ". "
