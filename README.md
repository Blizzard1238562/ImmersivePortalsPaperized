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
- _**/immersiveportalspaper create**_ Create a new portal
- _**/immersiveportalspaper createcustom**_ Create a custom portal
- _**/immersiveportalspaper createfromcoords**_ Create a portal from coordinates
- _**/immersiveportalspaper link**_ Link two portal locations together
- _**/immersiveportalspaper linkexternal**_ Create a cross-server portal
- _**/immersiveportalspaper remove**_ Remove the closest portal
- _**/immersiveportalspaper removebyname**_ Remove portals by name
- _**/immersiveportalspaper reload**_ Reload the plugin configuration
- _**/immersiveportalspaper wand**_ Get the portal selection wand
- _**/immersiveportalspaper tp**_ Teleport to a portal destination
- _**/immersiveportalspaper setprice**_ Set the price for using a portal
- _**/immersiveportalspaper setpreset**_ Set the preset for a portal
- _**/immersiveportalspaper getname**_ Get the name of a portal
- _**/immersiveportalspaper getportalname**_ Get the name of the nearest portal
- _**/immersiveportalspaper setPortalName**_ Set a custom name for a portal
- _**/immersiveportalspaper setOrigin**_ Set the selection as the origin position
- _**/immersiveportalspaper setDestination**_ Set the selection as the destination position
- _**/immersiveportalspaper menu**_ Open the Portal Admin GUI
- _**/immersiveportalspaper reconnect**_ Reconnect to the proxy if disconnected
- _**/immersiveportalspaper setseethroughportal**_ Toggle see-through portals for yourself
- _**/immersiveportalspaper toggleseethroughportal**_ Toggle see-through portal visibility
- _**/immersiveportalspaper getallowNonPlayerTeleportation**_ Check if items and mobs can teleport
- _**/immersiveportalspaper setAllowNonPlayerTeleportation**_ Allow or disallow item and mob teleportation

**Alias:**
- _**/p**_ Can be used instead of _**/immersiveportalspaper**_

**Permissions:**
- _**immersiveportalspaper.createfromcoords**_ Allows creating portals from coordinates
- _**immersiveportalspaper.getname**_ Allows getting portal names
- _**immersiveportalspaper.link**_ Allows linking portals
- _**immersiveportalspaper.linkexternal**_ Allows creating cross-server portals
- _**immersiveportalspaper.removeclosest**_ Allows removing the closest portal
- _**immersiveportalspaper.removebyname**_ Allows removing portals by name
- _**immersiveportalspaper.reload**_ Allows reloading the plugin configuration
- _**immersiveportalspaper.create**_ Allows creating portals
- _**immersiveportalspaper.updatecoords**_ Allows updating portal coordinates
- _**immersiveportalspaper.tp**_ Allows teleporting to portal destinations
- _**immersiveportalspaper.setprice**_ Allows setting portal prices
- _**immersiveportalspaper.setpreset**_ Allows setting portal presets
- _**immersiveportalspaper.bypassprice**_ Allows bypassing portal usage costs
- _**immersiveportalspaper.admin**_ Allows bypassing portal size restrictions and viewing debug information
- _**immersiveportalspaper.createcustom**_ Allows creating custom portals
- _**immersiveportalspaper.wand**_ Allows using the portal selection wand
- _**immersiveportalspaper.reconnect**_ Allows reconnecting to the proxy
- _**immersiveportalspaper.user**_ Allows using portals *(default: true)*
- _**immersiveportalspaper.see**_ Allows seeing through portals *(default: true)*

**Dependencies:**
- ProtocolLib (required, used for packet-based portal rendering)
- Vault (optional, for economy features)
- Multiverse-Core (optional, for world management)
- My_Worlds (optional, for world management)

**Configuration:**
The plugin configuration file will be generated on first startup at `plugins/ImmersivePortalsPaper/config.yml`.

The plugin supports hot-reloading via _**/immersiveportalspaper reload**_.

For detailed configuration options, including world connections, portal effect presets, proxy settings, and performance tuning, refer to the comments in the generated config.yml.

Have fun :)
</details>
