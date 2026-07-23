# SkyBlock Translator

**[Русский]** Клиентский Fabric-мод, который переводит Hypixel SkyBlock на ваш язык: предметы, лор, меню, диалоги NPC и чат. Игра «внутри» остаётся английской — перевод накладывается только при отрисовке, поэтому другие моды (NEU, Skyblocker и т.д.), поиск и команды продолжают работать.

**[English]** A client-side Fabric mod that translates Hypixel SkyBlock into your language: items, lore, menus, NPC dialogues and chat. The game stays English under the hood — translation is applied at render time only, so other mods (NEU, Skyblocker, etc.), search and commands keep working.

> ⚠️ В ранней разработке / Early development.

## Как это работает / How it works

```
Рендер текста → сегментация (сохраняем §-форматирование)
  → нормализация (числа → плейсхолдеры: "Damage: +{0}")
  → словарь → локальный кэш → API-переводчик
```

- **Free** — встроенные словари, локальный кэш, бесплатный машинный перевод для нового текста (с лимитами).
- **Premium** (планируется) — облачный сервис: LLM-перевод со SkyBlock-глоссарием, общий кэш переводов, перевод чата игроков, без лимитов. Сам мод бесплатен для всех — Premium это подписка на облачный API.

## Версии / Versions

| | |
|---|---|
| Minecraft | 26.2 |
| Loader | Fabric (Loader ≥ 0.19.3, Fabric API) |
| Java | 25 |

## Сборка / Building

```
./gradlew build
```

Jar появится в `build/libs/`. Для запуска дев-клиента: `./gradlew runClient`. Java 25 скачается автоматически (Gradle toolchain), нужна лишь любая Java 17+ для запуска Gradle.

## Словари / Dictionaries

Переводы лежат в `src/main/resources/assets/skyblock-translator/dict/<lang>/*.json` в формате `"английский шаблон": "перевод"`. Числа и имена заменяются плейсхолдерами `{0}`, `{1}`… — один шаблон покрывает все значения. PR с переводами приветствуются.

## Роадмап / Roadmap

- [x] Фаза 0 — каркас проекта, CI
- [ ] Фаза 1 — pipeline перевода, словарь, кэш, тултипы и меню
- [ ] Фаза 2 — чат и диалоги NPC, бесплатный API-переводчик — первый публичный релиз
- [ ] Фаза 3 — Premium: облачный LLM-перевод, общий кэш, перевод чата игроков
- [ ] Фаза 4 — scoreboard/bossbar/tablist, другие языки, комьюнити-словари

## Лицензия / License

[MIT](LICENSE). Not affiliated with Hypixel or Mojang.
