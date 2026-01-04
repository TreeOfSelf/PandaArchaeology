![image](https://github.com/user-attachments/assets/ba1a9b70-e39e-47fb-8d44-0bde5a982723)

# PandaArchaeology

## Description

A fun mod to spice up a server. Fish up or brush up despawned loot! When loot despawns, it is added to a pool of items. Only 10 of each item type (configurable) can exist in this pool. When doing archeology or fishing, there is a chance (configurable) to find this despawned loot. It will display the date it was dropped and by who it was dropped if it was dropped by a player.

## Configuring

config/PandaArchaeology.json generated at runtime

```
{
  "activeForFishing": true,      //Whether to activate for fishing
  "activeForBrushing": true,     //Whether to activate for brushing
  "onlyPlayerOwned": false,      //Only player dropped items will be added 
  "itemLimit": 10,               //The max amount of any item type (set 0 to disable)
  "fishingChance": 60,           //The 1 in x chance to find a despawned item when fishing 
  "brushChance": 10              //The 1 in x chance to find a despawned item when brushing 
  "luckMultiplier": 3            //How strongly luck improves your chances of discovery
}
```

## Try it out
`hardcoreanarchy.gay`   (Deathban Anarchy)  
`sky.hardcoreanarchy.gay`   (Skyblock Anarchy)

## Support

[Support discord here!]( https://discord.gg/3tP3Tqu983)

## License

[CC0](https://creativecommons.org/public-domain/cc0/)
