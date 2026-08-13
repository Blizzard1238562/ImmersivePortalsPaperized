# Immersive Portals Paperized
A highly-optimized, server-side portal rendering and teleportation engine for PaperMC. Inspired by the Immersive Portals mod. I made a port for modern versions based of the code from NetherView and BetterPortals.

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
- Entity tracking and replication (mobs, items, and other entities are visible through portals)
- Internationalization (25+ languages)

**Future Features:**
- _Entity/Player Pass through_ (walking through a portal's surface to teleport, without needing a vanilla portal block)
- Making the Plugin more stable
- Performance optimizations
- Automatic update checks via Modrinth

**Commands:**

All commands are under `/immersiveportalspaperized`, which can also be shortened to `/p` or `/bp`.

- _**/bp reload**_ Reloads the plugin and the config file
- _**/bp reconnect**_ Reconnects to the proxy if the connection dropped
- _**/bp wand**_ Gives you the portal selection wand
- _**/bp setOrigin**_ Sets your current wand selection as the origin position
- _**/bp setDestination**_ Sets your current wand selection as the destination position
- _**/bp linkPortals**_ Links your origin and destination selections together
- _**/bp linkExternalPortals**_ Links your origin selection with a destination selection on another server
- _**/bp createfromcoords**_ Creates a portal directly from coordinates, without needing a player or wand
- _**/bp remove**_ Removes the nearest portal within 20 blocks
- _**/bp removebyname**_ Removes all portals with the given name
- _**/bp setPortalName**_ Sets the name of the nearest custom portal within 20 blocks
- _**/bp getportalname**_ Tells you the name of the nearest portal within 20 blocks
- _**/bp setprice**_ Sets the price of the nearest custom portal
- _**/bp setpreset**_ Sets the effect preset of the nearest custom portal
- _**/bp getallowNonPlayerTeleportation**_ Tells you whether the nearest portal allows non-player (item/mob) teleportation
- _**/bp setAllowNonPlayerTeleportation**_ Sets whether the nearest portal allows non-player (item/mob) teleportation
- _**/bp setseethroughportal**_ Sets whether you personally can see through portals
- _**/bp toggleseethroughportal**_ Toggles whether you personally can see through portals
- _**/bp menu**_ Opens the Portal Admin GUI

**Alias:**
- _**/p**_ and _**/bp**_ Can be used instead of _**/immersiveportalspaperized**_

**Permissions:**
- _**immersiveportalspaperized.createfromcoords**_ Allows creating portals from coordinates
- _**immersiveportalspaperized.getname**_ Allows getting portal names
- _**immersiveportalspaperized.link**_ Allows linking portals
- _**immersiveportalspaperized.linkexternal**_ Allows creating cross-server portals
- _**immersiveportalspaperized.remove**_ Allows removing portals
- _**immersiveportalspaperized.remove.others**_ Allows removing portals you don't own
- _**immersiveportalspaperized.reload**_ Allows reloading the plugin configuration
- _**immersiveportalspaperized.reconnect**_ Allows reconnecting to the proxy
- _**immersiveportalspaperized.select**_ Allows using the selection wand and the admin menu
- _**immersiveportalspaperized.setname**_ Allows renaming portals and setting their price/preset
- _**immersiveportalspaperized.setname.others**_ Allows renaming portals you don't own
- _**immersiveportalspaperized.getallowNonPlayerTeleportation**_ Allows checking if items/mobs can teleport through a portal
- _**immersiveportalspaperized.setAllowNonPlayerTeleportation**_ Allows toggling item/mob teleportation for a portal
- _**immersiveportalspaperized.wand**_ Allows using the portal selection wand
- _**immersiveportalspaperized.user**_ Allows using portals *(default: true)*
- _**immersiveportalspaperized.see**_ Allows seeing through portals *(default: true)*

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
