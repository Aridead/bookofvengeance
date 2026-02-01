# Book Of Vengeance

## Что нужно заменить на свои файлы

1. **Шрифты** (обязательные):
   - `app/src/main/res/font/comic_sans.ttf` — Comic Sans для всего текста.
   - `app/src/main/res/font/graffiti_stroke.ttf` — Graffiti Stroke (Cocodesign) для статуса.
   - В `res/font` должны лежать только файлы шрифтов (`.ttf/.otf/.ttc`).

2. **Иконка синдиката** (плейсхолдер):
   - `app/src/main/res/drawable/ic_syndicate_placeholder.xml` — замените на вашу иконку (vector/PNG).

После добавления шрифтов нужно подключить их в `BookFontFamilies` в `MainActivity.kt`.

## Быстрый старт

Откройте проект в Android Studio и соберите приложение.

## Если сборка падает из-за SDK location not found

Ошибка `SDK location not found` означает, что Gradle не видит Android SDK. Нужно:

1. Убедиться, что Android SDK установлен в Android Studio.
2. Либо задать переменную окружения `ANDROID_HOME`,
3. Либо создать файл `local.properties` в корне проекта и прописать путь к SDK:

```
sdk.dir=C:\\Users\\<ваш_пользователь>\\AppData\\Local\\Android\\Sdk
```
