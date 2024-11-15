## MythicInventories — MC 1.20.6+
A simple way to go about generating inventories for MythicMobs.

> [!IMPORTANT]
> The one and only dependency is MythicMobs. 

## Download
You can download the latest version of MythicInventories [here](https://ci.heypr.dev/job/MythicInventories/).

## Support
If you're having difficulty figuring out how to work with the plugin, [join my support Discord](https://discord.gg/Drgk3CxrtV/)!

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
    - name: "<red>Summon lightning strike"
      type: skeleton_skull
      slot: 23
      lore:
      - "<red>lorem ipsum something something"
      - "<blue>Woo: <gradient>||||||||||||||||||||||||</gradient>!"
      mm_skill: lightning_skill
      click_type: shift_left_click
```

The "fill_item" option is for items that need to fill the inventory. It is `false` by default.

The "click_type" option is for setting the click type for the item. It is not set by default.
Make sure to set this value to a valid click type if you want a skill to be executed when the item is clicked.
Valid click types are as follows: 
- LEFT_CLICK
- RIGHT_CLICK
- SHIFT_LEFT_CLICK
- SHIFT_RIGHT_CLICK
- MIDDLE_CLICK
- SHIFT_MIDDLE_CLICK
- DROP
- HOTBAR_SWAP

The "item_flags" option is for setting flags on the item. It is empty by default.
Valid values for item flags are as follows:
- HIDE_ENCHANTS,
- HIDE_ATTRIBUTES,
- HIDE_UNBREAKABLE,
- HIDE_DESTROYS,
- HIDE_PLACED_ON,
- HIDE_ADDITIONAL_TOOLTIP,
- HIDE_DYE,
- HIDE_ARMOR_TRIM,
- HIDE_STORED_ENCHANTS;

## Commands
| Command                              | Description                                                                                               | Permission                             | Aliases                                  |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------|----------------------------------------|------------------------------------------|
| `mythicinventoryopen <inventory_id> [player]` | Opens the specified inventory for yourself or a target player if specified.                               | `mythicinventories.open.<inventory_id>` | `mio`, `miopen`, `mythicio`              |
| `mythicinventoryreload`              | Updates and reloads all inventories.                                                                      | `mythicinventories.reload`             | `mir`, `mireload`, `mythicireload`       |
