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
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.trainingcommon.PageConfig
import com.google.android.accessibility.talkback.trainingcommon.content.ExitBanner
import com.google.android.accessibility.talkback.trainingcommon.content.PageContentConfig
import com.google.android.accessibility.talkback.trainingcommon.content.Text
import com.google.android.accessibility.talkback.trainingcommon.content.TextList
import com.google.android.accessibility.talkback.trainingcommon.content.Title
import com.google.android.accessibility.talkback.trainingcommon.content.TutorialContentInterfaceInjector
import com.google.android.accessibility.talkback.trainingcommon.content.WebViewContentConfig

/** Provides an implementation on tutorial content, like [Text], etc., for the Wear form factor. */
class TutorialContentSupplierImpl : TutorialContentInterfaceInjector.TutorialContentSupplier {
  override fun provideText(vararg paragraphs: Text.Paragraph): Text {
    return ComposeText(*paragraphs)
  }

  override fun provideWebView(@StringRes textResId: Int): WebViewContentConfig {
    return WebViewContentConfig(textResId)
  }

  override fun provideTitle(pageConfig: PageConfig): Title {
    return ComposeTitle(pageConfig)
  }

  override fun providePageIconButton(
    drawableResId: Int,
    contentDescriptionResId: Int,
    clickListener: View.OnClickListener,
  ): PageContentConfig? {
    return ComposePageIconButton(drawableResId, contentDescriptionResId, clickListener)
  }

  override fun provideTextList(titlesResId: Int, summariesResId: Int): TextList {
    return ComposeTextList(titlesResId, summariesResId)
  }

  override fun provideExitBanner(predicate: PageConfig.PageAndContentPredicate): ExitBanner {
    return ComposeExitBanner().apply { this@apply.setShowingPredicate(predicate) }
  }

  override fun provideNavigationButton(
    context: Context,
    buttonType: Int,
    text: Int,
    onClickListener: View.OnClickListener,
    buttonCount: Int,
    currentPageNumber: Int,
  ): View {
    return createNavigationButton(
      context,
      buttonType,
      onClickListener,
      buttonCount,
      currentPageNumber,
    )
  }
}
