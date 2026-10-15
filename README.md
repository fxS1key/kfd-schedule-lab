# Kotlin Model

## Описание

Проект демонстрирует реализацию модели на Kotlin с использованием типобезопасных типов, коллекций и делегирования. Реализация сопровождается тестами, проверяющими корректность работы и граничные случаи.

## Сборка и запуск

Требуется JDK и Gradle Wrapper.

```bash
./gradlew clean build :app:installDist
python3 tools/verify.py
```

если без ошибок, то все окей

проверить большой файл
```bash
python tools/generate_samples.py --huge
python3 tools\verify.py --huge
```

