/*
 * Copyright 2024 Nikita
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.google.android.accessibility.talkback.actor.anthropic;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Describes unlabeled UI elements using Anthropic Claude Vision API.
 * Takes a screenshot bitmap of a UI element and returns a short text description.
 */
public class AnthropicIconDescriber {

  private static final String TAG = "AnthropicIconDescriber";
  private static final String API_URL = "https://api.anthropic.com/v1/messages";
  private static final String API_VERSION = "2023-06-01";
  private static final String MODEL = "claude-sonnet-4-20250514";
  private static final int MAX_IMAGE_SIZE = 256;
  private static final int JPEG_QUALITY = 80;
  private static final int TIMEOUT_MS = 15000;

  public static final String PREF_ANTHROPIC_API_KEY = "pref_anthropic_api_key";
  public static final String PREF_ANTHROPIC_ENABLED = "pref_anthropic_icon_detection_enabled";

  private final Context context;
  private final ExecutorService executor;
  private final Handler mainHandler;

  /** Callback for icon description results. */
  public interface DescriptionCallback {
    void onSuccess(@NonNull String description);
    void onError(@NonNull String errorMessage);
  }

  public AnthropicIconDescriber(@NonNull Context context) {
    this.context = context.getApplicationContext();
    this.executor = Executors.newSingleThreadExecutor();
    this.mainHandler = new Handler(Looper.getMainLooper());
  }

  /** Returns true if the user has enabled Anthropic icon detection and provided an API key. */
  public boolean isEnabled() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean enabled = prefs.getBoolean(PREF_ANTHROPIC_ENABLED, false);
    String apiKey = prefs.getString(PREF_ANTHROPIC_API_KEY, "");
    return enabled && !TextUtils.isEmpty(apiKey);
  }

  /** Returns the stored API key, or empty string. */
  @NonNull
  private String getApiKey() {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return prefs.getString(PREF_ANTHROPIC_API_KEY, "");
  }

  /**
   * Sends a screenshot of a UI element to Claude Vision and returns a short description.
   * The callback is always invoked on the main thread.
   *
   * @param bitmap Screenshot of the UI element
   * @param callback Callback for the result
   */
  public void describeIcon(@NonNull Bitmap bitmap, @NonNull DescriptionCallback callback) {
    if (!isEnabled()) {
      mainHandler.post(() -> callback.onError("Anthropic API не настроен"));
      return;
    }

    executor.execute(() -> {
      try {
        // Resize bitmap to save bandwidth
        Bitmap resized = resizeBitmap(bitmap, MAX_IMAGE_SIZE);

        // Encode to base64 JPEG
        String base64Image = bitmapToBase64(resized);
        if (base64Image == null) {
          mainHandler.post(() -> callback.onError("Не удалось сжать изображение"));
          return;
        }

        // Build and send request
        String result = callAnthropicApi(base64Image);
        mainHandler.post(() -> callback.onSuccess(result));

      } catch (IOException e) {
        Log.e(TAG, "Network error", e);
        mainHandler.post(() -> callback.onError("Ошибка сети: " + e.getMessage()));
      } catch (JSONException e) {
        Log.e(TAG, "JSON error", e);
        mainHandler.post(() -> callback.onError("Ошибка ответа API"));
      } catch (Exception e) {
        Log.e(TAG, "Unexpected error", e);
        mainHandler.post(() -> callback.onError("Ошибка: " + e.getMessage()));
      }
    });
  }

  /**
   * Calls the Anthropic Messages API with an image and returns the text description.
   */
  @NonNull
  private String callAnthropicApi(@NonNull String base64Image)
      throws IOException, JSONException {

    // Build the JSON request body
    JSONObject imageSource = new JSONObject();
    imageSource.put("type", "base64");
    imageSource.put("media_type", "image/jpeg");
    imageSource.put("data", base64Image);

    JSONObject imageContent = new JSONObject();
    imageContent.put("type", "image");
    imageContent.put("source", imageSource);

    JSONObject textContent = new JSONObject();
    textContent.put("type", "text");
    textContent.put("text",
        "This is a screenshot of a UI element (button or icon) from an Android app. "
        + "Describe what this icon/button does in 2-5 words in Russian. "
        + "Reply ONLY with the short label, nothing else. "
        + "Examples: 'Отправить сообщение', 'Назад', 'Меню', 'Поиск', 'Настройки'.");

    JSONArray contentArray = new JSONArray();
    contentArray.put(imageContent);
    contentArray.put(textContent);

    JSONObject userMessage = new JSONObject();
    userMessage.put("role", "user");
    userMessage.put("content", contentArray);

    JSONArray messagesArray = new JSONArray();
    messagesArray.put(userMessage);

    JSONObject requestBody = new JSONObject();
    requestBody.put("model", MODEL);
    requestBody.put("max_tokens", 50);
    requestBody.put("messages", messagesArray);

    // Make HTTP request
    URL url = new URL(API_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    try {
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("x-api-key", getApiKey());
      conn.setRequestProperty("anthropic-version", API_VERSION);
      conn.setDoOutput(true);
      conn.setConnectTimeout(TIMEOUT_MS);
      conn.setReadTimeout(TIMEOUT_MS);

      // Write request
      byte[] body = requestBody.toString().getBytes(StandardCharsets.UTF_8);
      try (OutputStream os = conn.getOutputStream()) {
        os.write(body);
      }

      // Read response
      int responseCode = conn.getResponseCode();
      if (responseCode != 200) {
        String error = readStream(conn.getErrorStream() != null
            ? new BufferedReader(new InputStreamReader(conn.getErrorStream()))
            : null);
        throw new IOException("API error " + responseCode + ": " + error);
      }

      String responseStr = readStream(
          new BufferedReader(new InputStreamReader(conn.getInputStream())));
      JSONObject response = new JSONObject(responseStr);

      // Extract text from response
      JSONArray content = response.getJSONArray("content");
      StringBuilder result = new StringBuilder();
      for (int i = 0; i < content.length(); i++) {
        JSONObject block = content.getJSONObject(i);
        if ("text".equals(block.optString("type"))) {
          result.append(block.getString("text"));
        }
      }

      String description = result.toString().trim();
      if (TextUtils.isEmpty(description)) {
        return "Неизвестный элемент";
      }
      return description;

    } finally {
      conn.disconnect();
    }
  }

  /** Resizes a bitmap keeping aspect ratio, max dimension = maxSize. */
  @NonNull
  private static Bitmap resizeBitmap(@NonNull Bitmap bitmap, int maxSize) {
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    if (width <= maxSize && height <= maxSize) {
      return bitmap;
    }
    float scale = Math.min((float) maxSize / width, (float) maxSize / height);
    int newWidth = Math.round(width * scale);
    int newHeight = Math.round(height * scale);
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
  }

  /** Encodes bitmap to base64 JPEG string. */
  @Nullable
  private static String bitmapToBase64(@NonNull Bitmap bitmap) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)) {
      return null;
    }
    byte[] bytes = baos.toByteArray();
    return Base64.encodeToString(bytes, Base64.NO_WRAP);
  }

  /** Reads all text from a BufferedReader. */
  @NonNull
  private static String readStream(@Nullable BufferedReader reader) throws IOException {
    if (reader == null) return "";
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line);
    }
    reader.close();
    return sb.toString();
  }

  /** Releases resources. Call when TalkBack shuts down. */
  public void shutdown() {
    executor.shutdownNow();
  }
}
