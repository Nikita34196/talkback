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
import android.view.View
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.training.content.NavigationIcons.ExitIcon
import com.google.android.accessibility.talkback.training.content.NavigationIcons.FinishEdgeIcon
import com.google.android.accessibility.talkback.training.content.NavigationIcons.NextEdgeIcon
import com.google.android.accessibility.talkback.training.content.NavigationIcons.NextIcon
import com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar
import com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar.BUTTON_TYPE_EXIT
import com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar.BUTTON_TYPE_FINISH
import com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar.BUTTON_TYPE_NEXT
import java.lang.IllegalArgumentException

fun createNavigationButton(
  context: Context,
  @NavigationButtonBar.ButtonType buttonType: Int,
  onClickListener: View.OnClickListener,
  buttonCount: Int,
  currentPageNumber: Int = 0,
): View {
  return ComposeView(context).apply {
    val onClick = { onClickListener.onClick(this) }

    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
      AccessibilitySuiteTheme {
        when (buttonType) {
          BUTTON_TYPE_NEXT ->
            when (buttonCount) {
              1 -> PositiveEdgeButton(onClick = onClick, content = NextEdgeIcon)
              else -> PositiveButton(onClick = onClick, content = NextIcon)
            }
          BUTTON_TYPE_EXIT -> NegativeButton(onClick = onClick, content = ExitIcon)
          BUTTON_TYPE_FINISH ->
            when (buttonCount) {
              1 -> PositiveEdgeButton(onClick = onClick, content = FinishEdgeIcon)
              else -> throw IllegalArgumentException("We don't support it.")
            }
          NavigationButtonBar.BUTTON_TYPE_BACK -> {
            throw IllegalArgumentException("We don't support it.")
          }
        }
      }
    }

    id = NavigationButtonBar.getButtonId(buttonType, currentPageNumber)
  }
}

private val buttonWidth = 54.67.dp
private val buttonHeight = 56.dp
private val bottomPadding = 42.dp
private val horizontalPadding = 2.dp

@Composable
internal fun PositiveEdgeButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
  EdgeButton(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(),
    buttonSize = EdgeButtonSize.Medium,
    content = content,
  )
}

@Composable
internal fun PositiveButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
  FilledTonalIconButton(
    onClick = onClick,
    modifier =
      Modifier.padding(bottom = bottomPadding, start = horizontalPadding)
        .size(buttonWidth, buttonHeight),
    colors = IconButtonDefaults.filledIconButtonColors(),
    shapes = IconButtonDefaults.shapes(CircleShape),
  ) {
    Row(content = content)
  }
}

@Composable
internal fun NegativeButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
  FilledTonalIconButton(
    onClick = onClick,
    modifier =
      Modifier.padding(bottom = bottomPadding, end = horizontalPadding)
        .size(buttonWidth, buttonHeight),
    colors = IconButtonDefaults.filledTonalIconButtonColors(),
    shapes = IconButtonDefaults.shapes(MaterialTheme.shapes.medium),
  ) {
    Row(content = content)
  }
}

object NavigationIcons {
  val NextIcon: @Composable RowScope.() -> Unit = {
    Icon(
      imageVector = Icons.AutoMirrored.Filled.ArrowForward,
      contentDescription = stringResource(R.string.training_next_button),
      modifier = Modifier.size(26.dp).align(Alignment.CenterVertically).testTag("NextIcon"),
    )
  }

  val NextEdgeIcon: @Composable RowScope.() -> Unit = {
    Icon(
      imageVector = Icons.AutoMirrored.Filled.ArrowForward,
      contentDescription = stringResource(R.string.training_next_button),
      modifier = Modifier.size(36.dp).align(Alignment.CenterVertically).testTag("NextEdgeIcon"),
    )
  }

  val ExitIcon: @Composable RowScope.() -> Unit = {
    Icon(
      imageVector = Icons.Outlined.Close,
      contentDescription = stringResource(R.string.training_finish_button),
      modifier = Modifier.size(26.dp).align(Alignment.CenterVertically).testTag("ExitIcon"),
    )
  }

  val FinishEdgeIcon: @Composable RowScope.() -> Unit = {
    Icon(
      imageVector = Icons.Filled.Check,
      contentDescription = stringResource(R.string.training_finish_button),
      modifier = Modifier.size(36.dp).align(Alignment.CenterVertically).testTag("FinishEdgeIcon"),
    )
  }
}
