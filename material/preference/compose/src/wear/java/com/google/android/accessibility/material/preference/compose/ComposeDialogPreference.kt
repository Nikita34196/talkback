package com.google.android.accessibility.material.preference.compose

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.res.TypedArrayUtils
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme

/** The [androidx.preference.DialogPreference] that renders preference UI with Compose. */
open class ComposeDialogPreference
@JvmOverloads
constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
  defStyleRes: Int = 0,
) : ComposeBaseDialogPreference(context, attrs, defStyleAttr, defStyleRes) {

  var onPositiveButtonClicked: (() -> Unit)? = null
  var onNegativeButtonClicked: (() -> Unit)? = null
  var onEdgeButtOnClicked: (() -> Unit)? = null
  var content: (ScalingLazyListScope.() -> Unit)? = null

  private val dialogIconResId: Int
  // If this flag is true, please make sure to implement onPreferenceDisplayDialog() to handle
  // displaying a custom dialog for this Preference.
  var disableOnDisplayPreferenceDialog: Boolean
  var edgeButtonText: String?

  init {
    var array =
      context.obtainStyledAttributes(attrs, R.styleable.DialogPreference, defStyleAttr, defStyleRes)
    dialogIconResId =
      TypedArrayUtils.getResourceId(
        array,
        R.styleable.DialogPreference_android_dialogIcon,
        R.styleable.DialogPreference_dialogIcon,
        /* defaultValue= */ 0,
      )
    array.recycle()

    array =
      context.obtainStyledAttributes(
        attrs,
        R.styleable.ComposeDialogPreference,
        defStyleAttr,
        defStyleRes,
      )
    edgeButtonText =
      TypedArrayUtils.getString(
        array,
        R.styleable.ComposeDialogPreference_edgeButtonText,
        /* fallbackIndex= */ 0,
      )
    disableOnDisplayPreferenceDialog =
      TypedArrayUtils.getBoolean(
        array,
        R.styleable.ComposeDialogPreference_disableOnDisplayPreferenceDialog,
        /* fallbackIndex= */ 0,
        /* defaultValue= */ false,
      )
    array.recycle()
  }

  override fun providePreferenceComposable(): @Composable () -> Unit {
    return {
      var dialogOpened by remember { mutableStateOf(false) }

      AccessibilitySuiteTheme {
        val edgeButtonText = this.edgeButtonText
        if (edgeButtonText.isNullOrEmpty()) {
          DefaultAlertDialog(dialogOpened) { dialogOpened = false }
        } else {
          EdgeButtonAlertDialog(dialogOpened, edgeButtonText) { dialogOpened = false }
        }

        FilledTonalButton(
          label = { Text(title.toString(), maxLines = 3, overflow = TextOverflow.Ellipsis) },
          onClick = {
            if (disableOnDisplayPreferenceDialog) {
              dialogOpened = true
            } else {
              performClick()
            }
          },
          secondaryLabel = toSecondaryLabel(),
        )
      }
    }
  }

  @Composable
  private fun DefaultAlertDialog(dialogOpened: Boolean, onCloseDialog: () -> Unit) {
    AlertDialog(
      visible = dialogOpened,
      onDismissRequest = onCloseDialog,
      title = { Text(dialogTitle.toString()) },
      text = { Text(dialogMessage.toString()) },
      icon = {
        if (dialogIconResId != 0) {
          Icon(
            painter = painterResource(id = dialogIconResId),
            contentDescription = dialogTitle.toString(),
          )
        }
      },
      confirmButton = {
        AlertDialogDefaults.ConfirmButton(
          onClick = {
            onCloseDialog()
            onPositiveButtonClicked?.invoke()
          }
        )
      },
      dismissButton = {
        AlertDialogDefaults.DismissButton(
          onClick = {
            onCloseDialog()
            onNegativeButtonClicked?.invoke()
          }
        )
      },
      properties = DialogProperties(windowTitle = DEFAULT_WINDOW_TITLE_FOR_DIALOG),
      content = content,
    )
  }

  @Composable
  private fun EdgeButtonAlertDialog(
    dialogOpened: Boolean,
    edgeButtonText: String,
    onCloseDialog: () -> Unit,
  ) {
    AlertDialog(
      visible = dialogOpened,
      onDismissRequest = onCloseDialog,
      title = { Text(dialogTitle.toString()) },
      text = { Text(dialogMessage.toString()) },
      icon = {
        if (dialogIconResId != 0) {
          Icon(
            painter = painterResource(id = dialogIconResId),
            contentDescription = dialogTitle.toString(),
          )
        }
      },
      edgeButton = {
        AlertDialogDefaults.EdgeButton(
          onClick = {
            onCloseDialog()
            onEdgeButtOnClicked?.invoke()
          }
        ) {
          Text(edgeButtonText)
        }
      },
      properties = DialogProperties(windowTitle = DEFAULT_WINDOW_TITLE_FOR_DIALOG),
      content = content,
    )
  }
}

const val DEFAULT_WINDOW_TITLE_FOR_DIALOG = " "
