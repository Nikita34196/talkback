/*
 * Copyright 2024 Nikita
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.google.android.accessibility.talkback.appcompat;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * Actively fixes accessibility issues in Max messenger (ru.oneme.app).
 *
 * <p>Max messenger marks some interactive elements (input field, send button, attach button,
 * voice message button) as not important for accessibility, making them invisible to TalkBack.
 *
 * <p>This fixer:
 * <ul>
 *   <li>Scans the FULL node tree including hidden elements</li>
 *   <li>Finds EditText, ImageButton and other interactive elements</li>
 *   <li>Provides methods to focus and interact with them</li>
 *   <li>Overrides navigation to include hidden elements</li>
 * </ul>
 */
public class MaxAccessibilityFixer {

  private static final String TAG = "MaxAccessibilityFixer";
  public static final String PACKAGE_NAME = "ru.oneme.app";

  private final AccessibilityService service;

  // Cached hidden interactive nodes found during last scan
  private final List<NodeInfo> hiddenNodes = new ArrayList<>();
  private int currentHiddenIndex = -1;
  private long lastScanTime = 0;
  private static final long SCAN_CACHE_MS = 2000; // rescan every 2 seconds

  /** Minimal info about a found node. */
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
   * Returns true if Max messenger is currently in foreground.
   */
  public boolean isMaxInForeground() {
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null) return false;
    boolean isMax = PACKAGE_NAME.equals(
        root.getPackageName() != null ? root.getPackageName().toString() : "");
    root.recycle();
    return isMax;
  }

  /**
   * Deep-scans the Max UI tree and finds all interactive elements,
   * including ones hidden from accessibility.
   *
   * @return list of found hidden interactive elements with labels
   */
  @NonNull
  public List<NodeInfo> scanForHiddenElements() {
    long now = System.currentTimeMillis();
    if (now - lastScanTime < SCAN_CACHE_MS && !hiddenNodes.isEmpty()) {
      return hiddenNodes;
    }

    hiddenNodes.clear();
    currentHiddenIndex = -1;

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

    Log.d(TAG, "Found " + hiddenNodes.size() + " hidden interactive elements in Max");
    return hiddenNodes;
  }

  /**
   * Recursively scans ALL nodes, finding interactive ones that are not
   * accessibility-focusable (hidden from TalkBack).
   */
  private void deepScan(AccessibilityNodeInfo node, int depth) {
    if (node == null || depth > 30) return;

    boolean isInteractive = isInteractiveElement(node);
    boolean isAccessibilityFocusable = node.isAccessibilityFocused()
        || node.isFocusable()
        || node.isScreenReaderFocusable();

    // We want interactive elements that TalkBack can't normally reach
    if (isInteractive && node.isVisibleToUser()) {
      String className = node.getClassName() != null ? node.getClassName().toString() : "";
      String label = guessLabel(node, className);

      // Check if this is likely hidden from TalkBack
      if (!isAccessibilityFocusable || isEditTextWithoutFocus(node, className)) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);

        // Only add if it has reasonable size (not 0x0)
        if (bounds.width() > 10 && bounds.height() > 10) {
          hiddenNodes.add(new NodeInfo(
              AccessibilityNodeInfo.obtain(node), className, label, bounds));
          Log.d(TAG, "Found hidden element: " + label + " (" + className + ") at " + bounds);
        }
      }
    }

    // Recurse into ALL children
    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfo child = node.getChild(i);
      if (child != null) {
        deepScan(child, depth + 1);
        // Don't recycle children - they might be stored in hiddenNodes
      }
    }
  }

  /**
   * Returns true if this node is an interactive UI element.
   */
  private boolean isInteractiveElement(AccessibilityNodeInfo node) {
    if (node.isClickable() || node.isLongClickable() || node.isEditable()) {
      return true;
    }
    String className = node.getClassName() != null ? node.getClassName().toString() : "";
    return className.contains("EditText")
        || className.contains("Button")
        || className.contains("ImageButton")
        || className.contains("ImageView");
  }

  /**
   * Check if this is an EditText that's not properly accessible.
   */
  private boolean isEditTextWithoutFocus(AccessibilityNodeInfo node, String className) {
    return className.contains("EditText") && node.isEditable();
  }

  /**
   * Guesses a Russian label for a hidden element based on class, ID, position.
   */
  @NonNull
  private String guessLabel(AccessibilityNodeInfo node, String className) {
    // Check content description first
    CharSequence desc = node.getContentDescription();
    if (!TextUtils.isEmpty(desc)) {
      return desc.toString();
    }

    // Check hint text for EditText
    CharSequence hint = node.getHintText();
    if (!TextUtils.isEmpty(hint)) {
      return hint.toString();
    }

    // Check text
    CharSequence text = node.getText();
    if (!TextUtils.isEmpty(text)) {
      return text.toString();
    }

    // Check view ID
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
      if (id.contains("close")) return "Закрыть";
      if (id.contains("reply")) return "Ответить";
      if (id.contains("forward")) return "Переслать";
      if (id.contains("delete")) return "Удалить";
      if (id.contains("play") || id.contains("pause")) return "Воспроизвести";
    }

    // Guess by class and position
    if (className.contains("EditText")) {
      return "Поле ввода сообщения";
    }

    Rect bounds = new Rect();
    node.getBoundsInScreen(bounds);
    // Bottom area buttons
    if (bounds.bottom > 1500) { // rough check for bottom area
      if (className.contains("ImageButton") || className.contains("ImageView")) {
        if (bounds.right > 900) return "Кнопка отправки";
        if (bounds.left < 200) return "Прикрепить";
        return "Кнопка ввода";
      }
    }

    if (className.contains("ImageButton")) return "Кнопка";
    if (className.contains("ImageView") && node.isClickable()) return "Кнопка-изображение";

    return "Элемент управления";
  }

  /**
   * Tries to click/focus the input field in Max.
   * Scans the tree and performs ACTION_CLICK on the first EditText found.
   *
   * @return true if an EditText was found and clicked
   */
  public boolean focusInputField() {
    List<NodeInfo> nodes = scanForHiddenElements();
    for (NodeInfo info : nodes) {
      if (info.className.contains("EditText")) {
        Log.d(TAG, "Focusing input field: " + info.label);
        info.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        info.node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        return true;
      }
    }
    // Fallback: try to find EditText in full tree even if not hidden
    return findAndFocusEditText();
  }

  /**
   * Fallback method: searches entire tree for any EditText and focuses it.
   */
  private boolean findAndFocusEditText() {
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null) return false;

    AccessibilityNodeInfo editText = findFirstByClass(root, "EditText", 0);
    if (editText != null) {
      editText.performAction(AccessibilityNodeInfo.ACTION_CLICK);
      editText.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
      Log.d(TAG, "Focused EditText via fallback");
      root.recycle();
      return true;
    }
    root.recycle();
    return false;
  }

  /**
   * Navigates to the next hidden element. Returns its label for announcement.
   *
   * @return label of the next hidden element, or null if none
   */
  @Nullable
  public String navigateNextHidden() {
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) return null;

    currentHiddenIndex = (currentHiddenIndex + 1) % nodes.size();
    NodeInfo info = nodes.get(currentHiddenIndex);

    // Try to force accessibility focus
    info.node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
    return info.label;
  }

  /**
   * Navigates to the previous hidden element.
   */
  @Nullable
  public String navigatePreviousHidden() {
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) return null;

    currentHiddenIndex--;
    if (currentHiddenIndex < 0) currentHiddenIndex = nodes.size() - 1;
    NodeInfo info = nodes.get(currentHiddenIndex);

    info.node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
    return info.label;
  }

  /**
   * Clicks the currently selected hidden element.
   *
   * @return true if an element was clicked
   */
  public boolean clickCurrentHidden() {
    if (currentHiddenIndex < 0 || currentHiddenIndex >= hiddenNodes.size()) return false;
    NodeInfo info = hiddenNodes.get(currentHiddenIndex);
    return info.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
  }

  /**
   * Sets text in the input field of Max.
   *
   * @param text the text to type
   * @return true if text was set
   */
  public boolean setInputText(@NonNull String text) {
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null) return false;

    AccessibilityNodeInfo editText = findFirstByClass(root, "EditText", 0);
    if (editText != null) {
      Bundle args = new Bundle();
      args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
      boolean result = editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
      root.recycle();
      return result;
    }
    root.recycle();
    return false;
  }

  /**
   * Gets all hidden elements as a readable summary for announcement.
   */
  @NonNull
  public String getHiddenElementsSummary() {
    List<NodeInfo> nodes = scanForHiddenElements();
    if (nodes.isEmpty()) {
      return "Скрытых элементов не найдено";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("Найдено ").append(nodes.size()).append(" скрытых элементов: ");
    for (int i = 0; i < nodes.size(); i++) {
      if (i > 0) sb.append(", ");
      sb.append(nodes.get(i).label);
    }
    return sb.toString();
  }

  /**
   * Finds first node matching a class name substring.
   */
  @Nullable
  private AccessibilityNodeInfo findFirstByClass(
      AccessibilityNodeInfo node, String classSubstring, int depth) {
    if (node == null || depth > 30) return null;

    String cls = node.getClassName() != null ? node.getClassName().toString() : "";
    if (cls.contains(classSubstring) && node.isVisibleToUser()) {
      return AccessibilityNodeInfo.obtain(node);
    }

    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfo child = node.getChild(i);
      if (child != null) {
        AccessibilityNodeInfo found = findFirstByClass(child, classSubstring, depth + 1);
        if (found != null) {
          child.recycle();
          return found;
        }
        child.recycle();
      }
    }
    return null;
  }

  /** Clears cached scan results. */
  public void clearCache() {
    for (NodeInfo info : hiddenNodes) {
      try { info.node.recycle(); } catch (Exception ignored) {}
    }
    hiddenNodes.clear();
    currentHiddenIndex = -1;
  }
}
