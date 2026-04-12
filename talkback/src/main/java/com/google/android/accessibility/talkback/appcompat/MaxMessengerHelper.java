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
    return isMaxMessenger(pkg);
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
    // Check if it's a clickable ImageButton/ImageView without description
    String className = node.getClassName() != null ? node.getClassName().toString() : "";

    if (className.contains("ImageButton") || className.contains("ImageView")) {
      if (node.isClickable()) {
        // Try to infer from position/parent
        AccessibilityNodeInfoCompat parent = node.getParent();
        if (parent != null) {
          String parentId = parent.getViewIdResourceName();
          if (parentId != null) {
            String lowerParentId = parentId.toLowerCase();
            if (lowerParentId.contains("toolbar") || lowerParentId.contains("action_bar")) {
              return "Кнопка панели";
            }
            if (lowerParentId.contains("input") || lowerParentId.contains("compose")) {
              return "Кнопка ввода";
            }
          }
        }
        return "Кнопка";
      }
      return "Изображение";
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
