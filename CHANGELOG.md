# Changelog

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
