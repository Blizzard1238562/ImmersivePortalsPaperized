# ImmersivePortalsPaperized
An attempt at making Immersive Portals but completely server-side as a Paper plugin.

This Repo includes code from both [NetherView](https://github.com/GorgeousOne/NetherView) and [BetterPortals](https://github.com/OpenCommunity-Original/BetterPortals)

<details>
<summary>Modrinth Description:</summary>
# Immersive Portals Paperized
A highly-optimized, server-side portal rendering and teleportation engine for PaperMC. Inspired by the Immersive Portals mod

### Plugin

A Minecraft plugin that allows nether portals to be seen through, it displays whats on the other side in real-time, and supports custom portal creation. Everything without any client modifications.

⚠️ **Currently in Alpha, don't expect a stable plugin!**

⚠️ **Spigot support is not guaranteed as it uses extensive PaperAPI features!**

**Please Message me if you encounter any Bugs!**
**Discord: blizzard8562 _(for Bug-Reports, other Issues and Feedback)_**

**Current Features:**
- See-through nether portals
- Real-time portal rendering
- Custom portal creation
- Cross-server portal support
- Portal effects and presets
- Economy integration (Vault)
- Selection wand
- Folia compatible
- GUI admin menu
- TPS-aware rendering
- Portal admin GUI
- Anti-dupe protection
- Multi-world support
- Dimension blending
- Light block system (1.18+)
- Internationalization (25+ languages)
- Automatic update checks via Modrinth

**Future Features:**
- _Entity/Player Pass through_
- _Entity tracking and replication_ (both are theoretically implemented but they dont work as they should)
- Making the Plugin more stable
- Performance optimizations

**Commands:**

All commands are under `/immersiveportalspaperized`, which can also be shortened to `/p` or `/bp`.

- _**/bp reload**_ Reloads the plugin and the config file
- _**/bp reconnect**_ Reconnects to the proxy if the connection dropped
- _**/bp wand**_ Gives you the portal selection wand
- _**/bp setOrigin**_ (alias `origin`) Sets your current wand selection as the origin position
- _**/bp setDestination**_ (alias `destination`/`dest`) Sets your current wand selection as the destination position
- _**/bp linkPortals [twoWay] [invert]**_ (alias `link`) Links your origin and destination selections together
- _**/bp linkExternalPortals [invert]**_ (alias `linkexternal`) Links your origin selection with a destination selection on another server
- _**/bp createfromcoords <originWorld> <originCorner1> <originCorner2> <destWorld> <destCorner1> <destCorner2> [twoWay] [invert] [name]**_ Creates a portal directly from coordinates, without needing a player or wand
- _**/bp remove [removeDestination]**_ (aliases `delete`/`del`) Removes the nearest portal within 20 blocks
- _**/bp removebyname <portalName>**_ (alias `deletename`) Removes all portals with the given name
- _**/bp setPortalName <name>**_ (alias `setname`) Sets the name of the nearest custom portal within 20 blocks
- _**/bp getportalname**_ (alias `getname`) Tells you the name of the nearest portal within 20 blocks
- _**/bp setprice <price>**_ Sets the price of the nearest custom portal
- _**/bp setpreset <preset>**_ Sets the effect preset of the nearest custom portal
- _**/bp getallowNonPlayerTeleportation**_ (alias `getcanteleportmobs`) Tells you whether the nearest portal allows non-player (item/mob) teleportation
- _**/bp setAllowNonPlayerTeleportation <true|false>**_ (alias `setcanteleportmobs`) Sets whether the nearest portal allows non-player (item/mob) teleportation
- _**/bp setseethroughportal <true|false>**_ (alias `setenablebpview`) Sets whether you personally can see through portals
- _**/bp toggleseethroughportal**_ (alias `togglevanillaview`) Toggles whether you personally can see through portals
- _**/bp menu**_ (aliases `list`/`admin`/`gui`) Opens the Portal Admin GUI

To build a custom portal: grab the wand (`/bp wand`), select your origin corners and run `/bp setOrigin`, select your destination corners and run `/bp setDestination`, then `/bp linkPortals` (or `/bp linkExternalPortals` for a cross-server portal).

**Permissions:**

- _**immersiveportalspaperized.createfromcoords**_ Allows creating portals from coordinates
- _**immersiveportalspaperized.getname**_ Allows getting portal names
- _**immersiveportalspaperized.link**_ Allows linking portals
- _**immersiveportalspaperized.linkexternal**_ Allows creating cross-server portals
- _**immersiveportalspaperized.remove**_ Allows removing portals (`/bp remove`, `/bp removebyname`)
- _**immersiveportalspaperized.remove.others**_ Allows removing portals you don't own
- _**immersiveportalspaperized.reload**_ Allows reloading the plugin configuration
- _**immersiveportalspaperized.reconnect**_ Allows reconnecting to the proxy
- _**immersiveportalspaperized.select**_ Allows using the selection wand workflow (`/bp setOrigin`, `/bp setDestination`) and the admin menu
- _**immersiveportalspaperized.setname**_ Allows renaming portals and setting their price/preset (`/bp setPortalName`, `/bp setprice`, `/bp setpreset`)
- _**immersiveportalspaperized.setname.others**_ Allows renaming portals you don't own
- _**immersiveportalspaperized.getallowNonPlayerTeleportation**_ Allows checking if items/mobs can teleport through a portal
- _**immersiveportalspaperized.setAllowNonPlayerTeleportation**_ Allows toggling item/mob teleportation for a portal
- _**immersiveportalspaperized.wand**_ Allows using the portal selection wand
- _**immersiveportalspaperized.user**_ Allows using portals *(default: true)*
- _**immersiveportalspaperized.see**_ Allows seeing through portals *(default: true)*

By default, everyone can walk through and see through portals (`.user`/`.see`). Everything related to creating, editing or removing portals defaults to OP.

**Dependencies:**
- PacketEvents (required, used for packet-based portal rendering)
- Vault (optional, for economy features)
- Multiverse-Core (optional, for world management)
- My_Worlds (optional, for world management)

**Configuration:**
The plugin configuration file will be generated on first startup at `plugins/ImmersivePortalsPaperized/config.yml`.

The plugin supports hot-reloading via _**/bp reload**_.

For detailed configuration options, including world connections, portal effect presets, proxy settings, and performance tuning, refer to the comments in the generated config.yml.

Have fun :)
</details>

## Build Instructions:

## Contribute:
If you wish to contribute to this Project please open a PR with a detailed explanation of your change.
