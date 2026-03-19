package com.google.android.accessibility.talkback.compose.widget

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FailureConfirmationDialog
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.openOnPhoneDialogCurvedText
import com.google.android.accessibility.material.theme.AccessibilitySuiteTheme

@JvmOverloads
fun createSuccessConfirmationDialog(
  context: Context,
  successText: String? = null,
  contentDescriptionString: String? = null,
  onDismissRequest: (() -> Unit)? = null,
): View {
  return ComposeView(context).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
      AccessibilitySuiteTheme {
        A11ySuccessConfirmationDialog(successText, contentDescriptionString, onDismissRequest)
      }
    }
  }
}

@JvmOverloads
fun createFailureConfirmationDialog(
  context: Context,
  failureText: String? = null,
  contentDescriptionString: String? = null,
  onDismissRequest: (() -> Unit)? = null,
): View {
  return ComposeView(context).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
      AccessibilitySuiteTheme {
        A11yFailureConfirmationDialog(failureText, contentDescriptionString, onDismissRequest)
      }
    }
  }
}

@JvmOverloads
fun createOpenOnPhoneConfirmationDialog(
  context: Context,
  onDismissRequest: (() -> Unit)? = null,
): View {
  return ComposeView(context).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent { AccessibilitySuiteTheme { A11yOpenOnPhoneConfirmationDialog(onDismissRequest) } }
  }
}

@Composable
fun A11ySuccessConfirmationDialog(
  successText: String? = null,
  contentDescriptionString: String? = null,
  onDismissRequest: (() -> Unit)? = null,
) {
  var showConfirmation by remember { mutableStateOf(true) }
  val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle

  val modifier =
    if (contentDescriptionString != null) {
      Modifier.clearAndSetSemantics { contentDescription = contentDescriptionString }
    } else {
      Modifier
    }

  SuccessConfirmationDialog(
    visible = showConfirmation,
    onDismissRequest = {
      showConfirmation = false
      onDismissRequest?.invoke()
    },
    curvedText = { successText?.let { confirmationDialogCurvedText(it, curvedTextStyle) } },
    modifier = modifier,
    properties = DialogProperties(windowTitle = DEFAULT_WINDOW_TITLE_FOR_DIALOG),
  )
}

@Composable
fun A11yFailureConfirmationDialog(
  failureText: String? = null,
  contentDescriptionString: String? = null,
  onDismissRequest: (() -> Unit)? = null,
) {
  var showConfirmation by remember { mutableStateOf(true) }
  val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle

  val modifier =
    if (contentDescriptionString != null) {
      Modifier.clearAndSetSemantics { contentDescription = contentDescriptionString }
    } else {
      Modifier
    }

  FailureConfirmationDialog(
    visible = showConfirmation,
    onDismissRequest = {
      showConfirmation = false
      onDismissRequest?.invoke()
    },
    curvedText = { failureText?.let { confirmationDialogCurvedText(it, curvedTextStyle) } },
    modifier = modifier,
    properties = DialogProperties(windowTitle = DEFAULT_WINDOW_TITLE_FOR_DIALOG),
  )
}

@Composable
fun A11yOpenOnPhoneConfirmationDialog(onDismissRequest: (() -> Unit)? = null) {
  var showConfirmation by remember { mutableStateOf(true) }

  val openOnPhoneText = OpenOnPhoneDialogDefaults.text
  val curvedTextStyle = OpenOnPhoneDialogDefaults.curvedTextStyle

  OpenOnPhoneDialog(
    visible = showConfirmation,
    onDismissRequest = {
      showConfirmation = false
      onDismissRequest?.invoke()
    },
    curvedText = { openOnPhoneDialogCurvedText(text = openOnPhoneText, style = curvedTextStyle) },
    // TODO: Missing content description for OpenOnPhoneDialog.
    //  We add this modifier to workaround the feedback of TalkBack.
    modifier = Modifier.clearAndSetSemantics { contentDescription = openOnPhoneText },
    properties = DialogProperties(windowTitle = DEFAULT_WINDOW_TITLE_FOR_DIALOG),
  )
}

const val DEFAULT_WINDOW_TITLE_FOR_DIALOG = " "
