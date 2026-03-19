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

import android.view.View
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.compose.screen.FocusOnResume
import com.google.android.accessibility.talkback.trainingcommon.content.TutorialContentInterfaceInjector

/** Provides an implementation on adding contents into scrollable view. */
class TutorialContentManagerImpl(
  parentView: View,
  bannerLayout: View?,
  navigationBarContainer: View?,
) : TutorialContentInterfaceInjector.TutorialContentManager {

  private var bannerView: View? = null
  private val contentList = mutableStateListOf<View>()
  private var navigationView: View? = null

  init {
    (parentView as? ComposeView)?.apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AccessibilitySuiteTheme { TutorialContentViewList(bannerView, contentList, navigationView) }
      }
    } ?: throw IllegalArgumentException("parentView should be ComposeView for tutorial pages")
  }

  override fun addTutorialContentView(tutorialContentView: View) {
    contentList.add(tutorialContentView)
  }

  override fun addTutorialBanner(bannerView: View) {
    this.bannerView = bannerView
  }

  override fun hasTutorialBannerContainer(): Boolean {
    return true
  }

  override fun addNavigationContentView(navigationContentView: View) {
    navigationView = navigationContentView
  }

  @Composable
  fun TutorialContentViewList(
    bannerView: View?,
    views: SnapshotStateList<View>,
    navigationView: View?,
  ) {
    val focusRequester = rememberActiveFocusRequester()
    val scrollState = rememberScrollState()

    AppScaffold {
      ScreenScaffold(scrollState = scrollState) {
        Column(
          Modifier.fillMaxSize()
            .rotaryScrollable(
              RotaryScrollableDefaults.behavior(scrollableState = scrollState),
              focusRequester,
            )
            .verticalScroll(scrollState)
            .focusable(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Spacer(modifier = Modifier.padding(top = dimensionResource(R.dimen.training_padding_top)))

          bannerView?.apply { AndroidView(factory = { this }) }

          for (view in views) {
            AndroidView(factory = { view })
          }

          navigationView?.apply {
            AndroidView(factory = { this }, modifier = Modifier.fillMaxWidth())
          }
        }

        FocusOnResume(LocalLifecycleOwner.current, focusRequester)
      }
    }
  }
}
