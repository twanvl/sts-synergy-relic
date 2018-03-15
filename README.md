# Synergistic card choices for Slay the Spire

Adds a relic (Bonsai Tree) to the game that offers synergistic cards at card reward screens.
By clicking on the small tree in the menu, you can configure whether to start with this relic.

Bonsai image from https://en.wikipedia.org/wiki/File:Eurya,_1970-2007.jpg, used under Creative Commons by-sa license.

## How does it work? ##

This mod has a predfined list of synergistic card choices. For example *Blade Dance* synergizes with cards that benefit from Shivs (like *Accuracy*) and also cards that benefit from cheap attacks (*Envenom*). When the card reward screen is loaded, the mod looks at all cards in the deck, as well as all your relics, to build a list of possible *synergy cards*. Then one of them is selected at random. The selection is weighted, so if there is more synergy with a certain card it is more likely to show up.

To not saddle you with a million *Dropkicks* and *Underhanded Strikes*, the starting cards have a very low weight.


## Requirements ##
* ModTheSpire (https://github.com/t-larson/ModTheSpire/releases)
* BaseMod (https://github.com/daviscook477/BaseMod/releases)
* Java 8+

## Installation ##
1. [Download `ModTheSpire.jar`](https://github.com/kiooeht/ModTheSpire/releases)
2. Move `ModTheSpire.jar` into your **Slay The Spire** directory. This directory is likely to be found under `C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire`.
3. Create a `mods` folder in your **Slay The Spire** directory
4. [Download `BaseMod.jar`](https://github.com/daviscook477/BaseMod/releases), and place it in the `mods` folder.
5. [Download `SynergyRelic.jar`](https://github.com/twanvl/sts-synergy-relic/releases), and place it in the `mods` folder.
6. Your modded version of **Slay The Spire** can now be launched by double-clicking on `ModTheSpire.jar`
7. This will open a mod select menu where you need to make sure that both `BaseMod` and `SynergyRelic` are checked before clicking **play**

