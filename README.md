<div align="center">

# Kilnwork

**A small library mod for NeoForge: fluid gauges, fluid shapes and rendering for block entities, registration helpers.**

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-3c8527)](https://www.minecraft.net)
[![NeoForge 21.1](https://img.shields.io/badge/NeoForge-21.1.197%2B-e0682b)](https://neoforged.net)
[![License LGPL-3.0](https://img.shields.io/badge/license-LGPL--3.0-blue)](LICENSE)

Kilnwork is the library behind [Ceramics for 1.21](https://github.com/Vladeezyy/ceramics-1.21). It is built from the
parts of [Mantle](https://github.com/SlimeKnights/Mantle) by SlimeKnights that Ceramics needs, rewritten, because
Mantle has no release for newer Minecraft versions. Players do not install it separately: mods ship it inside their jar.

</div>

<details>
<summary><b>🇬🇧 English — click to open</b></summary>

## What is inside

| Package | What |
| --- | --- |
| `registration` | `BlockRegister` (blocks with items, one per enum value, block + slab + stairs + wall), `EnumObject`, `BuildingBlocks` |
| `block` | `SyncedBlockEntity` (one method for data saved and sent to the client), `GaugeBlock` |
| `fluid` | `FluidTransferHelper` (fill and drain tanks with held items), data driven transfers for bowls, bottles and sponges, `FluidText` |
| `tag` | gauge tags, `TagPreference` |
| `data` | JSON loaders per block and per block state, with `parent` inheritance |
| `client.render` | fluid cuboids, faucet and channel fluid shapes, `FluidCuboidRenderer` |
| `client.model` | block model geometry helpers |
| `client` | the gauge tooltip shown while looking at a gauge or tank |

## Data

- `assets/<namespace>/kilnwork/block_fluids/<block>.json` — fluid cuboids of a block entity, per block state.
- `assets/<namespace>/kilnwork/faucet_fluid/<block>.json` — fluid drawn under a faucet or channel.
- `assets/<namespace>/kilnwork/channel_fluids/<block>.json` — fluid shapes of a channel.
- `data/<namespace>/kilnwork/fluid_transfer/<name>.json` — `fill_item` / `empty_item` transfers for items without a
  fluid handler.
- Block tags `kilnwork:gauges`, `kilnwork:gauges/attached`, `kilnwork:gauges/tank`, `kilnwork:gauge_blacklist`,
  `kilnwork:hides_gauge_amount`.

## Using it

```bash
./gradlew publishToMavenLocal
```
Then in the mod: `implementation "dev.vladeezyy:kilnwork:1.21.1-1.0.0"` from `mavenLocal()`, plus `jarJar` to ship it.

## Languages

English, Russian, Ukrainian, German, Spanish, Brazilian Portuguese.

## License

LGPL-3.0-only ([LICENSE](LICENSE), GPL text in [licenses/COPYING](licenses/COPYING)), Copyright (c) 2026 Vladeezyy.
Parts are based on Mantle by SlimeKnights, MIT ([licenses/LICENSE-MANTLE](licenses/LICENSE-MANTLE)).

</details>

<details>
<summary><b>🇷🇺 Русский — нажмите, чтобы открыть</b></summary>

## Что это

Библиотека, на которой стоит [Ceramics для 1.21](https://github.com/Vladeezyy/ceramics-1.21). Собрана из тех частей
[Mantle](https://github.com/SlimeKnights/Mantle) (SlimeKnights), которые нужны Ceramics, и переписана: у Mantle нет
версий под новые Minecraft. Игрокам ставить отдельно не нужно — моды везут её внутри своего jar.

## Что внутри

| Пакет | Что |
| --- | --- |
| `registration` | `BlockRegister` (блоки с предметами, по блоку на значение enum, блок + плита + ступеньки + ограда), `EnumObject`, `BuildingBlocks` |
| `block` | `SyncedBlockEntity` (один метод для данных на диск и клиенту), `GaugeBlock` |
| `fluid` | `FluidTransferHelper` (налить и слить жидкость предметом в руке), перелив из мисок, бутылок и губки через данные, `FluidText` |
| `tag` | теги датчиков, `TagPreference` |
| `data` | загрузчики JSON на блок и на состояние блока с наследованием через `parent` |
| `client.render` | кубоиды жидкости, формы жидкости кранов и жёлобов, `FluidCuboidRenderer` |
| `client.model` | помощники для геометрии моделей блоков |
| `client` | подсказка датчика, когда смотришь на датчик или бак |

## Языки

Английский, русский, украинский, немецкий, испанский, бразильский португальский.

## Лицензия

LGPL-3.0-only ([LICENSE](LICENSE), текст GPL в [licenses/COPYING](licenses/COPYING)), Copyright (c) 2026 Vladeezyy.
Часть кода основана на Mantle от SlimeKnights, MIT ([licenses/LICENSE-MANTLE](licenses/LICENSE-MANTLE)).

</details>
