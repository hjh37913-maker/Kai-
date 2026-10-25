# KAI Launcher

Домашний экран для Android.

## Как получить APK через GitHub

1. Создай репозиторий на GitHub.
2. Загрузи все файлы из этого архива (включая папку `.github`).
3. Перейди во вкладку **Actions**.
4. Запусти workflow **Build APK** (или он запустится сам после push).
5. Когда сборка закончится — скачай APK из **Artifacts**.

Debug APK не требует подписи. Для release нужен свой keystore (см. `keystore.properties.example` и `RELEASE.md`).

## Локальная сборка

```bash
./gradlew assembleDebug
```

APK появится в `app/build/outputs/apk/debug/`.
