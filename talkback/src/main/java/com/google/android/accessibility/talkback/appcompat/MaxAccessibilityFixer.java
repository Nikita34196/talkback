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
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fixes accessibility issues in Max messenger (ru.oneme.app).
 * Finds hidden elements RELATIVE to the input field, not by absolute coordinates.
 * Works correctly even when keyboard is open.
 */
public class MaxAccessibilityFixer {

  private static final String TAG = "MaxAccessibilityFixer";
  public static final String PACKAGE_NAME = "ru.oneme.app";

  private final AccessibilityService service;
  private final List<NodeInfo> hiddenNodes = new ArrayList<>();
  private int currentHiddenIndex = -1;
  private long lastScanTime = 0;
  private static final long SCAN_CACHE_MS = 10000; // 10 seconds cache while navigating

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

  /**
   * Checks if Max is in foreground by scanning ALL windows.
   * This works even when keyboard is on top.
   */
  public boolean isMaxInForeground() {
    for (AccessibilityWindowInfo window : service.getWindows()) {
      AccessibilityNodeInfo root = window.getRoot();
      if (root != null) {
        CharSequence pkg = root.getPackageName();
        root.recycle();
        if (PACKAGE_NAME.equals(pkg != null ? pkg.toString() : "")) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Gets the root of the Max app window (not keyboard).
   */
  @Nullable
  private AccessibilityNodeInfo getMaxRoot() {
    for (AccessibilityWindowInfo window : service.getWindows()) {
      AccessibilityNodeInfo root = window.getRoot();
      if (root != null) {
        CharSequence pkg = root.getPackageName();
        if (PACKAGE_NAME.equals(pkg != null ? pkg.toString() : "")) {
          return root;
        }
        root.recycle();
      }
    }
    return null;
  }

  public void invalidateCache() {
    lastScanTime = 0;
  }

  @NonNull
  public List<NodeInfo> scanForHiddenElements() {
    long now = System.currentTimeMillis();
    if (now - lastScanTime < SCAN_CACHE_MS && !hiddenNodes.isEmpty()) {
      return hiddenNodes;
    }
    clearCache();

    AccessibilityNodeInfo root = getMaxRoot();
    if (root == null) return hiddenNodes;

    // Step 1: Find the EditText (input field) — our anchor point
    Rect editTextBounds = new Rect();
    AccessibilityNodeInfo editText = findFirst(root, "EditText", 0);
    if (editText != null) {
      editText.getBoundsInScreen(editTextBounds);
    }

    // Step 2: Deep scan for all hidden interactive elements
    List<NodeInfo> raw = new ArrayList<>();
    deepScan(root, 0, raw, editTextBounds);
    root.recycle();

    // Step 3: Deduplicate overlapping nodes
    deduplicateNodes(raw);

    // Step 4: Sort — elements near EditText first, then left to right
    hiddenNodes.sort((a, b) -> {
      // Same row (within 30px of each other vertically)
      if (Math.abs(a.bounds.centerY() - b.bounds.centerY()) < 30) {
        return Integer.compare(a.bounds.left, b.bounds.left);
      }
      return Integer.compare(a.bounds.top, b.bounds.top);
    });

    lastScanTime = now;
    Log.d(TAG, "Found " + hiddenNodes.size() + " hidden elements:");
    for (int i = 0; i < hiddenNodes.size(); i++) {
      NodeInfo n = hiddenNodes.get(i);
      Log.d(TAG, "  " + (i+1) + ": " + n.label + " " + n.bounds);
    }
    return hiddenNodes;
  }

  private void deepScan(AccessibilityNodeInfo node, int depth,
      List<NodeInfo> results, Rect editTextBounds) {
    if (node == null || depth > 30) return;
    if (isInteractiveElement(node) && node.isVisibleToUser()) {
      String className = node.getClassName() != null ? node.getClassName().toString() : "";
      boolean hasGoodLabel = !TextUtils.isEmpty(node.getContentDescription())
          || !TextUtils.isEmpty(node.getText());
      boolean isScreenReaderVisible = node.isScreenReaderFocusable()
          || (node.isClickable() && node.isFocusable() && hasGoodLabel);
      boolean isHiddenEditText = className.contains("EditText") && node.isEditable();

      if (!isScreenReaderVisible || isHiddenEditText) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.width() > 20 && bounds.height() > 20) {
          results.add(new NodeInfo(
              AccessibilityNodeInfo.obtain(node), className,
              guessLabelRelative(node, className, bounds, editTextBounds), bounds));
        }
      }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfo child = node.getChild(i);
      if (child != null) deepScan(child, depth + 1, results, editTextBounds);
    }
  }

  private boolean isInteractiveElement(AccessibilityNodeInfo node) {
    if (node.isClickable() || node.isLongClickable() || node.isEditable()) return true;
    String cn = node.getClassName() != null ? node.getClassName().toString() : "";
    return cn.contains("EditText") || cn.contains("ImageButton");
  }

  private void deduplicateNodes(List<NodeInfo> raw) {
    Set<Integer> removed = new HashSet<>();
    for (int i = 0; i < raw.size(); i++) {
      if (removed.contains(i)) continue;
      for (int j = i + 1; j < raw.size(); j++) {
        if (removed.contains(j)) continue;
        if (overlaps(raw.get(i).bounds, raw.get(j).bounds)) {
          int areaA = raw.get(i).bounds.width() * raw.get(i).bounds.height();
          int areaB = raw.get(j).bounds.width() * raw.get(j).bounds.height();
          removed.add(areaA <= areaB ? j : i);
          if (areaA > areaB) break;
        }
      }
    }
    for (int i = 0; i < raw.size(); i++) {
      if (!removed.contains(i)) hiddenNodes.add(raw.get(i));
    }
  }

  private boolean overlaps(Rect a, Rect b) {
    int oL = Math.max(a.left, b.left);
    int oT = Math.max(a.top, b.top);
    int oR = Math.min(a.right, b.right);
    int oB = Math.min(a.bottom, b.bottom);
    if (oL >= oR || oT >= oB) return false;
    int oa = (oR - oL) * (oB - oT);
    int sa = Math.min(a.width() * a.height(), b.width() * b.height());
    return sa > 0 && (float) oa / sa > 0.65f;
  }

  // =========================================================================
  // Label guessing — RELATIVE to EditText position
  // =========================================================================

  @NonNull
  private String guessLabelRelative(AccessibilityNodeInfo node, String className,
      Rect bounds, Rect editTextBounds) {
    // Try actual labels first
    CharSequence desc = node.getContentDescription();
    if (!TextUtils.isEmpty(desc)) return desc.toString();
    CharSequence hint = node.getHintText();
    if (!TextUtils.isEmpty(hint)) return hint.toString();
    CharSequence text = node.getText();
    if (!TextUtils.isEmpty(text)) return text.toString();

    // Try view ID
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

    // Guess by position RELATIVE to EditText (works with keyboard open)
    if (editTextBounds != null && editTextBounds.height() > 0) {
      int editY = editTextBounds.centerY();
      boolean sameRow = Math.abs(bounds.centerY() - editY) < 60;

      if (sameRow && (className.contains("Image") || node.isClickable())) {
        // Same row as input field:
        // Layout: [emoji] [EditText] [attach] [camera] [mic/send]
        if (bounds.right <= editTextBounds.left) {
          return "Эмодзи";
        }
        if (bounds.left >= editTextBounds.right) {
          // Count how many buttons are to the right
          // The rightmost = mic/send, then camera, then attach
          // Use relative position
          int distFromRight = getScreenWidth() - bounds.centerX();
          if (distFromRight < getScreenWidth() * 0.1) return "Голосовое сообщение";
          if (distFromRight < getScreenWidth() * 0.2) return "Камера";
          return "Прикрепить файл";
        }
      }
    }

    if (className.contains("ImageButton")) return "Кнопка";
    if (className.contains("ImageView") && node.isClickable()) return "Кнопка-изображение";
    return "Элемент";
  }

  private int getScreenWidth() {
    Rect screen = new Rect();
    AccessibilityNodeInfo root = getMaxRoot();
    if (root != null) { root.getBoundsInScreen(screen); root.recycle(); }
    return screen.width() > 0 ? screen.width() : 1080;
  }

  // =========================================================================
  // Navigation
  // =========================================================================

  @Nullable
  public String navigateNextHidden() {
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) return "Скрытых элементов не найдено";
    // Clamp index if list changed size
    if (currentHiddenIndex >= nodes.size()) currentHiddenIndex = -1;
    currentHiddenIndex = (currentHiddenIndex + 1) % nodes.size();
    return announceAndFocus();
  }

  @Nullable
  public String navigatePreviousHidden() {
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) return "Скрытых элементов не найдено";
    if (currentHiddenIndex >= nodes.size()) currentHiddenIndex = nodes.size();
    currentHiddenIndex--;
    if (currentHiddenIndex < 0) currentHiddenIndex = nodes.size() - 1;
    return announceAndFocus();
  }

  private String announceAndFocus() {
    if (currentHiddenIndex < 0 || currentHiddenIndex >= hiddenNodes.size()) {
      return "Ошибка навигации";
    }
    NodeInfo info = hiddenNodes.get(currentHiddenIndex);
    info.node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
    return (currentHiddenIndex + 1) + " из " + hiddenNodes.size() + ": " + info.label;
  }

  // =========================================================================
  // Interaction
  // =========================================================================

  @Nullable
  public String clickCurrentHidden() {
    if (currentHiddenIndex < 0 || currentHiddenIndex >= hiddenNodes.size()) {
      return "Сначала выберите элемент свайпом 3 пальца";
    }
    NodeInfo info = hiddenNodes.get(currentHiddenIndex);
    boolean clicked = info.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    if (!clicked) clicked = tapAtCenter(info.bounds);
    invalidateCache();
    return clicked ? info.label + ", нажато" : "Не удалось нажать";
  }

  @Nullable
  public String focusInputField() {
    invalidateCache();
    List<NodeInfo> nodes = scanForHiddenElements();
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).className.contains("EditText")) {
        currentHiddenIndex = i;
        NodeInfo info = nodes.get(i);
        // Try clicking the node directly
        boolean ok = info.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!ok) ok = info.node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        if (!ok) tapAtCenter(info.bounds);
        return info.label;
      }
    }
    // Deep fallback
    AccessibilityNodeInfo root = getMaxRoot();
    if (root != null) {
      AccessibilityNodeInfo et = findFirst(root, "EditText", 0);
      if (et != null) {
        et.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        Rect b = new Rect(); et.getBoundsInScreen(b);
        tapAtCenter(b);
        root.recycle();
        return "Поле ввода сообщения";
      }
      root.recycle();
    }
    return null;
  }

  @Nullable
  public String clickSendButton() {
    // MUST rescan because UI changed after typing
    invalidateCache();
    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
    invalidateCache();

    // Find EditText to get its row
    AccessibilityNodeInfo root = getMaxRoot();
    if (root == null) return "Max не найден";

    AccessibilityNodeInfo editText = findFirst(root, "EditText", 0);
    Rect editBounds = new Rect();
    if (editText != null) {
      editText.getBoundsInScreen(editBounds);
    }

    // Rescan
    List<NodeInfo> nodes = scanForHiddenElements();

    // Strategy 1: find button with "send" in ID
    for (int i = 0; i < nodes.size(); i++) {
      String id = nodes.get(i).node.getViewIdResourceName();
      if (id != null && id.toLowerCase().contains("send")) {
        root.recycle();
        return clickNode(nodes.get(i));
      }
    }

    // Strategy 2: rightmost clickable element in the SAME ROW as EditText
    if (editBounds.height() > 0) {
      NodeInfo rightmost = null;
      int maxX = 0;
      for (NodeInfo info : nodes) {
        if (info.className.contains("EditText")) continue;
        boolean sameRow = Math.abs(info.bounds.centerY() - editBounds.centerY()) < 60;
        if (sameRow && info.bounds.centerX() > maxX) {
          maxX = info.bounds.centerX();
          rightmost = info;
        }
      }
      if (rightmost != null) {
        root.recycle();
        return clickNode(rightmost);
      }
    }

    // Strategy 3: tap just to the right of EditText
    if (editBounds.height() > 0) {
      int tapX = editBounds.right + 100;
      int tapY = editBounds.centerY();
      root.recycle();
      Rect target = new Rect(tapX - 20, tapY - 20, tapX + 20, tapY + 20);
      boolean ok = tapAtCenter(target);
      return ok ? "Попытка отправить" : "Кнопка не найдена";
    }

    root.recycle();
    return "Кнопка отправки не найдена";
  }

  @NonNull
  private String clickNode(NodeInfo info) {
    boolean clicked = info.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    if (!clicked) clicked = tapAtCenter(info.bounds);
    invalidateCache();
    return clicked ? "Сообщение отправлено" : "Не удалось отправить";
  }

  @NonNull
  public String getHiddenElementsSummary() {
    invalidateCache();
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) return "Скрытых элементов не найдено";
    StringBuilder sb = new StringBuilder();
    sb.append(nodes.size()).append(" скрытых элементов. ");
    for (int i = 0; i < nodes.size(); i++) {
      sb.append(i + 1).append(": ").append(nodes.get(i).label);
      if (i < nodes.size() - 1) sb.append(". ");
    }
    sb.append(". Свайп 3 пальца для навигации.");
    return sb.toString();
  }

  // =========================================================================
  // Coordinate tap
  // =========================================================================

  /** Public method to tap at the center of given bounds. Used by GestureController. */
  public boolean tapByCoordinates(Rect bounds) {
    return tapAtCenter(bounds);
  }

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

  // =========================================================================
  // Utility
  // =========================================================================

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
    // Do NOT reset currentHiddenIndex here — preserves position during rescan
  }

  /** Full reset: clears cache AND resets navigation position. */
  public void resetNavigation() {
    clearCache();
    currentHiddenIndex = -1;
  }
}
