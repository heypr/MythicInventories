## MythicInventories — MC 1.21.4+
A simple way to go about generating inventories for MythicMobs.

> [!NOTE]
> A better, more structured, and more visually appealing documentation site is planned.

Quick Links:
<!-- TOC -->
* [Download](#download)
* [Support](#support)
* [Usage](#usage)
* [Options](#options)
* [Commands](#commands)
<!-- TOC -->

> [!IMPORTANT]
> The one and only dependency is [MythicMobs](https://mythiccraft.io/index.php?resources/mythicmobs.1/), however the 
> plugin can be used without it, anything involving MythicMobs (like the click skills and the trinket stuff) will 
> simply not function.

Planned updates:
- MythicStats support for trinkets
- Better documentation site
- Enchants option
- Vanilla item attributes option
- Custom NBT option (with support for adding items in-hand) for external plugin item support

## Download
You can download the latest version of MythicInventories [here](https://ci.heypr.dev/job/MythicInventories/).

## Support
If you're having difficulty figuring out how to work with the plugin, need to report a bug, or are interested in 
contributing to the project, please [join my support Discord](https://discord.gg/Drgk3CxrtV/)!

## Usage
Making inventories is pretty simple. All you need to do is open up the MythicInventories folder, create a .yml file 
with a name of your choice, then get to customizing an inventory! 
> [!NOTE]
> This plugin supports [MiniMessage](https://github.com/Minevictus/MiniMessage/blob/master/DOCS.md). Go crazy with colors!

Here's a basic layout for an inventory that you could make:
```denizenscript
my_first_inventory:
  name: "<green>My <blue>Epic &cInventory"
  size: 45
  items:
    - name: "<black>"
      type: black_stained_glass_pane
      fill_item: true
    - name: "<red>Summon explosion and particles"
      type: potato
      slot: 23
      lore:
        - "<red>lorem ipsum something something"
        - "<blue>Woo: <gradient>||||||||||||||||||||||||</gradient>!"
      left_click:
        - effect:explosion @Self
      drop:
        - effect:particles{p=happyVillager;amount=1000;hSpread=15;ySpread=1;speed=0;yOffset=0.5} @Self
      save: false
      interactable: false
      item_flags:
        - HIDE_ATTRIBUTES
    - type: mythic:KingsCrown
      slot: 25
      unbreakable: true
    - type: air
      slot: 20
      trinket: true
      save: true
      interactable: true
```
---
Options
---
The `name` option is for setting the name of the inventory or the item.
It is not required on either. If not set on the inventory, the name will default to "Container" and if not set on the 
item, it will default to the item's material type.
---
The `size` option is for setting the size of the inventory.
It is optional, and should be a multiple of 9 and greater than 0. If it is not specified, it will be set to 9 by default.
---
The `items` option is for setting the items in the inventory.
Ideally you should add items to your inventory.
---
The `type` option is for setting the material type of the item.
It is required.
---
The `slot` option is for setting the slot of the item.
It is required.
---
The `lore` option is for setting the lore of the item.
It is optional.
---
The `fill_item` option is for items that need to fill the inventory.
It is `false` by default, and is optional. Please note that only one item can be a fill item.
---
The `interactable` option is for setting whether the item can be picked up and manipulated. It is `false` by 
default, and is optional.
---
The `save` option is for setting whether the item should be saved.
It is `false` by default, and is optional.
---
The following are the varying click types that you can have MythicInventories listen for to run a skill:
  - `left_click`
  - `right_click`
  - `shift_left_click`
  - `shift_right_click`
  - `middle_click`
  - `shift_middle_click`
  - `drop`
  - `hotbar_swap`

Each item can have multiple click types, each click type can have multiple skills, and each skill is limited only by what you can do with MythicMobs skills.
See the above example inventory for a basic layout of how to set up a skill.
---
The `trinket` option is for setting a slot as a trinket slot.
This makes MythicInventories treat items that are already filled in this slot, or are put into this slot by a player,
as a trinket item. Trinkets are currently only supported for items from MythicMobs, as defined through a configuration section within the item itself.
```yaml
# Example MythicMobs item for Trinkets
KingsCrown:
  Id: GOLDEN_HELMET
  Display: '&dCrown of the King'
  Lore:
    - '&6A kingly crown!'
  Trinket: # this is a new section that you can now add to your Mythic Items
    skill: effect:particles{p=happyVillager;amount=1000;hSpread=15;ySpread=1;speed=0;yOffset=0.5} @Self
    # as seen in the inventory item above, an inline skill that is run based on the parameters below.
    interval: 60 
    # interval, in ticks, that the above inline skill(s) should execute
    uses: 10 
    # the amount of times that the above skill(s) will execute every interval before stopping. the item stops running the skill after its uses are up
    initial_item_disappears: false
    # if the item you have defined within the MythicInventory is *not* 
    attributes:
      # supports all vanilla attributes, mythic stats support coming soon
      MAX_HEALTH:
        type: add # supported types: add, subtract, multiply
        amount: 25 # I hope I don't have to explain what a number is...
```
---
> [!WARNING]
> Do keep in mind that the above is *not* what should be added to any item within MythicInventories inventory 
> configuration, but instead inside any valid MythicMobs item within the MythicMobs plugin folder. As defined in the 
> inventory section at the top, you can place this in any slot that has the interactable option enabled, but *no 
> skills will run, and no attributes will be applied to the player*.
---
The `item_flags` option is for setting flags on the item. It is empty by default, and is optional.
Valid values for item flags are as follows:
  - `HIDE_ENCHANTS`,
  - `HIDE_ATTRIBUTES`,
  - `HIDE_UNBREAKABLE`,
  - `HIDE_DESTROYS`,
  - `HIDE_PLACED_ON`,
  - `HIDE_ADDITIONAL_TOOLTIP`,
  - `HIDE_DYE`,
  - `HIDE_ARMOR_TRIM`,
  - `HIDE_STORED_ENCHANTS`;
---
## Commands
| Command                             | Description                                                                 | Permission                              | Aliases                            |
|-------------------------------------|-----------------------------------------------------------------------------|-----------------------------------------|------------------------------------|
| `mythicinventoryopen <id> [player]` | Opens the specified inventory for yourself or a target player if specified. | `mythicinventories.open.<inventory_id>` | `mio`, `miopen`, `mythicio`        |
| `mythicinventoryreload`             | Updates and reloads all inventories.                                        | `mythicinventories.reload`              | `mir`, `mireload`, `mythicireload` |
