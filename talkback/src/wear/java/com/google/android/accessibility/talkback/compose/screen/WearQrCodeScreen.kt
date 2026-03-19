package com.google.android.accessibility.talkback.compose.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.compose.screen.WearQrCodeSpec.AlternatePaddingBottom
import com.google.android.accessibility.talkback.compose.screen.WearQrCodeSpec.AlternatePaddingHorizontal
import com.google.android.accessibility.talkback.compose.screen.WearQrCodeSpec.AlternatePaddingTop
import com.google.android.accessibility.talkback.compose.screen.WearQrCodeSpec.CallToActionPaddingTop
import com.google.android.accessibility.talkback.compose.screen.WearQrCodeSpec.QrCodePaddingTop
import com.google.android.accessibility.talkback.compose.screen.WearQrCodeSpec.TitlePaddingTop
import com.google.android.accessibility.talkback.compose.viewmodel.WearQrCodeViewModel

/** Binds the [qrCodeViewModel] to a screen showing a qr code page. */
@Composable
fun WearQrCodeScreen(qrCodeViewModel: WearQrCodeViewModel) {

  val focusRequester = remember { FocusRequester() }
  val lazyColumnState = rememberScrollState()

  val qrCodePage by
    qrCodeViewModel.qrCodePage.observeAsState(initial = WearQrCodeViewModel.QRCodePage())

  AppScaffold {
    ScreenScaffold(scrollState = lazyColumnState) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .focusable()
            .rotaryScrollable(
              RotaryScrollableDefaults.behavior(scrollableState = lazyColumnState),
              focusRequester,
            )
            .verticalScroll(lazyColumnState),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = qrCodePage.title,
          modifier = Modifier.fillMaxWidth().padding(top = TitlePaddingTop),
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.bodyLarge,
        )
        Text(
          text = qrCodePage.ctaText,
          modifier = Modifier.fillMaxWidth().padding(top = CallToActionPaddingTop),
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.bodyLarge,
        )
        val bitmap = qrCodePage.bitmap
        if (bitmap != null) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            modifier = Modifier.fillMaxWidth().padding(top = QrCodePaddingTop),
            contentDescription = stringResource(id = R.string.qr_code_content_description),
          )
        }
        Text(
          text = qrCodePage.alternate,
          modifier =
            Modifier.fillMaxWidth()
              .padding(
                top = AlternatePaddingTop,
                start = AlternatePaddingHorizontal,
                end = AlternatePaddingHorizontal,
                bottom = AlternatePaddingBottom,
              ),
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      FocusOnResume(LocalLifecycleOwner.current, focusRequester)
    }
  }
}

internal object WearQrCodeSpec {
  val TitlePaddingTop = 42.dp
  val CallToActionPaddingTop = 12.dp
  val QrCodePaddingTop = 8.dp

  val AlternatePaddingTop = 8.dp
  val AlternatePaddingHorizontal = 24.dp
  val AlternatePaddingBottom = 58.dp
}
