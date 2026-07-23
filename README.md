<div align="center">

<img src="https://github.com/smokahs/SoL-Pot-Pie/blob/1.20.1/webassets/logo.png?raw=true" width="156" alt="Spice of Life: Pot Pie Edition" />

# Spice of Life: Pot Pie Edition

**A Minecraft Forge 1.20.1 mod that rewards eating a varied diet.**

Requires [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli) and [AppleSkin](https://www.curseforge.com/minecraft/mc-mods/AppleSkin)


</div>

## How it works

- **Every food has a nutritional score**, computed automatically from its own nutrition and saturation. Special or modded foods can be hand-tuned with config overrides.
- **Lifetime total.** Eating a food for the *first time ever* permanently adds its score to your lifetime points. Every 10 points (configurable) grants **+1 permanent heart**. Hearts are never lost on death or when your diet changes.
- **Diminishing returns** now scale on **how many times** you have eaten a food in the last 'eaten food queue'(default 128, configurable). The first time is always worth full value; subsequentinal consumptions lower the value over a default of 5, then it bottoms out. Diminishing return values are shown in AppleSkin as well!
- **Hidden until tasted.** A food's values stay hidden until you eat it once (`Not yet eaten. What does it taste like?`). AppleSkin's hunger/HUD previews are hidden for un-eaten foods too.

## In-game items

- **Food Book:** craft it from a book and a pot pie (use a keybind if its in your inventory!) to see your full record: buttons down the left page, and live detail on the right for whichever one you're hovering. Click through for hearts earned, lifetime points, points until your next heart, how much of the pack you have tasted, and the foods you've eaten recently.
- **Lunchbag / Lunchbox / Golden Lunchbox:** food storage that holds 5 / 9 / 14 stacks.
- **Pot Pie:** a hearty meal. Cooked in [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight)'s cooking pot from a pie crust, two cooked chicken cuts, an onion, a carrot and a potato; without Farmer's Delight installed it has a vanilla crafting recipe instead.

## Changelog
See [CHANGELOG.md](https://github.com/smokahs/SoL-Pot-Pie/blob/1.20.1/CHANGELOG.md) for version history.

## Commands

`/solpotpie` - clear a player's recent-food list, force a sync, print stats (points / hearts / next heart / recent variety / foods tasted, each against the pack total), or manage the Origins cache.

## Configuration

Config generates **globally** at `config/solpotpie-common.toml`, so a pack can ship one file that every world uses (client-only options live beside it in `config/solpotpie-client.toml`). Worlds made before this change keep an old `saves/<world>/serverconfig/solpotpie-server.toml` that is no longer read, so copy any values you had customised over. Sections:

| Section | Controls |
| --- | --- |
| **Hearts** | Base heart cost, per-heart cost increment, health per heart, max hearts |
| **Scoring** | Score multiplier, max score, per-food score overrides |
| **DiminishingReturns** | Toggle, eats until a food bottoms out, hunger/saturation floors, recovery rate, recent-food queue size |
| **Filtering** | Blacklist / whitelist, whether blacklisted foods still fill queue slots |
| **Miscellaneous** | Reset recent foods on death, limit progression to survival mode |
| **Client** | Food tooltip toggle, hide values until eaten |

## Building

```
./gradlew build
```

The output jar lands in `build/libs/`. Use `./gradlew runClient` to test in-game.

## License & credits

LGPL-2.1. A fork of [Spice of Life: Apple Pie Edition](https://github.com/txnimc/Spice-of-Life-Apple-Pie), itself descended from the Sweet Potato, Potato, and Carrot Editions of Spice of Life.
