<div align="center">

<img src="https://github.com/smokahs/SoL-Pot-Pie/blob/1.20.1/webassets/pot_pie_full_res.png?raw=true" width="156" alt="Spice of Life: Pot Pie Edition" />

# Spice of Life: Pot Pie Edition

**A Minecraft Forge 1.20.1 mod that rewards eating a varied diet.**

</div>

## How it works

- **Every food has a nutritional score**, computed automatically from its own nutrition and saturation. Special or modded foods can be hand-tuned with config overrides.
- **First bites earn permanent hearts.** Eating a food for the *first time ever* permanently adds its score to your lifetime points. Every 8 points (configurable) grants **+1 permanent heart**. Hearts are never lost on death or when your diet changes.
- **Diminishing returns.** Re-eating a food you ate recently restores less hunger and saturation. It scales back up to full as that food ages out of your recent-food queue, so keep it varied!
- **Hidden until tasted.** A food's values stay hidden until you eat it once (`Not yet eaten. What does it taste like?`). AppleSkin's hunger/HUD previews are hidden for un-eaten foods too.

## In-game items

- **Food Book:** press the keybind (or open the item) for an overview of your hearts earned, lifetime points, points until your next heart, and the foods you've eaten recently.
- **Lunchbag / Lunchbox / Golden Lunchbox:** food storage that holds 5 / 9 / 14 stacks.

## Commands

`/solpotpie` - clear a player's recent-food list, force a sync, print stats (points / hearts / next heart / recent variety), or manage the Origins cache.

## Configuration

Server config generates **per world** at `saves/<world>/serverconfig`, so existing worlds keep their old defaults. Sections:

| Section | Controls |
| --- | --- |
| **Hearts** | Base heart cost, per-heart cost increment, health per heart, max hearts |
| **Scoring** | Score multiplier, max score, per-food score overrides |
| **DiminishingReturns** | Toggle, floor multiplier, recent-food queue size |
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
