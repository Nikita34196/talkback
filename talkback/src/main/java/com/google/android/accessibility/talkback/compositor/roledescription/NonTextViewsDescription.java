/*
 * Copyright (C) 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.accessibility.talkback.compositor.roledescription;

import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.AccessibilityNodeFeedbackUtils;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.accessibility.talkback.imagecaption.ImageContents;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Role;

/**
 * Role description for non-text-view {@link Role.ROLE_IMAGE} and {@link Role.ROLE_IMAGE_BUTTON}.
 */
public final class NonTextViewsDescription implements RoleDescription {

  private static final String MAX_PACKAGE = "ru.oneme.app";
  private final ImageContents imageContents;

  NonTextViewsDescription(ImageContents imageContents) {
    this.imageContents = imageContents;
  }

  /**
   * Tries to label a Max messenger element by its position relative to the EditText.
   * Returns null if not in Max or can't determine label.
   */
  private static String tryLabelMaxElement(
      AccessibilityNodeInfoCompat node, Context context) {
    if (!isFromMax(node)) {
      return null;
    }

    String className = node.getClassName() != null ? node.getClassName().toString() : "";
    if (className.contains("EditText")) {
      return "Написать сообщение";
    }

    android.graphics.Rect nodeBounds = new android.graphics.Rect();
    node.getBoundsInScreen(nodeBounds);
    if (nodeBounds.width() <= 0 || nodeBounds.height() <= 0) return null;

    // Get screen dimensions from root
    int screenWidth = getScreenWidth(node);
    int screenHeight = getScreenHeight(node);

    // ================================================================
    // ZONE 1: Top toolbar (back, call, video call, menu)
    // Typically within top ~200px of screen
    // ================================================================
    if (nodeBounds.centerY() < 200) {
      // Back arrow — leftmost button
      if (nodeBounds.centerX() < screenWidth * 0.12) {
        return "Назад";
      }
      // Right side buttons: phone, video, menu (right-to-left)
      if (nodeBounds.centerX() > screenWidth * 0.55) {
        int toRight = countSiblingsToRightInRow(node, nodeBounds);
        if (toRight == 0) return "Меню";
        if (toRight == 1) return "Видеозвонок";
        if (toRight == 2) return "Голосовой звонок";
        return "Кнопка панели";
      }
      // Avatar area
      return null;
    }

    // ================================================================
    // ZONE 2: Input bar (emoji, edittext, attach, camera, mic/send)
    // Same row as EditText
    // ================================================================
    android.graphics.Rect editBounds = findEditTextInNearby(node);
    if (editBounds != null && editBounds.height() > 0) {
      boolean sameRow = Math.abs(nodeBounds.centerY() - editBounds.centerY()) < 80;
      if (sameRow) {
        if (nodeBounds.right <= editBounds.left + 20) {
          return "Эмодзи";
        }
        if (nodeBounds.left >= editBounds.right - 20) {
          int toRight = countButtonsToRight(node, nodeBounds);
          if (toRight == 0) return "Голосовое сообщение";
          if (toRight == 1) return "Камера";
          if (toRight == 2) return "Прикрепить файл";
          return "Кнопка";
        }
      }
    }

    // ================================================================
    // ZONE 3: Recording mode bottom bar (delete, pause, send)
    // When recording, these appear at the very bottom of screen
    // ================================================================
    if (nodeBounds.centerY() > screenHeight - 250) {
      // No EditText visible = probably recording mode
      if (editBounds == null || editBounds.height() == 0) {
        if (nodeBounds.centerX() < screenWidth * 0.2) {
          return "Удалить запись";
        }
        if (nodeBounds.centerX() > screenWidth * 0.8) {
          return "Отправить запись";
        }
        if (nodeBounds.centerX() > screenWidth * 0.3
            && nodeBounds.centerX() < screenWidth * 0.7) {
          return "Пауза";
        }
      }
      // Bottom area with EditText visible — already handled in ZONE 2
      // If we're here, it's an extra button
      if (nodeBounds.centerX() > screenWidth * 0.8) {
        return "Отправить";
      }
    }

    // ================================================================
    // ZONE 4: Message area — play buttons on voice messages
    // Circular buttons in the middle area of the screen
    // ================================================================
    if (nodeBounds.centerY() > 200 && nodeBounds.centerY() < screenHeight - 250) {
      // Play buttons are typically square-ish and small (voice message controls)
      int w = nodeBounds.width();
      int h = nodeBounds.height();
      boolean isSmallSquare = w > 30 && h > 30 && w < 200 && h < 200
          && Math.abs(w - h) < 40;
      if (isSmallSquare && node.isClickable()) {
        return "Воспроизвести";
      }
    }

    // Generic fallback
    if (node.isClickable()) return "Кнопка";
    return null;
  }

  /** Get screen width from node's root bounds. */
  private static int getScreenWidth(AccessibilityNodeInfoCompat node) {
    try {
      AccessibilityNodeInfoCompat current = node;
      for (int i = 0; i < 20; i++) {
        AccessibilityNodeInfoCompat parent = current.getParent();
        if (parent == null) break;
        current = parent;
      }
      android.graphics.Rect rootBounds = new android.graphics.Rect();
      current.getBoundsInScreen(rootBounds);
      if (rootBounds.width() > 0) return rootBounds.width();
    } catch (Exception ignored) {}
    return 1080;
  }

  /** Get screen height from node's root bounds. */
  private static int getScreenHeight(AccessibilityNodeInfoCompat node) {
    try {
      AccessibilityNodeInfoCompat current = node;
      for (int i = 0; i < 20; i++) {
        AccessibilityNodeInfoCompat parent = current.getParent();
        if (parent == null) break;
        current = parent;
      }
      android.graphics.Rect rootBounds = new android.graphics.Rect();
      current.getBoundsInScreen(rootBounds);
      if (rootBounds.height() > 0) return rootBounds.height();
    } catch (Exception ignored) {}
    return 2400;
  }

  /** Count siblings to the right in the same row (for toolbar buttons). */
  private static int countSiblingsToRightInRow(
      AccessibilityNodeInfoCompat node, android.graphics.Rect nodeBounds) {
    AccessibilityNodeInfoCompat parent = node.getParent();
    if (parent == null) return -1;
    int count = 0;
    for (int i = 0; i < parent.getChildCount(); i++) {
      AccessibilityNodeInfoCompat sib = parent.getChild(i);
      if (sib != null && sib != node) {
        android.graphics.Rect sb = new android.graphics.Rect();
        sib.getBoundsInScreen(sb);
        if (sb.left > nodeBounds.right
            && Math.abs(sb.centerY() - nodeBounds.centerY()) < 50
            && sb.width() > 10) {
          count++;
        }
      }
    }
    return count;
  }

  private static boolean isFromMax(AccessibilityNodeInfoCompat node) {
    // Check node itself
    CharSequence pkg = node.getPackageName();
    if (MAX_PACKAGE.equals(pkg != null ? pkg.toString() : "")) return true;
    // Check parents (up to 15 levels)
    AccessibilityNodeInfoCompat current = node;
    for (int i = 0; i < 15; i++) {
      AccessibilityNodeInfoCompat parent = current.getParent();
      if (parent == null) break;
      pkg = parent.getPackageName();
      if (MAX_PACKAGE.equals(pkg != null ? pkg.toString() : "")) return true;
      current = parent;
    }
    // Global flag fallback
    try {
      return com.google.android.accessibility.utils.AppCompatState.isMaxMessengerActive();
    } catch (Exception e) {
      return false;
    }
  }

  private static android.graphics.Rect findEditTextInNearby(AccessibilityNodeInfoCompat node) {
    // Search in parent and grandparent
    AccessibilityNodeInfoCompat parent = node.getParent();
    for (int level = 0; level < 3 && parent != null; level++) {
      for (int i = 0; i < parent.getChildCount() && i < 20; i++) {
        AccessibilityNodeInfoCompat child = parent.getChild(i);
        if (child != null) {
          String cls = child.getClassName() != null ? child.getClassName().toString() : "";
          if (cls.contains("EditText")) {
            android.graphics.Rect b = new android.graphics.Rect();
            child.getBoundsInScreen(b);
            if (b.width() > 0) return b;
          }
          // Check one level deeper
          for (int j = 0; j < child.getChildCount() && j < 10; j++) {
            AccessibilityNodeInfoCompat grandchild = child.getChild(j);
            if (grandchild != null) {
              cls = grandchild.getClassName() != null ? grandchild.getClassName().toString() : "";
              if (cls.contains("EditText")) {
                android.graphics.Rect b = new android.graphics.Rect();
                grandchild.getBoundsInScreen(b);
                if (b.width() > 0) return b;
              }
            }
          }
        }
      }
      parent = parent.getParent();
    }
    return null;
  }

  private static int countButtonsToRight(
      AccessibilityNodeInfoCompat node, android.graphics.Rect nodeBounds) {
    AccessibilityNodeInfoCompat parent = node.getParent();
    if (parent == null) return -1;
    int count = 0;
    for (int i = 0; i < parent.getChildCount(); i++) {
      AccessibilityNodeInfoCompat sib = parent.getChild(i);
      if (sib != null && sib != node) {
        android.graphics.Rect sb = new android.graphics.Rect();
        sib.getBoundsInScreen(sb);
        if (sb.left > nodeBounds.right
            && Math.abs(sb.centerY() - nodeBounds.centerY()) < 80
            && sb.width() > 10) {
          count++;
        }
      }
    }
    return count;
  }

  @Override
  public CharSequence nodeName(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    CharSequence result = AccessibilityNodeFeedbackUtils.getNodeTextOrLabelOrIdDescription(
        node, context, imageContents, globalVariables);
    // If no label found, try Max messenger position-based labeling
    if (TextUtils.isEmpty(result) && node != null) {
      String maxLabel = tryLabelMaxElement(node, context);
      if (maxLabel != null) {
        return maxLabel;
      }
    }
    return result;
  }

  @Override
  public CharSequence nodeRole(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    if (!globalVariables.getSpeakRoles() || node == null) {
      return "";
    }

    CharSequence nodeRoleDescription =
        AccessibilityNodeFeedbackUtils.getNodeRoleDescription(node, context, globalVariables);
    if (!TextUtils.isEmpty(nodeRoleDescription)) {
      return nodeRoleDescription;
    }

    if (Role.getRole(node) == Role.ROLE_IMAGE
        && AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityNodeInfoCompat.ACTION_SELECT)) {
      return node.isAccessibilityFocused() ? context.getString(R.string.value_image) : "";
    } else {
      return AccessibilityNodeFeedbackUtils.getNodeRoleName(node, context);
    }
  }

  @Override
  public CharSequence nodeState(
      AccessibilityEvent event,
      AccessibilityNodeInfoCompat node,
      Context context,
      GlobalVariables globalVariables) {
    CharSequence nodeStateDescription =
        AccessibilityNodeFeedbackUtils.getNodeStateDescription(node, context, globalVariables);
    if (!TextUtils.isEmpty(nodeStateDescription)) {
      return nodeStateDescription;
    }

    CharSequence nodeTextOrLabelOrId =
        AccessibilityNodeFeedbackUtils.getNodeTextOrLabelOrIdDescription(
            node, context, imageContents, globalVariables);
    if (TextUtils.isEmpty(nodeTextOrLabelOrId)) {
      // Try Max messenger labeling before saying "без ярлыка"
      if (node != null) {
        String maxLabel = tryLabelMaxElement(node, context);
        if (maxLabel != null) {
          return "";
        }
      }
      return context.getString(R.string.value_unlabelled);
    }
    return "";
  }
}
