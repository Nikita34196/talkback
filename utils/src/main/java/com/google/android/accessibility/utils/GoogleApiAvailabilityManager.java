/*
 * Copyright (C) 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.accessibility.utils;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * A singleton class for verifying Google Play services availability by {@link
 * GoogleApiAvailability}.
 */
public class GoogleApiAvailabilityManager {

  @SuppressWarnings("NonFinalStaticField")
  private static GoogleApiAvailabilityManager instance;

  public static GoogleApiAvailabilityManager getInstance() {
    if (instance == null) {
      instance = new GoogleApiAvailabilityManager(GoogleApiAvailability.getInstance());
    }
    return instance;
  }

  /**
   * Sets a fake GoogleApiAvailable {@link
   * com.google.android.gms.common.testing.FakeGoogleApiAvailability} for test. This should be
   * invoked before the first APIs are created.
   */
  @VisibleForTesting
  public static void initializeForTest(GoogleApiAvailability googleApiAvailability) {
    instance = new GoogleApiAvailabilityManager(googleApiAvailability);
  }

  @VisibleForTesting
  public static void stopInstanceForTest() {
    instance = null;
  }

  private final GoogleApiAvailability googleApiAvailability;

  private GoogleApiAvailabilityManager(GoogleApiAvailability googleApiAvailability) {
    this.googleApiAvailability = googleApiAvailability;
  }

  /**
   * Verifies that Google Play services is installed and enabled on this device.
   *
   * @param minApkVersion the installed version is not older than the specified version
   */
  public int isGooglePlayServicesAvailable(Context context, int minApkVersion) {
    return googleApiAvailability.isGooglePlayServicesAvailable(context, minApkVersion);
  }
}
