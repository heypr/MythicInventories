## MythicInventories — MC 1.20.6+
A simple way to go about generating inventories for MythicMobs.

> [!IMPORTANT]
> The one and only dependency is [MythicMobs](https://mythiccraft.io/index.php?resources/mythicmobs.1/), however the plugin can be used without it, the mm_skill option will just not work.

## Download
You can download the latest version of MythicInventories [here](https://ci.heypr.dev/job/MythicInventories/).

## Support
If you're having difficulty figuring out how to work with the plugin, need to report a bug, or are interested in 
contributing to the project, please [join my support Discord](https://discord.gg/Drgk3CxrtV/)!

## Usage
Making inventories is pretty simple. All you need to do is open up the MythicInventories folder, create a .yml file with a name of your choice, then create one! 
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
```

## Options

- The `name` option is for setting the name of the inventory or the item. It is not required on either. If not set 
  on the inventory, the name will default to "Container" & if not set on the item, it will default to the item's
  material type.


- The `size` option is for setting the size of the inventory. It is optional, and should be a multiple of 9 and 
  greater than 0. If it is not specified, it will be set to 9 by default.


- The `items` option is for setting the items in the inventory. Ideally you should add items to your inventory.


- The `type` option is for setting the material type of the item. It is required.


- The `slot` option is for setting the slot of the item. It is required.


- The `lore` option is for setting the lore of the item. It is optional.


- The `gui` option is for setting whether all items in the inventory can be picked up and manipulated. It is `true` by 
default. Do note, that enabling this option will *not* allow players to modify anything in the inventory unless 
explicitly set through the `interactable` option (see below).


- The `fill_item` option is for items that need to fill the inventory. It is `false` by default, and is optional. 
  Please note that only one item can be a fill item.


- The `interactable` option is for setting whether the item can be picked up and manipulated. It is `false` by 
default, and is optional.


- The `save` option is for setting whether the item should be saved. It is `false` by default, and is optional.

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

- The `item_flags` option is for setting flags on the item. It is empty by default, and is optional.
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

## Commands
| Command                                       | Description                                                                 | Permission                              | Aliases                            |
|-----------------------------------------------|-----------------------------------------------------------------------------|-----------------------------------------|------------------------------------|
| `mythicinventoryopen <inventory_id> [player]` | Opens the specified inventory for yourself or a target player if specified. | `mythicinventories.open.<inventory_id>` | `mio`, `miopen`, `mythicio`        |
| `mythicinventoryreload`                       | Updates and reloads all inventories.                                        | `mythicinventories.reload`              | `mir`, `mireload`, `mythicireload` |
| `migrateolddata`                              | Migrates your old save data from <0.7.0 versions of the plugin.             | `mythicinventories.migrate`             | `migrateold`, `migrate`            |
