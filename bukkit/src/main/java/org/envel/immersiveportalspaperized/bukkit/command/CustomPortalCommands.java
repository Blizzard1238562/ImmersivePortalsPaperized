package org.envel.immersiveportalspaperized.bukkit.command;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.command.framework.CommandException;
import org.envel.immersiveportalspaperized.bukkit.command.framework.CommandTree;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Aliases;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Argument;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Arguments;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Command;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Description;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Path;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.RequiresPermissions;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.RequiresPlayer;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.gui.PortalAdminGUI;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerData;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import org.envel.immersiveportalspaperized.bukkit.portal.selection.IPortalSelection;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
public class CustomPortalCommands {
   private static final String[] EASTER_EGG_NAMES = new String[]{"dinnerbone"};
   private static final double MODIFY_DISTANCE = 20.0;
   private final IPortalManager portalManager;
   private final MessageConfig messageConfig;
   private final IPortal.Factory portalFactory;
   private final Provider<IPortalSelection> selectionProvider;
   private final PortalAdminGUI adminGUI;

   @Inject
   public CustomPortalCommands(
      CommandTree commandTree,
      IPortalManager portalManager,
      MessageConfig messageConfig,
      IPortal.Factory portalFactory,
      Provider<IPortalSelection> selectionProvider,
      PortalAdminGUI adminGUI
   ) {
      this.portalManager = portalManager;
      this.messageConfig = messageConfig;
      this.portalFactory = portalFactory;
      this.selectionProvider = selectionProvider;
      this.adminGUI = adminGUI;
      commandTree.registerCommands(this);
   }

   @NotNull
   private IPortal getClosestPortal(Player player) throws CommandException {
      IPortal portal = this.portalManager.findClosestPortal(player.getLocation(), MODIFY_DISTANCE);
      if (portal == null) {
         throw new CommandException(this.messageConfig.getErrorMessage("noPortalCloseEnough"));
      } else {
         return portal;
      }
   }

   @Command
   @Path("immersiveportalspaperized/removebyname")
   @RequiresPermissions({"immersiveportalspaperized.remove"})
   @Description("Removes all portals with the given name")
   @Argument(
      name = "portalName"
   )
   @Aliases({"deletename"})
   public boolean removePortalsByName(CommandSender sender, String portalName) throws CommandException {
      List<IPortal> toRemove = this.portalManager.getAllPortals().stream().filter(portal -> portalName.equals(portal.getName())).toList();
      if (toRemove.isEmpty()) {
         throw new CommandException(this.messageConfig.getErrorMessage(sender, "noPortalsWithName").replace("{name}", portalName));
      } else {
         toRemove.forEach(this.portalManager::removePortal);
         sender.sendMessage(this.messageConfig.getChatMessage(sender, "portalsRemoved"));
         return true;
      }
   }

   @Command
   @Path("immersiveportalspaperized/createfromcoords")
   @RequiresPermissions({"immersiveportalspaperized.createfromcoords"})
   @Description("Creates a portal from the coordinates of its corners without requiring a player")
   @Arguments({@Argument(
         name = "originWorld"
      ), @Argument(
         name = "originCorner1"
      ), @Argument(
         name = "originCorner2"
      ), @Argument(
         name = "destWorld"
      ), @Argument(
         name = "destCorner1"
      ), @Argument(
         name = "destCorner2"
      ), @Argument(
         name = "twoWay?",
         defaultValue = "false"
      ), @Argument(
         name = "invert?",
         defaultValue = "false"
      ), @Argument(
         name = "name",
         defaultValue = " no name"
      )})
   public boolean createFromCoordinates(
      CommandSender sender,
      World originWorld,
      Vector originCorner1,
      Vector originCorner2,
      World destWorld,
      Vector destCorner1,
      Vector destCorner2,
      String twoWayStr,
      String invertStr,
      String name
   ) throws CommandException {
      if (" no name".equals(name)) {
         name = null;
      }

      boolean twoWay = twoWayStr.equalsIgnoreCase("true") || twoWayStr.equalsIgnoreCase("twoWay") || twoWayStr.equalsIgnoreCase("dual");
      boolean invert = invertStr.equalsIgnoreCase("true") || invertStr.equalsIgnoreCase("invert");
      IPortalSelection origin = this.makeSelection(sender, originWorld, originCorner1, originCorner2);
      IPortalSelection dest = this.makeSelection(sender, destWorld, destCorner1, destCorner2);
      if (!origin.getPortalSize().equals(dest.getPortalSize())) {
         throw new CommandException(this.messageConfig.getErrorMessage(sender, "differentSizes"));
      } else {
         if (invert) {
            dest.invertDirection();
         }

         IPortal portal = this.portalFactory
            .create(origin.getPortalPosition(), dest.getPortalPosition(), origin.getPortalSize(), true, UUID.randomUUID(), null, name, true);
         this.portalManager.registerPortal(portal);
         if (twoWay) {
            IPortal reversePortal = this.portalFactory
               .create(dest.getPortalPosition(), origin.getPortalPosition(), origin.getPortalSize(), true, UUID.randomUUID(), null, name, true);
            this.portalManager.registerPortal(reversePortal);
         }

         sender.sendMessage(this.messageConfig.getChatMessage(sender, "portalCreated"));
         return true;
      }
   }

   private IPortalSelection makeSelection(CommandSender sender, World world, Vector corner1, Vector corner2) throws CommandException {
      IPortalSelection selection = this.selectionProvider.get();
      selection.setPositionA(corner1.toLocation(world));
      selection.setPositionB(corner2.toLocation(world));
      if (!selection.isValid()) {
         throw new CommandException(
            String.format(
               this.messageConfig.getErrorMessage(sender, "coordinatesNotInLine"),
               corner1.getBlockX(),
               corner1.getBlockY(),
               corner1.getBlockZ(),
               corner2.getBlockX(),
               corner2.getBlockY(),
               corner2.getBlockZ()
            )
         );
      } else {
         return selection;
      }
   }

   @Command
   @Path("immersiveportalspaperized/remove")
   @RequiresPermissions({"immersiveportalspaperized.remove"})
   @RequiresPlayer
   @Aliases({"delete", "del"})
   @Description("Removes the nearest portal within 20 blocks of the player")
   @Argument(
      name = "removeDestination?",
      defaultValue = "true"
   )
   public boolean deleteNearest(Player player, boolean removeDestination) throws CommandException {
      IPortal portal = this.getClosestPortal(player);
      if (!player.hasPermission("immersiveportalspaperized.remove.others") && !player.getUniqueId().equals(portal.getOwnerId())) {
         throw new CommandException(this.messageConfig.getErrorMessage(player, "removeNotOwnedByPlayer"));
      } else {
         this.portalManager.removePortal(portal);
         if (removeDestination && !portal.isCrossServer()) {
            Location destPosition = portal.getDestPos().getLocation();
            this.portalManager.removePortalsAt(destPosition);
         }

         player.sendMessage(this.messageConfig.getChatMessage(player, "portalRemoved"));
         return true;
      }
   }

   @Command
   @Path("immersiveportalspaperized/setOrigin")
   @Aliases({"origin"})
   @RequiresPermissions({"immersiveportalspaperized.select"})
   @RequiresPlayer
   @Description("Sets the current portal wand selection as your origin position")
   public boolean setOrigin(IPlayerData playerData) throws CommandException {
      playerData.getSelection().trySelectOrigin();
      playerData.getPlayer().sendMessage(this.messageConfig.getChatMessage(playerData.getPlayer(), "originPortalSet"));
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/setDestination")
   @Aliases({"destination", "dest"})
   @RequiresPermissions({"immersiveportalspaperized.select"})
   @RequiresPlayer
   @Description("Sets the current portal wand selection as your destination position")
   public boolean setDestination(IPlayerData playerData) throws CommandException {
      playerData.getSelection().trySelectDestination();
      playerData.getPlayer().sendMessage(this.messageConfig.getChatMessage(playerData.getPlayer(), "destPortalSet"));
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/linkPortals")
   @Aliases({"link"})
   @RequiresPermissions({"immersiveportalspaperized.link"})
   @RequiresPlayer
   @Description("Links the origin and destination portal together")
   @Arguments({@Argument(
         name = "twoWay?",
         defaultValue = "false"
      ), @Argument(
         name = "invert?",
         defaultValue = "false"
      )})
   public boolean linkPortals(IPlayerData playerData, String twoWayStr, String invertStr) throws CommandException {
      boolean twoWay = twoWayStr.equalsIgnoreCase("true") || twoWayStr.equalsIgnoreCase("twoWay") || twoWayStr.equalsIgnoreCase("dual");
      boolean invert = invertStr.equalsIgnoreCase("true") || invertStr.equalsIgnoreCase("invert");
      playerData.getSelection().tryCreateFromSelection(playerData.getPlayer(), twoWay, invert);
      playerData.getPlayer().sendMessage(this.messageConfig.getChatMessage(playerData.getPlayer(), "portalsLinked"));
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/linkExternalPortals")
   @Aliases({"linkexternal"})
   @RequiresPermissions({"immersiveportalspaperized.linkexternal"})
   @RequiresPlayer
   @Description("Links the origin selection on this server with a destination on another server")
   @Argument(
      name = "invert?",
      defaultValue = "false"
   )
   public boolean linkExternalPortals(IPlayerData playerData, boolean invert) throws CommandException {
      playerData.getSelection().tryCreateFromExternalSelection(playerData.getPlayer(), invert);
      playerData.getPlayer().sendMessage(this.messageConfig.getChatMessage(playerData.getPlayer(), "portalsLinked"));
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/wand")
   @RequiresPermissions({"immersiveportalspaperized.wand"})
   @RequiresPlayer
   @Description("Gives you the wand for selecting portals")
   public boolean getPortalWand(Player player) {
      player.getInventory().addItem(new ItemStack[]{this.messageConfig.getPortalWand()});
      return true;
   }

   private void setName(IPortal portal, String name) {
      boolean isEgg = false;

      for (String egg : EASTER_EGG_NAMES) {
         if (egg.equalsIgnoreCase(name) || egg.equalsIgnoreCase(portal.getName())) {
            isEgg = true;
            break;
         }
      }

      if (!isEgg) {
         portal.setName(name);
      } else {
         this.portalManager.removePortal(portal);
         IPortal replacement = this.portalFactory
            .create(portal.getOriginPos(), portal.getDestPos(), portal.getSize(), portal.isCustom(), portal.getId(), portal.getOwnerId(), name, true);
         this.portalManager.registerPortal(replacement);
      }
   }

   @Command
   @Path("immersiveportalspaperized/setPortalName")
   @RequiresPermissions({"immersiveportalspaperized.setname"})
   @Argument(
      name = "newName"
   )
   @Aliases({"setname"})
   @RequiresPlayer
   @Description("Sets the name of the nearest portal within 20 blocks")
   public boolean setName(Player player, String newName) throws CommandException {
      IPortal portal = this.getClosestPortal(player);
      if (!player.hasPermission("immersiveportalspaperized.setname.others") && !player.getUniqueId().equals(portal.getOwnerId())) {
         throw new CommandException(this.messageConfig.getErrorMessage(player, "nameNotOwnedbyPlayer"));
      } else if (portal.isNetherPortal()) {
         throw new CommandException(this.messageConfig.getErrorMessage(player, "nameNetherPortal"));
      } else {
         this.setName(portal, newName);
         player.sendMessage(this.messageConfig.getChatMessage(player, "changedName"));
         return true;
      }
   }

   @Command
   @Path("immersiveportalspaperized/getportalname")
   @RequiresPermissions({"immersiveportalspaperized.getname"})
   @RequiresPlayer
   @Aliases({"getname"})
   @Description("Tells you the name of the nearest portal within 20 blocks")
   public boolean getName(Player player) throws CommandException {
      IPortal portal = this.getClosestPortal(player);
      String name = portal.getName();
      if (name == null) {
         throw new CommandException(this.messageConfig.getErrorMessage(player, "noName"));
      } else {
         String nameFormat = this.messageConfig.getChatMessage(player, "currentName");
         nameFormat = nameFormat.replace("{name}", portal.getName());
         player.sendMessage(nameFormat);
         return true;
      }
   }

   @Command
   @Path("immersiveportalspaperized/getallowNonPlayerTeleportation")
   @RequiresPermissions({"immersiveportalspaperized.getallowNonPlayerTeleportation"})
   @RequiresPlayer
   @Aliases({"getcanteleportmobs"})
   @Description("Tells you whether or not the nearest portal within 20 blocks allows item teleportation")
   public boolean getAllowNonPlayerTeleportation(Player player) throws CommandException {
      IPortal portal = this.getClosestPortal(player);
      if (portal.allowsNonPlayerTeleportation()) {
         player.sendMessage(this.messageConfig.getChatMessage(player, "allowsItems"));
      } else {
         player.sendMessage(this.messageConfig.getChatMessage(player, "doesNotAllowItems"));
      }

      return true;
   }

   @Command
   @Path("immersiveportalspaperized/setAllowNonPlayerTeleportation")
   @RequiresPermissions({"immersiveportalspaperized.setAllowNonPlayerTeleportation"})
   @RequiresPlayer
   @Aliases({"setcanteleportmobs"})
   @Description("Sets whether or not the nearest portal within 20 blocks allows item teleportation")
   @Argument(
      name = "allow"
   )
   public boolean setAllowNonPlayerTeleportation(Player player, boolean allowTeleportation) throws CommandException {
      IPortal portal = this.getClosestPortal(player);
      portal.setAllowsNonPlayerTeleportation(allowTeleportation);
      if (allowTeleportation) {
         player.sendMessage(this.messageConfig.getChatMessage(player, "changedAllowsItems"));
      } else {
         player.sendMessage(this.messageConfig.getChatMessage(player, "changedDoesNotAllowItems"));
      }

      return true;
   }

   @Command
   @Path("immersiveportalspaperized/setseethroughportal")
   @RequiresPermissions({"immersiveportalspaperized.see"})
   @RequiresPlayer
   @Aliases({"setenablebpview"})
   @Description("Sets whether or not the current player is able to see what's on the other side of a portal.")
   @Argument(
      name = "seethroughportal"
   )
   public boolean setSeeThroughPortal(IPlayerData playerData, boolean seeThroughPortal) {
      Player player = playerData.getPlayer();
      if (seeThroughPortal) {
         playerData.getPermanentData().set("seeThroughPortal", true);
         playerData.savePermanentData();
         player.sendMessage(this.messageConfig.getChatMessage(player, "seeThroughPortalEnabled"));
      } else {
         playerData.getPermanentData().set("seeThroughPortal", false);
         playerData.savePermanentData();
         player.sendMessage(this.messageConfig.getChatMessage(player, "seeThroughPortalDisabled"));
      }

      return true;
   }

   @Command
   @Path("immersiveportalspaperized/toggleseethroughportal")
   @RequiresPermissions({"immersiveportalspaperized.see"})
   @RequiresPlayer
   @Aliases({"togglevanillaview"})
   @Description("Toggles whether or not the current player is able to see what's on the other side of a portal.")
   public boolean toggleSeeThroughPortal(IPlayerData playerData) {
      this.setSeeThroughPortal(playerData, !playerData.getPermanentData().getBoolean("seeThroughPortal"));
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/menu")
   @Aliases({"list", "admin", "gui"})
   @RequiresPermissions({"immersiveportalspaperized.select"})
   @RequiresPlayer
   @Description("Opens the Portal Admin GUI menu")
   public boolean openAdminMenu(Player player) {
      this.adminGUI.open(player);
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/setprice")
   @RequiresPermissions({"immersiveportalspaperized.setname"})
   @RequiresPlayer
   @Argument(
      name = "price"
   )
   @Description("Sets the price of the closest portal")
   public boolean setPortalPrice(Player player, double price) throws CommandException {
      IPortal portal = this.getClosestPortal(player);
      if (portal.isNetherPortal()) {
         throw new CommandException(this.messageConfig.getErrorMessage(player, "nameNetherPortal"));
      } else {
         portal.setPrice(price);
         player.sendMessage(this.messageConfig.formatMiniMessage("<green>Set closest portal's price to " + price + "</green>"));
         return true;
      }
   }

   @Command
   @Path("immersiveportalspaperized/setpreset")
   @RequiresPermissions({"immersiveportalspaperized.setname"})
   @RequiresPlayer
   @Argument(
      name = "preset"
   )
   @Description("Sets the effect preset of the closest portal")
   public boolean setPortalPreset(Player player, String preset) throws CommandException {
      IPortal portal = this.getClosestPortal(player);
      if (portal.isNetherPortal()) {
         throw new CommandException(this.messageConfig.getErrorMessage(player, "nameNetherPortal"));
      } else {
         portal.setEffectPreset(preset);
         player.sendMessage(this.messageConfig.formatMiniMessage("<green>Set closest portal's effect preset to " + preset + "</green>"));
         return true;
      }
   }
}
