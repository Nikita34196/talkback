package com.google.android.accessibility.talkback.compose.viewmodel

import android.graphics.Bitmap
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.accessibility.talkback.utils.QrCodeGenerator
import com.google.zxing.WriterException

/** A ViewModel that holds the info of a qr code page. */
class WearQrCodeViewModel : ViewModel() {

  data class QRCodePage(
    val title: String = "",
    val ctaText: String = "",
    val alternate: String = "",
    val bitmap: Bitmap? = null,
  )

  private val _qrCodePage = MutableLiveData<QRCodePage>()
  val qrCodePage: LiveData<QRCodePage> = _qrCodePage

  fun renderQrCode(title: String, ctaText: String, alternate: String, url: String?, sizePx: Int) {
    _qrCodePage.value = QRCodePage(title, ctaText, alternate, getUrlQrCode(url, sizePx))
  }

  private fun getUrlQrCode(url: String?, sizePx: Int): Bitmap? {
    if (url == null) {
      return null
    }

    val isValidUrl = Patterns.WEB_URL.matcher(url).matches()

    if (isValidUrl) {
      try {
        return QrCodeGenerator.encodeQrCode(url, sizePx, /* invert= */ true)
      } catch (ex: WriterException) {
        Log.w(TAG, "Could not generate the QR code: $url")
      }
    } else {
      // It shouldn't happen and we should prevent it in advance.
      throw IllegalArgumentException("The URL is invalid to be encoded as a QR code: $url")
    }
    return null
  }

  companion object {
    private const val TAG = "WearQrCodeViewModel"
  }
}
