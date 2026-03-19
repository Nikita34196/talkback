/*
 * Copyright (C) 2024 Google Inc.
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.trainingcommon.PageConfig
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient
import com.google.android.accessibility.talkback.trainingcommon.content.Title

class ComposeTitle constructor(pageConfig: PageConfig) : Title(pageConfig) {
  override fun createView(
    inflater: LayoutInflater,
    container: ViewGroup,
    context: Context,
    data: TrainingIpcClient.ServiceData?,
  ): View {

    val title = provideFinalizedTitle(context)

    return (inflater.inflate(R.layout.training_compose, container, false) as ComposeView).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AccessibilitySuiteTheme {
          Box(
            modifier =
              Modifier.wrapContentHeight()
                .fillMaxWidth()
                .padding(
                  start = dimensionResource(R.dimen.training_title_padding_horizontal),
                  end = dimensionResource(R.dimen.training_title_padding_horizontal),
                ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            )
          }
        }
      }
    }
  }
}
