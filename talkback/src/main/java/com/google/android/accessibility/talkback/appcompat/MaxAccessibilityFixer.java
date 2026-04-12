/*
 * Copyright 2024 Nikita
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.google.android.accessibility.talkback.appcompat;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fixes accessibility issues in Max messenger (ru.oneme.app).
 * Deep-scans the node tree, finds hidden elements, provides navigation
 * and interaction via coordinate-based clicking.
 */
public class MaxAccessibilityFixer {

  private static final String TAG = "MaxAccessibilityFixer";
  public static final String PACKAGE_NAME = "ru.oneme.app";

  private final AccessibilityService service;
  private final List<NodeInfo> hiddenNodes = new ArrayList<>();
  private int currentHiddenIndex = -1;
  private long lastScanTime = 0;
  private static final long SCAN_CACHE_MS = 2000;

  public static class NodeInfo {
    public final AccessibilityNodeInfo node;
    public final String className;
    public final String label;
    public final Rect bounds;

    NodeInfo(AccessibilityNodeInfo node, String className, String label, Rect bounds) {
      this.node = node;
      this.className = className;
      this.label = label;
      this.bounds = bounds;
    }
  }

  public MaxAccessibilityFixer(@NonNull AccessibilityService service) {
    this.service = service;
  }

  public boolean isMaxInForeground() {
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null) return false;
    boolean isMax = PACKAGE_NAME.equals(
        root.getPackageName() != null ? root.getPackageName().toString() : "");
    root.recycle();
    return isMax;
  }

  @NonNull
  public List<NodeInfo> scanForHiddenElements() {
    long now = System.currentTimeMillis();
    if (now - lastScanTime < SCAN_CACHE_MS && !hiddenNodes.isEmpty()) {
      return hiddenNodes;
    }
    clearCache();
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null) return hiddenNodes;
    if (!PACKAGE_NAME.equals(
        root.getPackageName() != null ? root.getPackageName().toString() : "")) {
      root.recycle();
      return hiddenNodes;
    }
    deepScan(root, 0);
    root.recycle();
    lastScanTime = now;
    Log.d(TAG, "Found " + hiddenNodes.size() + " hidden elements in Max");
    return hiddenNodes;
  }

  private void deepScan(AccessibilityNodeInfo node, int depth) {
    if (node == null || depth > 30) return;
    if (isInteractiveElement(node) && node.isVisibleToUser()) {
      String className = node.getClassName() != null ? node.getClassName().toString() : "";
      boolean isAccessible = node.isScreenReaderFocusable()
          || (node.isClickable() && node.isFocusable()
              && !TextUtils.isEmpty(node.getContentDescription()));
      if (!isAccessible || (className.contains("EditText") && node.isEditable())) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.width() > 10 && bounds.height() > 10) {
          hiddenNodes.add(new NodeInfo(
              AccessibilityNodeInfo.obtain(node), className,
              guessLabel(node, className, bounds), bounds));
        }
      }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfo child = node.getChild(i);
      if (child != null) { deepScan(child, depth + 1); }
    }
  }

  private boolean isInteractiveElement(AccessibilityNodeInfo node) {
    if (node.isClickable() || node.isLongClickable() || node.isEditable()) return true;
    String cn = node.getClassName() != null ? node.getClassName().toString() : "";
    return cn.contains("EditText") || cn.contains("ImageButton");
  }

  @NonNull
  private String guessLabel(AccessibilityNodeInfo node, String className, Rect bounds) {
    CharSequence desc = node.getContentDescription();
    if (!TextUtils.isEmpty(desc)) return desc.toString();
    CharSequence hint = node.getHintText();
    if (!TextUtils.isEmpty(hint)) return hint.toString();
    CharSequence text = node.getText();
    if (!TextUtils.isEmpty(text)) return text.toString();

    String viewId = node.getViewIdResourceName();
    if (!TextUtils.isEmpty(viewId)) {
      String id = viewId.toLowerCase();
      if (id.contains("send")) return "Отправить";
      if (id.contains("attach") || id.contains("clip")) return "Прикрепить файл";
      if (id.contains("voice") || id.contains("mic") || id.contains("audio")) return "Голосовое сообщение";
      if (id.contains("emoji") || id.contains("smile") || id.contains("sticker")) return "Эмодзи";
      if (id.contains("camera") || id.contains("photo")) return "Камера";
      if (id.contains("input") || id.contains("edit") || id.contains("message") || id.contains("compose")) return "Написать сообщение";
      if (id.contains("record")) return "Записать";
      if (id.contains("call")) return "Позвонить";
      if (id.contains("search")) return "Поиск";
      if (id.contains("back")) return "Назад";
      if (id.contains("menu") || id.contains("more")) return "Меню";
      if (id.contains("play") || id.contains("pause")) return "Воспроизвести";
    }

    if (className.contains("EditText")) return "Поле ввода сообщения";

    // Guess by screen position
    Rect screen = new Rect();
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root != null) { root.getBoundsInScreen(screen); root.recycle(); }
    int sh = screen.height() > 0 ? screen.height() : 2000;
    int sw = screen.width() > 0 ? screen.width() : 1080;
    if (bounds.top > sh * 0.8) {
      if (className.contains("ImageButton") || className.contains("ImageView")) {
        if (bounds.centerX() > sw * 0.8) return "Отправить";
        if (bounds.centerX() < sw * 0.2) return "Прикрепить";
        return "Кнопка ввода";
      }
    }

    if (className.contains("ImageButton")) return "Кнопка";
    if (className.contains("ImageView") && node.isClickable()) return "Кнопка-изображение";
    return "Элемент";
  }

  // === Navigation ===

  @Nullable
  public String navigateNextHidden() {
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) return null;
    currentHiddenIndex = (currentHiddenIndex + 1) % nodes.size();
    NodeInfo info = nodes.get(currentHiddenIndex);
    info.node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
    return (currentHiddenIndex + 1) + " из " + nodes.size() + ": " + info.label;
  }

  @Nullable
  public String navigatePreviousHidden() {
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) return null;
    currentHiddenIndex--;
    if (currentHiddenIndex < 0) currentHiddenIndex = nodes.size() - 1;
    NodeInfo info = nodes.get(currentHiddenIndex);
    info.node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
    return (currentHiddenIndex + 1) + " из " + nodes.size() + ": " + info.label;
  }

  // === Interaction ===

  @Nullable
  public String clickCurrentHidden() {
    if (currentHiddenIndex < 0 || currentHiddenIndex >= hiddenNodes.size()) return null;
    NodeInfo info = hiddenNodes.get(currentHiddenIndex);
    boolean clicked = info.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    if (!clicked) clicked = tapAtCenter(info.bounds);
    return clicked ? info.label + ", нажато" : "Не удалось нажать";
  }

  @Nullable
  public String focusInputField() {
    List<NodeInfo> nodes = scanForHiddenElements();
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).className.contains("EditText")) {
        currentHiddenIndex = i;
        NodeInfo info = nodes.get(i);
        boolean ok = info.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!ok) ok = info.node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        if (!ok) tapAtCenter(info.bounds);
        return info.label;
      }
    }
    return findAndFocusAnyEditText();
  }

  @Nullable
  public String clickSendButton() {
    // Force rescan
    lastScanTime = 0;
    List<NodeInfo> nodes = scanForHiddenElements();

    // By ID or label
    for (int i = 0; i < nodes.size(); i++) {
      NodeInfo info = nodes.get(i);
      String id = info.node.getViewIdResourceName();
      if ((id != null && id.toLowerCase().contains("send"))
          || info.label.equals("Отправить")) {
        currentHiddenIndex = i;
        boolean clicked = info.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!clicked) clicked = tapAtCenter(info.bounds);
        return clicked ? "Сообщение отправлено" : "Не удалось отправить";
      }
    }

    // Rightmost button in bottom area
    Rect screen = new Rect();
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root != null) { root.getBoundsInScreen(screen); root.recycle(); }
    int sh = screen.height() > 0 ? screen.height() : 2000;
    int sw = screen.width() > 0 ? screen.width() : 1080;

    NodeInfo best = null;
    int bestIdx = -1;
    int maxX = 0;
    for (int i = 0; i < nodes.size(); i++) {
      NodeInfo info = nodes.get(i);
      if (!info.className.contains("EditText")
          && info.bounds.top > sh * 0.75
          && info.bounds.centerX() > maxX) {
        maxX = info.bounds.centerX();
        best = info;
        bestIdx = i;
      }
    }
    if (best != null) {
      currentHiddenIndex = bestIdx;
      boolean clicked = best.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
      if (!clicked) clicked = tapAtCenter(best.bounds);
      return clicked ? "Сообщение отправлено" : "Не удалось отправить";
    }
    return null;
  }

  @NonNull
  public String getHiddenElementsSummary() {
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) return "Скрытых элементов не найдено";
    StringBuilder sb = new StringBuilder();
    sb.append(nodes.size()).append(" скрытых элементов. ");
    for (int i = 0; i < nodes.size(); i++) {
      sb.append(i + 1).append(": ").append(nodes.get(i).label);
      if (i < nodes.size() - 1) sb.append(". ");
    }
    sb.append(". Свайп тремя пальцами для навигации, вниз для нажатия, вверх для отправки.");
    return sb.toString();
  }

  // === Coordinate tap ===

  private boolean tapAtCenter(Rect bounds) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
    int x = bounds.centerX();
    int y = bounds.centerY();
    if (x <= 0 || y <= 0) return false;
    Log.d(TAG, "Tap at " + x + "," + y);
    Path path = new Path();
    path.moveTo(x, y);
    GestureDescription gesture = new GestureDescription.Builder()
        .addStroke(new GestureDescription.StrokeDescription(path, 0, 100))
        .build();
    return service.dispatchGesture(gesture, null, null);
  }

  // === Fallback ===

  @Nullable
  private String findAndFocusAnyEditText() {
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null) return null;
    AccessibilityNodeInfo et = findFirst(root, "EditText", 0);
    if (et != null) {
      et.performAction(AccessibilityNodeInfo.ACTION_CLICK);
      Rect b = new Rect(); et.getBoundsInScreen(b);
      tapAtCenter(b);
      root.recycle();
      return "Поле ввода сообщения";
    }
    root.recycle();
    return null;
  }

  @Nullable
  private AccessibilityNodeInfo findFirst(AccessibilityNodeInfo node, String cls, int depth) {
    if (node == null || depth > 30) return null;
    String cn = node.getClassName() != null ? node.getClassName().toString() : "";
    if (cn.contains(cls) && node.isVisibleToUser()) return AccessibilityNodeInfo.obtain(node);
    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfo child = node.getChild(i);
      if (child != null) {
        AccessibilityNodeInfo f = findFirst(child, cls, depth + 1);
        if (f != null) { child.recycle(); return f; }
        child.recycle();
      }
    }
    return null;
  }

  public void clearCache() {
    for (NodeInfo info : hiddenNodes) {
      try { info.node.recycle(); } catch (Exception ignored) {}
    }
    hiddenNodes.clear();
    currentHiddenIndex = -1;
  }
}
