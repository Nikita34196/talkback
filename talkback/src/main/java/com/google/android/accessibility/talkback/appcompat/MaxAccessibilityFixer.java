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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
  private static final long SCAN_CACHE_MS = 1500;

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

    /** Key for deduplication: bounds rectangle as string. */
    public String boundsKey() {
      return bounds.left + "," + bounds.top + "," + bounds.right + "," + bounds.bottom;
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

  /** Force rescan on next call. */
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
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null) return hiddenNodes;
    if (!PACKAGE_NAME.equals(
        root.getPackageName() != null ? root.getPackageName().toString() : "")) {
      root.recycle();
      return hiddenNodes;
    }

    List<NodeInfo> raw = new ArrayList<>();
    deepScan(root, 0, raw);
    root.recycle();

    // Deduplicate: if two nodes have overlapping bounds, keep the more specific one (smaller)
    deduplicateNodes(raw);

    // Sort by position: top to bottom, left to right
    hiddenNodes.sort((a, b) -> {
      if (Math.abs(a.bounds.top - b.bounds.top) > 30) {
        return Integer.compare(a.bounds.top, b.bounds.top);
      }
      return Integer.compare(a.bounds.left, b.bounds.left);
    });

    lastScanTime = now;
    Log.d(TAG, "Found " + hiddenNodes.size() + " unique hidden elements in Max");
    for (int i = 0; i < hiddenNodes.size(); i++) {
      NodeInfo n = hiddenNodes.get(i);
      Log.d(TAG, "  " + (i+1) + ": " + n.label + " at " + n.bounds + " [" + n.className + "]");
    }
    return hiddenNodes;
  }

  private void deepScan(AccessibilityNodeInfo node, int depth, List<NodeInfo> results) {
    if (node == null || depth > 30) return;
    if (isInteractiveElement(node) && node.isVisibleToUser()) {
      String className = node.getClassName() != null ? node.getClassName().toString() : "";
      // Check if TalkBack would normally skip this element
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
              guessLabel(node, className, bounds), bounds));
        }
      }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfo child = node.getChild(i);
      if (child != null) {
        deepScan(child, depth + 1, results);
      }
    }
  }

  /**
   * Removes duplicate/overlapping nodes. If two nodes overlap by >70%,
   * keep the smaller (more specific) one.
   */
  private void deduplicateNodes(List<NodeInfo> raw) {
    Set<Integer> removed = new HashSet<>();
    for (int i = 0; i < raw.size(); i++) {
      if (removed.contains(i)) continue;
      for (int j = i + 1; j < raw.size(); j++) {
        if (removed.contains(j)) continue;
        Rect a = raw.get(i).bounds;
        Rect b = raw.get(j).bounds;
        if (overlapsSignificantly(a, b)) {
          // Keep the smaller one (more specific)
          int areaA = a.width() * a.height();
          int areaB = b.width() * b.height();
          if (areaA <= areaB) {
            removed.add(j);
          } else {
            removed.add(i);
            break;
          }
        }
      }
    }
    for (int i = 0; i < raw.size(); i++) {
      if (!removed.contains(i)) {
        hiddenNodes.add(raw.get(i));
      }
    }
  }

  private boolean overlapsSignificantly(Rect a, Rect b) {
    int overlapLeft = Math.max(a.left, b.left);
    int overlapTop = Math.max(a.top, b.top);
    int overlapRight = Math.min(a.right, b.right);
    int overlapBottom = Math.min(a.bottom, b.bottom);
    if (overlapLeft >= overlapRight || overlapTop >= overlapBottom) return false;
    int overlapArea = (overlapRight - overlapLeft) * (overlapBottom - overlapTop);
    int smallerArea = Math.min(a.width() * a.height(), b.width() * b.height());
    return smallerArea > 0 && (float) overlapArea / smallerArea > 0.7f;
  }

  private boolean isInteractiveElement(AccessibilityNodeInfo node) {
    if (node.isClickable() || node.isLongClickable() || node.isEditable()) return true;
    String cn = node.getClassName() != null ? node.getClassName().toString() : "";
    return cn.contains("EditText") || cn.contains("ImageButton");
  }

  // =========================================================================
  // Label guessing
  // =========================================================================

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

    // Guess by screen position using the screenshot layout:
    // Bottom bar: [emoji] [input field] [attach] [camera] [mic/send]
    Rect screen = getScreenBounds();
    int sh = screen.height();
    int sw = screen.width();

    if (bounds.top > sh * 0.88) {
      if (className.contains("ImageButton") || className.contains("ImageView")
          || node.isClickable()) {
        float relX = (float) bounds.centerX() / sw;
        if (relX < 0.12) return "Эмодзи";
        if (relX > 0.88) return "Голосовое сообщение";
        if (relX > 0.75) return "Камера";
        if (relX > 0.60) return "Прикрепить файл";
        return "Кнопка ввода";
      }
    }

    if (className.contains("ImageButton")) return "Кнопка";
    if (className.contains("ImageView") && node.isClickable()) return "Кнопка-изображение";
    return "Элемент";
  }

  private Rect getScreenBounds() {
    Rect screen = new Rect();
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root != null) {
      root.getBoundsInScreen(screen);
      root.recycle();
    }
    if (screen.width() <= 0) screen.set(0, 0, 1080, 2340);
    return screen;
  }

  // =========================================================================
  // Navigation
  // =========================================================================

  @Nullable
  public String navigateNextHidden() {
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) return "Скрытых элементов не найдено";
    currentHiddenIndex = (currentHiddenIndex + 1) % nodes.size();
    return announceAndFocus();
  }

  @Nullable
  public String navigatePreviousHidden() {
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) return "Скрытых элементов не найдено";
    currentHiddenIndex--;
    if (currentHiddenIndex < 0) currentHiddenIndex = nodes.size() - 1;
    return announceAndFocus();
  }

  private String announceAndFocus() {
    NodeInfo info = hiddenNodes.get(currentHiddenIndex);
    // Try multiple ways to focus the element
    info.node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
    return (currentHiddenIndex + 1) + " из " + hiddenNodes.size() + ": " + info.label;
  }

  // =========================================================================
  // Interaction
  // =========================================================================

  @Nullable
  public String clickCurrentHidden() {
    if (currentHiddenIndex < 0 || currentHiddenIndex >= hiddenNodes.size()) {
      return "Сначала выберите элемент свайпом";
    }
    NodeInfo info = hiddenNodes.get(currentHiddenIndex);
    boolean clicked = info.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    if (!clicked) clicked = tapAtCenter(info.bounds);
    // Invalidate cache since UI may have changed
    invalidateCache();
    return clicked ? info.label + ", нажато" : "Не удалось нажать " + info.label;
  }

  @Nullable
  public String focusInputField() {
    // Always rescan when looking for input
    invalidateCache();
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
    // ALWAYS rescan because after typing text, mic changes to send
    invalidateCache();
    // Small delay to let UI update
    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
    invalidateCache();
    List<NodeInfo> nodes = scanForHiddenElements();

    // Pass 1: explicit send in ID
    for (int i = 0; i < nodes.size(); i++) {
      NodeInfo info = nodes.get(i);
      String id = info.node.getViewIdResourceName();
      if (id != null && id.toLowerCase().contains("send")) {
        currentHiddenIndex = i;
        return clickNode(info, "Сообщение отправлено");
      }
    }

    // Pass 2: the rightmost button in the bottom bar
    // From the screenshot: [emoji] [input] [attach] [camera] [mic/send]
    // After typing, the rightmost becomes "send"
    Rect screen = getScreenBounds();
    int sh = screen.height();
    int sw = screen.width();

    NodeInfo rightmostBottom = null;
    int rightmostIdx = -1;
    int maxCenterX = 0;
    for (int i = 0; i < nodes.size(); i++) {
      NodeInfo info = nodes.get(i);
      // Skip the text field
      if (info.className.contains("EditText")) continue;
      // Must be in bottom 15% of screen
      if (info.bounds.top > sh * 0.85 && info.bounds.centerX() > maxCenterX) {
        maxCenterX = info.bounds.centerX();
        rightmostBottom = info;
        rightmostIdx = i;
      }
    }
    if (rightmostBottom != null) {
      currentHiddenIndex = rightmostIdx;
      return clickNode(rightmostBottom, "Сообщение отправлено");
    }

    // Pass 3: brute force — try tapping the bottom-right corner area
    int tapX = (int)(sw * 0.93);
    int tapY = (int)(sh * 0.95);
    Rect fakeBounds = new Rect(tapX - 20, tapY - 20, tapX + 20, tapY + 20);
    boolean tapped = tapAtCenter(fakeBounds);
    return tapped ? "Попытка отправить по координатам" : "Кнопка отправки не найдена";
  }

  @NonNull
  private String clickNode(NodeInfo info, String successMsg) {
    boolean clicked = info.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    if (!clicked) clicked = tapAtCenter(info.bounds);
    invalidateCache();
    return clicked ? successMsg : "Не удалось нажать";
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
    sb.append(". Свайп 3 пальцами вправо влево для навигации. Вниз нажать. Вверх отправить.");
    return sb.toString();
  }

  // =========================================================================
  // Coordinate tap
  // =========================================================================

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
  // Fallback
  // =========================================================================

  @Nullable
  private String findAndFocusAnyEditText() {
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null) return null;
    AccessibilityNodeInfo et = findFirst(root, "EditText", 0);
    if (et != null) {
      et.performAction(AccessibilityNodeInfo.ACTION_CLICK);
      Rect b = new Rect();
      et.getBoundsInScreen(b);
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
