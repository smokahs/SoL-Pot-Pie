# Changelog

## 2.3.1 
1. Added appleskin compat for lunchboxes
 - The hunger overlay and tooltip preview the food the container would actually feed you. Empty containers show nothing.
 - `hideValuesUntilEaten` now checks the food inside, not the container.

2. Containers no longer feed you worthless food, lol

## 2.3.0

1. Lunchboxes now open and close, like the old spice of life edition
 - Right click a closed container to open its inventory. Sneak and right click toggles it open or closed.
 - Eating only works while open: an open container eats the best food inside.
 - Right clicking a chest (or any inventory block) with an open container restocks it: contents dump into the chest, then it pulls back the best assortment, ranked by what you have never tasted and gone longest without. Each food takes at most one slot to keep the mix varied. Closed containers open the chest as normal.

2. Two optional diminishing returns mechanics from the old 1.7.10 editions (both off by default)
 - `diminishingEatsToFloorAddsNutrition`: a food's own hunger value is added to `diminishingEatsToFloor`, so junk food bottoms out after a couple of eats while filling food holds value longer.
 - `diminishedEatTimeScaling`: worn-out foods take longer to eat, eating time divided by the food's current fraction of its full hunger, uncapped. A food at the floor of 0 hunger is effectively uneatable.
 - GTNH values: both true, `diminishingFloorHunger` 0, `diminishingFloorSaturation` 0, `diminishingRecoveryVal` 0.0, `diminishingEatsToFloor` 8.

## 2.2.1

1. Lunchbox slot counts are now configurable
 - New `Lunchboxes` config section: `lunchbagSlots` (default 5), `lunchboxSlots` (default 9), `goldenLunchboxSlots` (default 14). Each ranges 0-14, the most the GUI fits.
 - Values sync from the server. Existing containers resize on next reload; shrinking a container voids items past the new size, so empty them first.

2. Fixes
 - Containers configured to an odd slot count above 9 no longer draw a third slot row on top of the player inventory.

## 2.2.0

1. Added food groups
 - Groups are `.json` files in `config/solpotpie/`, one per group, matching item ids and item tags. Fruit, vegetables, meat, fish, grain, sweets, soups and foraged ship by default, so it works out of the box. See `_example.json.txt` for the format.
 - `foodGroupDiversityThreshold` (default 5): while your recent meals cover this many distinct groups or fewer, diminishing returns apply as normal. Cover more and they switch off entirely, however much you repeat yourself. Eating widely is now the way out, not eating rarely. 0 ignores groups.
 - `useFoodGroupsAsWhitelists` (default false): when on, a food in no group at all never diminishes. A group marked `"blacklist": true` always exempts its foods either way.

2. Added a grace period for new characters
 - `newPlayerFoodsEatenThreshold` (default 10): diminishing returns don't apply until your 11th food. Counted per player per world, survives death. 0 disables.

3. Default changes (new configs only, existing ones are left alone)
 - `queueSize` 128 -> 20. 
 - `baseHeartCost` 10, or 6 if Hunger Overhauled is installed.

4. Fixes
 - Food groups now sync from the server, meaning AppleSkin previews on a server were corrected. 
 - An empty or unreadable food group folder no longer switches diminishing returns off for the whole pack when `useFoodGroupsAsWhitelists` is on.

## 2.1.0
1. Added a "Hold shift for more" on some tooltips to reduce clutter once you eat a food for the first time.


## 2.0.0

1. Added an in game chicken pot pie
 - Cooked in Farmer's Delight's cooking pot from a pie crust, two cooked chicken cuts, an onion, a carrot and a potato. If FD is not installed it falls back to a vanilla recipe, my favorite food lol

2. Diminishing returns rewritten and general bug fixes to hunger system
    - DA RULEZ
        - Diminishing returns now scale on **how many times** you have eaten a food in the last 'eaten food queue'(default 128, configurable). The first time is always worth full value; subsequentinal consumptions lower the value over a default of 5, then it bottoms out
            - `diminishingEatsToFloor` (default 5)
            - `diminishingFloorHunger` (default 1) and `diminishingFloorSaturation` (default 0.5): what a bottomed-out food restores.
            - `diminishingRecoveryVal` (default 1.0): how fast a worn-out food climbs back to full as you eat other things. 1.0 spreads recovery across the whole recent-food queue, so a food is back to normal exactly as it ages out. Below 1.0 is slower, above 1.0 is faster, and 0.0 disables recovery so a food only resets by ageing out.
 - Fixed the penalty being applied twice to any meal that would have overfilled the hunger bar. 

3. Added AppleSkin Dependency
 - Added support for awesome AppleSkin tooltip and HUD previews. they now show the diminished values instead of the food's raw ones, really happy with how this turned out

4. Added patchouli Dependency for proper food book!
 - **Patchouli is now a required dependency.** 
 - The book now opens on a menu: buttons down the left page, and a live detail panel on the right that follows whichever button you hover. Clicking a button opens its entry.
 - New "Yet to Try" page: every food in the pack you have never eaten, worth the most first, without giving away values (unless config option is turned off). Gives the player some sort of incentive for the quickest hearts, open to feedback on this 
 - The book now shows the pack's total points and hearts available from every food in the pack, plus the amount you've had. `/solpotpie stats` and the server log report the same totals.
 - The overview now draws progress bars for your next heart, and the total amount of food you've tried.
 - New food book texture.
 - Holding the food book keybind no longer reopens the book every tick.
 - The keybind only opens the book if you are actually carrying one (hotbar, inventory, offhand or armor slots). No more phantom book out of thin air
 - The "Yet to Try" page now reads the config: the line telling you to hover a food for its score only shows up when the score is actually there to read, so with `hideValuesUntilEaten` on (or tooltips off entirely) the book stops promising something it can't deliver

5. Tooltip updates
 - Your most recent meal now reads as a streak: "Most recent meal", "eaten twice in a row", "eaten seven times in a row".

6. Config now in the global `config/` folder

7. Other
 - The food book is now crafted from a book and a pot pie, instead of a book and a potato. 
 - `/solpotpie stats` now reports points, hearts and foods tasted against the pack totals. The output format changed, dont think anyone cares.

## 1.1.0
 - Added all items to creative tab
 - Added new config option to blacklist certain foods from showing ALL SoL:Pot Pie tooltips!

## 1.0.0

 - Forked from Spice of Life: Apple Pie Edition (1.20.1); rebranded to Spice of Life: Pot Pie Edition (`solpotpie`).
 - Replaced the threshold/benefit diversity system with permanent hearts: each unique food eaten adds its score to lifetime points; every 8 points (configurable) grants +1 permanent heart (configurable cost, growth, cap, and health per heart).
 - Food scores are computed automatically from each food's own nutrition and saturation
 - Added diminishing returns: recently eaten foods restore less hunger/saturation, scaling with how recently they were eaten.
 - Food scores are hidden until first taste ("Not yet eaten. What does it taste like?")
 - Reset-on-death now only clears the recent-food queue (off by default); hearts are always permanent.
