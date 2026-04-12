# ScreenReader TB — Модификации

## Что добавлено

### 1. Anthropic AI для распознавания иконок
Когда стандартные методы (OCR, icon detection) не могут описать элемент,
скриншот отправляется в Claude Vision API и возвращается описание на русском.

**Настройка:** В настройках TalkBack → ввести API ключ Anthropic.

### 2. Улучшенная поддержка мессенджера Max (ru.oneme.app)
- Автоматическое распознавание неподписанных кнопок по ID ресурсов
- **Глубокий сканер** — находит скрытые от TalkBack элементы (поле ввода, кнопки)
- Специальные жесты в Max:
  - **3 пальца тап** — фокус на поле ввода сообщения
  - **3 пальца двойной тап** — озвучить все скрытые элементы
  - **3 пальца свайп вправо/влево** — навигация по скрытым элементам
  - **3 пальца свайп вниз** — нажать текущий скрытый элемент
- Работает без настройки

### 3. Кастомные жесты для приложений
Можно назначить разные действия жестов для конкретных приложений.
Настройки хранятся в SharedPreferences по packageName.

## Установка

1. Склонируй `google/talkback`
2. Распакуй этот zip в корень репозитория (заменит файлы)
3. Пуш → GitHub Actions соберёт APK

## Файлы

**Новые:**
- `AnthropicIconDescriber.java` — API клиент Claude Vision
- `PerAppGestureManager.java` — менеджер жестов по приложениям
- `MaxMessengerHelper.java` — подписи кнопок для Max
- `MaxAccessibilityFixer.java` — глубокий сканер скрытых элементов Max

**Модифицированные (оригинал + наши изменения):**
- `ImageCaptioner.java` — добавлен Anthropic как fallback
- `GestureController.java` — добавлен перехват жестов по packageName
- `AccessibilityNodeFeedbackUtils.java` — добавлен Max messenger labeling
- `build.gradle` — изменён applicationId
- `.github/workflows/build.yml` — CI/CD
