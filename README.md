# Spice of Life: Pot Pie Edition

A Minecraft Forge 1.20.1 mod that encourages dietary variety, in the spirit of the original Spice of Life and Carrot Edition.

## How it works

- Every food gets a **score** computed automatically from its crafting tree and nutrition. Basic crops and raw foods are worth little; multi-step meals made from other foods are worth much more. Machine/mod recipes the recipe walker can't see can be scored via config overrides.
- Eating a food for the **first time** permanently adds its score to your lifetime points. **Every 10 points = +1 permanent heart** (cost and growth configurable). 
- **Diminishing returns**: re-eating a food you ate recently restores less hunger and saturation, scaling back up as it ages out of your recent-food queue. Keep your diet varied!
- Food values are hidden until you taste a food for the first time (configurable).

## Building

```
./gradlew build
```

Output jar lands in `build/libs/`.

## License

LGPL-2.1. This project is a fork of [Spice of Life: Apple Pie Edition](https://github.com/txnimc/Spice-of-Life-Apple-Pie), itself descended from the Sweet Potato, Potato, and Carrot Editions of Spice of Life.
