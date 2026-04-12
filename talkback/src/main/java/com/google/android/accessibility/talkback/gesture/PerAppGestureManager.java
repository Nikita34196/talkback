/*
 * Copyright 2024 Nikita
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.google.android.accessibility.talkback.gesture;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;

/**
 * Manages per-application gesture overrides.
 *
 * <p>Allows users to assign different gesture actions for specific apps.
 * For example, swipe-right in a messenger could mean "next chat" instead of "next element".
 *
 * <p>Gesture overrides are stored in SharedPreferences as JSON maps per package name.
 * Format: { "gestureId": "actionKey", ... }
 */
public class PerAppGestureManager {

  private static final String TAG = "PerAppGestureManager";
  private static final String PREFS_NAME = "per_app_gestures";
  private static final String KEY_ENABLED_APPS = "enabled_apps";

  private final SharedPreferences prefs;
  // Cache: packageName -> (gestureId -> actionKey)
  private final Map<String, Map<Integer, String>> cache = new HashMap<>();

  public PerAppGestureManager(@NonNull Context context) {
    prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  /**
   * Checks if there is a gesture override for the given app and gesture.
   *
   * @param packageName The foreground app's package name
   * @param gestureId The gesture ID (from AccessibilityService gesture constants)
   * @return The action key string, or null if no override exists
   */
  @Nullable
  public String getGestureOverride(@NonNull String packageName, int gestureId) {
    Map<Integer, String> appGestures = getAppGestures(packageName);
    if (appGestures == null) {
      return null;
    }
    return appGestures.get(gestureId);
  }

  /**
   * Sets a gesture override for a specific app.
   *
   * @param packageName The app's package name
   * @param gestureId The gesture ID
   * @param actionKey The action key to assign (e.g., "next_element", "scroll_down")
   */
  public void setGestureOverride(
      @NonNull String packageName, int gestureId, @NonNull String actionKey) {
    Map<Integer, String> appGestures = getAppGestures(packageName);
    if (appGestures == null) {
      appGestures = new HashMap<>();
    }
    appGestures.put(gestureId, actionKey);
    cache.put(packageName, appGestures);
    saveAppGestures(packageName, appGestures);
    addToEnabledApps(packageName);
  }

  /**
   * Removes a gesture override for a specific app.
   */
  public void removeGestureOverride(@NonNull String packageName, int gestureId) {
    Map<Integer, String> appGestures = getAppGestures(packageName);
    if (appGestures == null) {
      return;
    }
    appGestures.remove(gestureId);
    if (appGestures.isEmpty()) {
      removeAllOverrides(packageName);
    } else {
      cache.put(packageName, appGestures);
      saveAppGestures(packageName, appGestures);
    }
  }

  /**
   * Removes all gesture overrides for an app.
   */
  public void removeAllOverrides(@NonNull String packageName) {
    cache.remove(packageName);
    prefs.edit().remove(packageName).apply();
    removeFromEnabledApps(packageName);
  }

  /**
   * Returns true if the given app has any gesture overrides.
   */
  public boolean hasOverrides(@NonNull String packageName) {
    Map<Integer, String> appGestures = getAppGestures(packageName);
    return appGestures != null && !appGestures.isEmpty();
  }

  /**
   * Returns the set of all apps that have gesture overrides.
   */
  @NonNull
  public Set<String> getEnabledApps() {
    return prefs.getStringSet(KEY_ENABLED_APPS, new HashSet<>());
  }

  /**
   * Returns all gesture overrides for the given app, or null if none.
   */
  @Nullable
  private Map<Integer, String> getAppGestures(@NonNull String packageName) {
    // Check cache first
    if (cache.containsKey(packageName)) {
      return cache.get(packageName);
    }

    // Load from prefs
    String json = prefs.getString(packageName, null);
    if (TextUtils.isEmpty(json)) {
      return null;
    }

    try {
      JSONObject obj = new JSONObject(json);
      Map<Integer, String> gestures = new HashMap<>();
      Iterator<String> keys = obj.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        gestures.put(Integer.parseInt(key), obj.getString(key));
      }
      cache.put(packageName, gestures);
      return gestures;
    } catch (JSONException | NumberFormatException e) {
      Log.e(TAG, "Error parsing gestures for " + packageName, e);
      return null;
    }
  }

  private void saveAppGestures(
      @NonNull String packageName, @NonNull Map<Integer, String> gestures) {
    JSONObject obj = new JSONObject();
    try {
      for (Map.Entry<Integer, String> entry : gestures.entrySet()) {
        obj.put(String.valueOf(entry.getKey()), entry.getValue());
      }
      prefs.edit().putString(packageName, obj.toString()).apply();
    } catch (JSONException e) {
      Log.e(TAG, "Error saving gestures for " + packageName, e);
    }
  }

  private void addToEnabledApps(@NonNull String packageName) {
    Set<String> apps = new HashSet<>(getEnabledApps());
    apps.add(packageName);
    prefs.edit().putStringSet(KEY_ENABLED_APPS, apps).apply();
  }

  private void removeFromEnabledApps(@NonNull String packageName) {
    Set<String> apps = new HashSet<>(getEnabledApps());
    apps.remove(packageName);
    prefs.edit().putStringSet(KEY_ENABLED_APPS, apps).apply();
  }
}
