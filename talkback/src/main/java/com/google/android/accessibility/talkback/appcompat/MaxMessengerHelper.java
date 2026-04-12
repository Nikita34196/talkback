/*
 * Copyright 2024 Nikita
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.google.android.accessibility.talkback.appcompat;

import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * Accessibility helper for the Max messenger (ru.oneme.app).
 *
 * <p>Improves TalkBack behavior in Max by:
 * <ul>
 *   <li>Better labeling of unlabeled buttons and icons</li>
 *   <li>Improved chat list navigation</li>
 *   <li>Better message reading with sender/time info</li>
 *   <li>Identifying input fields and action buttons</li>
 * </ul>
 */
public class MaxMessengerHelper {

  public static final String PACKAGE_NAME = "ru.oneme.app";

  // Common view IDs in Max messenger (may need updates as app changes)
  private static final String ID_PREFIX = "ru.oneme.app:id/";

  // Known unlabeled button patterns
  private static final String[][] BUTTON_HINTS = {
      {"send", "Отправить сообщение"},
      {"attach", "Прикрепить файл"},
      {"emoji", "Эмодзи"},
      {"sticker", "Стикеры"},
      {"voice", "Голосовое сообщение"},
      {"record", "Записать"},
      {"camera", "Камера"},
      {"call", "Позвонить"},
      {"video_call", "Видеозвонок"},
      {"back", "Назад"},
      {"menu", "Меню"},
      {"search", "Поиск"},
      {"more", "Ещё"},
      {"close", "Закрыть"},
      {"delete", "Удалить"},
      {"reply", "Ответить"},
      {"forward", "Переслать"},
      {"copy", "Копировать"},
      {"pin", "Закрепить"},
      {"mute", "Без звука"},
      {"unmute", "Со звуком"},
      {"photo", "Фото"},
      {"gallery", "Галерея"},
      {"file", "Файл"},
      {"contact", "Контакт"},
      {"location", "Местоположение"},
      {"edit", "Редактировать"},
      {"profile", "Профиль"},
      {"settings", "Настройки"},
      {"chat", "Чат"},
      {"channel", "Канал"},
      {"group", "Группа"},
      {"notification", "Уведомления"},
      {"add", "Добавить"},
      {"create", "Создать"},
      {"input", "Поле ввода"},
      {"message_input", "Написать сообщение"},
  };

  /**
   * Returns true if the event is from Max messenger.
   */
  public static boolean isMaxMessenger(@Nullable CharSequence packageName) {
    return PACKAGE_NAME.equals(packageName != null ? packageName.toString() : null);
  }

  /**
   * Returns true if the given node is from Max messenger.
   */
  public static boolean isMaxMessenger(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) return false;
    CharSequence pkg = node.getPackageName();
    if (isMaxMessenger(pkg)) return true;
    // Fallback: check global state (package may be null on some nodes)
    try {
      return com.google.android.accessibility.utils.AppCompatState.isMaxMessengerActive();
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Tries to provide a better label for an unlabeled node in Max.
   * Checks the view's resource ID against known patterns.
   *
   * @param node The unlabeled node
   * @return A Russian label, or null if not recognized
   */
  @Nullable
  public static String getLabelForNode(@NonNull AccessibilityNodeInfoCompat node) {
    String viewId = node.getViewIdResourceName();
    if (TextUtils.isEmpty(viewId)) {
      return guessFromContext(node);
    }

    // Strip package prefix
    String shortId = viewId;
    if (viewId.startsWith(ID_PREFIX)) {
      shortId = viewId.substring(ID_PREFIX.length());
    }
    String lowerShortId = shortId.toLowerCase();

    // Match against known patterns
    for (String[] hint : BUTTON_HINTS) {
      if (lowerShortId.contains(hint[0])) {
        return hint[1];
      }
    }

    return guessFromContext(node);
  }

  /**
   * Tries to guess a label from the node's context (class, content description, etc).
   */
  @Nullable
  private static String guessFromContext(@NonNull AccessibilityNodeInfoCompat node) {
    String className = node.getClassName() != null ? node.getClassName().toString() : "";

    if (className.contains("EditText")) {
      return "Написать сообщение";
    }

    if (className.contains("ImageButton") || className.contains("ImageView")) {
      if (node.isClickable() || node.isLongClickable()) {
        // Use position-based labeling for the bottom input bar
        // Layout from screenshot: [emoji] [EditText] [attach] [camera] [mic/send]
        android.graphics.Rect bounds = new android.graphics.Rect();
        node.getBoundsInScreen(bounds);

        // Find EditText sibling to determine relative position
        AccessibilityNodeInfoCompat parent = node.getParent();
        android.graphics.Rect editBounds = null;
        if (parent != null) {
          for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfoCompat sibling = parent.getChild(i);
            if (sibling != null) {
              String sibClass = sibling.getClassName() != null
                  ? sibling.getClassName().toString() : "";
              if (sibClass.contains("EditText")) {
                editBounds = new android.graphics.Rect();
                sibling.getBoundsInScreen(editBounds);
                break;
              }
            }
          }
          // Also check grandparent for EditText
          if (editBounds == null) {
            AccessibilityNodeInfoCompat grandparent = parent.getParent();
            if (grandparent != null) {
              editBounds = findEditTextBounds(grandparent);
            }
          }
        }

        if (editBounds != null && editBounds.height() > 0) {
          boolean sameRow = Math.abs(bounds.centerY() - editBounds.centerY()) < 80;
          if (sameRow) {
            if (bounds.right <= editBounds.left) {
              return "Эмодзи";
            }
            if (bounds.left >= editBounds.right) {
              // Count position from right: rightmost = send/mic, then camera, then attach
              int buttonsToRight = 0;
              if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                  AccessibilityNodeInfoCompat sib = parent.getChild(i);
                  if (sib != null) {
                    android.graphics.Rect sibBounds = new android.graphics.Rect();
                    sib.getBoundsInScreen(sibBounds);
                    if (sibBounds.left > bounds.right
                        && Math.abs(sibBounds.centerY() - bounds.centerY()) < 80) {
                      buttonsToRight++;
                    }
                  }
                }
              }
              if (buttonsToRight == 0) return "Голосовое сообщение";
              if (buttonsToRight == 1) return "Камера";
              if (buttonsToRight == 2) return "Прикрепить файл";
              return "Кнопка";
            }
          }
        }

        // Fallback: use rough screen position
        // Assume 1080px width screen, bottom bar
        if (bounds.centerX() < 150) return "Эмодзи";
        if (bounds.centerX() > 900) return "Голосовое сообщение";
        if (bounds.centerX() > 700) return "Камера";
        if (bounds.centerX() > 500) return "Прикрепить файл";

        return "Кнопка";
      }
      return null; // non-clickable image, don't label
    }

    if (className.contains("Button") && node.isClickable()) {
      return "Кнопка";
    }

    return null;
  }

  /** Recursively find EditText bounds in a subtree. */
  @Nullable
  private static android.graphics.Rect findEditTextBounds(
      @NonNull AccessibilityNodeInfoCompat node) {
    String cls = node.getClassName() != null ? node.getClassName().toString() : "";
    if (cls.contains("EditText")) {
      android.graphics.Rect b = new android.graphics.Rect();
      node.getBoundsInScreen(b);
      return b;
    }
    for (int i = 0; i < node.getChildCount() && i < 20; i++) {
      AccessibilityNodeInfoCompat child = node.getChild(i);
      if (child != null) {
        android.graphics.Rect result = findEditTextBounds(child);
        if (result != null) return result;
      }
    }
    return null;
  }

  /**
   * Enhances chat message announcements by extracting structured info.
   * Tries to build a string like "Имя: сообщение, время".
   *
   * @param node A node representing a chat message item
   * @return Enhanced description, or null if not a recognized message layout
   */
  @Nullable
  public static String getEnhancedMessageDescription(
      @NonNull AccessibilityNodeInfoCompat node) {

    if (!isMaxMessenger(node)) return null;

    // Collect all text children
    List<String> texts = new ArrayList<>();
    collectTexts(node, texts, 0);

    if (texts.size() >= 2) {
      // Typical message layout: sender, message text, time
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < texts.size(); i++) {
        if (i > 0) sb.append(", ");
        sb.append(texts.get(i));
      }
      return sb.toString();
    }

    return null;
  }

  /**
   * Recursively collects text content from child nodes.
   */
  private static void collectTexts(
      @NonNull AccessibilityNodeInfoCompat node, @NonNull List<String> texts, int depth) {
    if (depth > 10) return; // Safety limit

    CharSequence text = node.getText();
    if (!TextUtils.isEmpty(text)) {
      texts.add(text.toString().trim());
    }

    CharSequence contentDesc = node.getContentDescription();
    if (!TextUtils.isEmpty(contentDesc) && TextUtils.isEmpty(text)) {
      texts.add(contentDesc.toString().trim());
    }

    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfoCompat child = node.getChild(i);
      if (child != null) {
        collectTexts(child, texts, depth + 1);
      }
    }
  }

  /**
   * Returns a list of important nodes in the chat list for quick navigation.
   * Finds chat item containers so the user can swipe between chats.
   */
  @NonNull
  public static List<AccessibilityNodeInfoCompat> findChatListItems(
      @NonNull AccessibilityNodeInfoCompat root) {
    List<AccessibilityNodeInfoCompat> items = new ArrayList<>();
    findClickableItems(root, items, 0);
    return items;
  }

  private static void findClickableItems(
      @NonNull AccessibilityNodeInfoCompat node,
      @NonNull List<AccessibilityNodeInfoCompat> items,
      int depth) {
    if (depth > 15) return;

    if (node.isClickable() && node.isVisibleToUser()) {
      String className = node.getClassName() != null ? node.getClassName().toString() : "";
      // Look for list item containers
      if (className.contains("ViewGroup") || className.contains("LinearLayout")
          || className.contains("RelativeLayout") || className.contains("FrameLayout")) {
        // Check if it has text children (likely a chat item)
        List<String> texts = new ArrayList<>();
        collectTexts(node, texts, 0);
        if (!texts.isEmpty()) {
          items.add(node);
          return; // Don't descend into chat items
        }
      }
    }

    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfoCompat child = node.getChild(i);
      if (child != null) {
        findClickableItems(child, items, depth + 1);
      }
    }
  }
}
