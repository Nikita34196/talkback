/*
 * Copyright 2024 Nikita
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Fragment for Anthropic AI settings (API key, enable/disable). */
public class AnthropicSettingsFragment extends TalkbackBaseFragment {

  public AnthropicSettingsFragment() {
    super(R.xml.anthropic_preferences);
  }

  @Override
  protected CharSequence getTitle() {
    return getText(R.string.pref_anthropic_settings_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Context context = getContext();
    if (context == null) {
      return;
    }

    // Mask the API key input
    EditTextPreference apiKeyPref = findPreference("pref_anthropic_api_key");
    if (apiKeyPref != null) {
      apiKeyPref.setOnBindEditTextListener(
          (EditText editText) -> {
            editText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
          });

      // Show masked key in summary
      SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
      String key = prefs.getString("pref_anthropic_api_key", "");
      if (!key.isEmpty()) {
        String masked = key.substring(0, Math.min(8, key.length())) + "...";
        apiKeyPref.setSummary("Ключ: " + masked);
      }

      apiKeyPref.setOnPreferenceChangeListener(
          (Preference preference, Object newValue) -> {
            String newKey = (String) newValue;
            if (!newKey.isEmpty()) {
              String m = newKey.substring(0, Math.min(8, newKey.length())) + "...";
              apiKeyPref.setSummary("Ключ: " + m);
            } else {
              apiKeyPref.setSummary(R.string.pref_anthropic_api_key_summary);
            }
            return true;
          });
    }
  }
}
