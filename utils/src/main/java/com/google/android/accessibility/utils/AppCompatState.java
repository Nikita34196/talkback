/*
 * Copyright 2024 Nikita
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.google.android.accessibility.utils;

/**
 * Global state for app-specific accessibility fixes.
 * Used to communicate between TalkBack modules (gesture controller, focus logic)
 * about which app is currently in the foreground.
 *
 * <p>This is intentionally simple static state — it's set by TalkBack's gesture controller
 * and read by the traversal/focus logic in the utils module.
 */
public final class AppCompatState {

  private static volatile boolean maxMessengerActive = false;

  private AppCompatState() {}

  /** Called by GestureController when Max messenger is detected in foreground. */
  public static void setMaxMessengerActive(boolean active) {
    maxMessengerActive = active;
  }

  /** Returns true if Max messenger is currently in foreground. */
  public static boolean isMaxMessengerActive() {
    return maxMessengerActive;
  }
}
