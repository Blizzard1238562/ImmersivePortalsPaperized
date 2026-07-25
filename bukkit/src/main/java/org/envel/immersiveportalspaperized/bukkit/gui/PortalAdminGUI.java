package org.envel.immersiveportalspaperized.bukkit.gui;

import io.foxserver.common.locale.LocaleAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.envel.immersiveportalspaperized.bukkit.events.IEventRegistrar;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortalManager;
import org.envel.immersiveportalspaperized.bukkit.portal.effects.PortalEffectPreset;
import org.envel.immersiveportalspaperized.bukkit.portal.effects.PortalEffectsTask;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PortalAdminGUI implements Listener {
   private final IPortalManager portalManager;
   private final PortalEffectsTask effectsTask;
   private final LocaleAPI localeApi;
   private final Map<UUID, PortalAdminGUI.GUISession> activeSessions = new HashMap<>();

   @Inject
   public PortalAdminGUI(IEventRegistrar eventRegistrar, IPortalManager portalManager, PortalEffectsTask effectsTask, LocaleAPI localeApi) {
      this.portalManager = portalManager;
      this.effectsTask = effectsTask;
      this.localeApi = localeApi;
      eventRegistrar.register(this);
   }

   public void open(Player player) {
      this.openListPage(player, 0);
   }

   private Component getTitle(Player player, String key, Object... replacements) {
      String msg = this.localeApi.getMessage(player, key, replacements);
      return (Component)(msg == null ? Component.text(key) : MiniMessage.miniMessage().deserialize(msg));
   }

   private void openListPage(Player player, int page) {
      List<IPortal> portals = new ArrayList<>();

      for (IPortal p : this.portalManager.getAllPortals()) {
         if (p.isCustom()) {
            portals.add(p);
         }
      }

      Component title = this.getTitle(player, "gui_title_list", "{page}", String.valueOf(page + 1));
      Inventory inv = Bukkit.createInventory(null, 54, title);
      this.fillBackground(inv, 45, 54);
      int startIdx = page * 45;
      int endIdx = Math.min(startIdx + 45, portals.size());
      int slot = 0;

      for (int i = startIdx; i < endIdx; i++) {
         inv.setItem(slot++, this.createPortalListItem(player, portals.get(i)));
      }

      if (page > 0) {
         inv.setItem(45, this.createLocalizedItem(player, Material.ARROW, "gui_button_prev", null));
      }

      if (endIdx < portals.size()) {
         inv.setItem(53, this.createLocalizedItem(player, Material.ARROW, "gui_button_next", null));
      }

      PortalAdminGUI.GUISession session = new PortalAdminGUI.GUISession();
      session.viewState = 0;
      session.page = page;
      session.inventory = inv;
      session.portalsCache = portals;
      this.activeSessions.put(player.getUniqueId(), session);
      player.openInventory(inv);
   }

   private void openEditor(Player player, IPortal portal) {
      Component title = this.getTitle(player, "gui_title_editor");
      Inventory inv = Bukkit.createInventory(null, 27, title);
      this.fillBackground(inv, 0, 27);
      inv.setItem(10, this.createLocalizedItem(player, Material.COMPASS, "gui_button_teleport", "gui_lore_teleport"));
      String allowItems = this.localeApi.getMessage(player, portal.allowsNonPlayerTeleportation() ? "gui_status_enabled" : "gui_status_disabled");
      inv.setItem(11, this.createLocalizedItem(player, Material.HOPPER, "gui_button_item_mob", "gui_lore_click_toggle", "{status}", allowItems));
      inv.setItem(12, this.createLocalizedItem(player, Material.RED_WOOL, "gui_button_dec_price", "gui_lore_dec_price"));
      inv.setItem(
         13, this.createLocalizedItem(player, Material.GOLD_BLOCK, "gui_button_price", "gui_lore_price", "{price}", String.format("$%.2f", portal.getPrice()))
      );
      inv.setItem(14, this.createLocalizedItem(player, Material.LIME_WOOL, "gui_button_inc_price", "gui_lore_inc_price"));
      String presetName = portal.getEffectPreset() != null ? portal.getEffectPreset() : "default";
      inv.setItem(15, this.createLocalizedItem(player, Material.BLAZE_POWDER, "gui_button_effects", "gui_lore_effects", "{preset}", presetName));
      String soundStatus = this.localeApi.getMessage(player, portal.isSoundEnabled() ? "gui_status_enabled" : "gui_status_disabled");
      inv.setItem(16, this.createLocalizedItem(player, Material.NOTE_BLOCK, "gui_button_sound", "gui_lore_sound", "{status}", soundStatus));
      inv.setItem(17, this.createLocalizedItem(player, Material.BARRIER, "gui_button_delete", "gui_lore_delete"));
      inv.setItem(22, this.createLocalizedItem(player, Material.ARROW, "gui_button_back", null));
      PortalAdminGUI.GUISession session = this.activeSessions.get(player.getUniqueId());
      if (session == null) {
         session = new PortalAdminGUI.GUISession();
      }

      session.viewState = 1;
      session.targetPortal = portal;
      session.inventory = inv;
      this.activeSessions.put(player.getUniqueId(), session);
      player.openInventory(inv);
   }

   private void openEffectsMenu(Player player, IPortal portal) {
      Component title = this.getTitle(player, "gui_title_effects");
      Inventory inv = Bukkit.createInventory(null, 27, title);
      this.fillBackground(inv, 0, 27);
      int slot = 10;

      for (String presetName : this.effectsTask.getPresetNames()) {
         if (slot > 16) {
            break;
         }

         PortalEffectPreset preset = this.effectsTask.getPreset(presetName);
         if (preset != null) {
            inv.setItem(slot++, this.createPresetItem(player, portal, preset));
         }
      }

      inv.setItem(22, this.createLocalizedItem(player, Material.ARROW, "gui_button_back_to_editor", null));
      PortalAdminGUI.GUISession session = this.activeSessions.get(player.getUniqueId());
      if (session == null) {
         session = new PortalAdminGUI.GUISession();
      }

      session.viewState = 2;
      session.targetPortal = portal;
      session.inventory = inv;
      this.activeSessions.put(player.getUniqueId(), session);
      player.openInventory(inv);
   }

   private void fillBackground(Inventory inv, int start, int end) {
      ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
      ItemMeta meta = pane.getItemMeta();
      if (meta != null) {
         meta.displayName(Component.empty());
         pane.setItemMeta(meta);
      }

      for (int i = start; i < end; i++) {
         if (inv.getItem(i) == null) {
            inv.setItem(i, pane);
         }
      }
   }

   private ItemStack createPortalListItem(Player player, IPortal portal) {
      ItemStack item = new ItemStack(Material.END_PORTAL_FRAME);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         String name = portal.getName() != null ? portal.getName() : this.localeApi.getMessage(player, "gui_list_default_portal_name");
         if (name == null) {
            name = "Custom Portal";
         }

         meta.displayName(MiniMessage.miniMessage().deserialize("<gold>" + name + "</gold>"));
         String idStr = portal.getId().toString().substring(0, 8);
         String originStr = this.formatLocation(portal.getOriginPos().getLocation());
         String priceStr = String.format("$%.2f", portal.getPrice());
         String presetStr = portal.getEffectPreset() != null ? portal.getEffectPreset() : "default";
         String loreStr = this.localeApi
            .getMessage(player, "gui_list_item_lore", "{id}", idStr, "{origin}", originStr, "{price}", priceStr, "{preset}", presetStr);
         if (loreStr != null) {
            List<Component> lore = new ArrayList<>();

            for (String line : loreStr.split("\n")) {
               lore.add(MiniMessage.miniMessage().deserialize(line));
            }

            meta.lore(lore);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createPresetItem(Player player, IPortal portal, PortalEffectPreset preset) {
      String activePresetName = portal.getEffectPreset();
      if (activePresetName == null) {
         activePresetName = "default";
      }

      boolean isActive = activePresetName.equalsIgnoreCase(preset.getName());
      ItemStack item = new ItemStack(isActive ? Material.BLAZE_POWDER : Material.GUNPOWDER);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         String nameFormat = this.localeApi.getMessage(player, isActive ? "gui_effects_active" : "gui_effects_inactive", "{name}", preset.getName());
         if (nameFormat != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(nameFormat));
         }

         String particleStr = preset.getParticle().name();
         String countStr = String.valueOf(preset.getParticleCount());
         String speedStr = String.format("%.2f", preset.getParticleSpeed());
         String soundStr = preset.getSound() != null ? preset.getSound().getKey().toString() : "None";
         String volumeStr = String.format("%.2f", preset.getSoundVolume());
         String pitchStr = String.format("%.2f", preset.getSoundPitch());
         String intervalStr = String.valueOf(preset.getSoundIntervalTicks());
         String statusText = this.localeApi.getMessage(player, isActive ? "gui_effects_selected" : "gui_effects_click_select");
         String loreStr = this.localeApi
            .getMessage(
               player,
               "gui_effects_lore",
               "{particle}",
               particleStr,
               "{count}",
               countStr,
               "{speed}",
               speedStr,
               "{sound}",
               soundStr,
               "{volume}",
               volumeStr,
               "{pitch}",
               pitchStr,
               "{interval}",
               intervalStr,
               "{status_text}",
               statusText
            );
         if (loreStr != null) {
            List<Component> lore = new ArrayList<>();

            for (String line : loreStr.split("\n")) {
               lore.add(MiniMessage.miniMessage().deserialize(line));
            }

            meta.lore(lore);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createLocalizedItem(Player player, Material material, String nameKey, String loreKey, Object... replacements) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         String nameStr = this.localeApi.getMessage(player, nameKey, replacements);
         if (nameStr != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(nameStr));
         }

         if (loreKey != null) {
            String loreStr = this.localeApi.getMessage(player, loreKey, replacements);
            if (loreStr != null) {
               List<Component> lore = new ArrayList<>();

               for (String line : loreStr.split("\n")) {
                  lore.add(MiniMessage.miniMessage().deserialize(line));
               }

               meta.lore(lore);
            }
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   private String formatLocation(Location loc) {
      return loc == null ? "External Server" : String.format("%s, %d, %d, %d", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      Player player = (Player)event.getWhoClicked();
      PortalAdminGUI.GUISession session = this.activeSessions.get(player.getUniqueId());
      if (session != null) {
         event.setCancelled(true);
         int slot = event.getRawSlot();
         if (slot >= 0) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
               if (session.viewState == 1) {
                  this.handleEditorClick(player, session, slot, event.isRightClick());
               } else if (session.viewState == 2) {
                  this.handleEffectsClick(player, session, slot);
               } else {
                  this.handleListClick(player, session, slot);
               }
            }
         }
      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      Player player = (Player)event.getPlayer();
      PortalAdminGUI.GUISession session = this.activeSessions.get(player.getUniqueId());
      if (session != null && event.getInventory().equals(session.inventory)) {
         this.activeSessions.remove(player.getUniqueId());
      }
   }

   private void handleListClick(Player player, PortalAdminGUI.GUISession session, int slot) {
      if (slot == 45 && session.page > 0) {
         this.openListPage(player, session.page - 1);
      } else if (slot == 53) {
         this.openListPage(player, session.page + 1);
      } else {
         if (slot < 45) {
            int idx = session.page * 45 + slot;
            if (idx >= session.portalsCache.size()) {
               return;
            }

            this.openEditor(player, session.portalsCache.get(idx));
         }
      }
   }

   private void handleEditorClick(Player player, PortalAdminGUI.GUISession session, int slot, boolean isRight) {
      IPortal portal = session.targetPortal;
      if (portal != null) {
         switch (slot) {
            case 10 -> {
               player.closeInventory();
               player.teleportAsync(portal.getOriginPos().getLocation());
            }
            case 11 -> {
               portal.setAllowsNonPlayerTeleportation(!portal.allowsNonPlayerTeleportation());
               this.openEditor(player, portal);
            }
            case 12 -> {
               double newPriceDec = Math.max(0.0, portal.getPrice() - (isRight ? 10.0 : 1.0));
               portal.setPrice(newPriceDec);
               this.openEditor(player, portal);
            }
            case 13 -> {
               portal.setPrice(0.0);
               this.openEditor(player, portal);
            }
            case 14 -> {
               double newPriceInc = portal.getPrice() + (isRight ? 10.0 : 1.0);
               portal.setPrice(newPriceInc);
               this.openEditor(player, portal);
            }
            case 15 -> this.openEffectsMenu(player, portal);
            case 16 -> {
               portal.setSoundEnabled(!portal.isSoundEnabled());
               this.openEditor(player, portal);
            }
            case 17 -> {
               this.portalManager.removePortal(portal);
               this.openListPage(player, session.page);
            }
            case 22 -> this.openListPage(player, session.page);
            default -> {
            }
         }
      }
   }

   private void handleEffectsClick(Player player, PortalAdminGUI.GUISession session, int slot) {
      IPortal portal = session.targetPortal;
      if (portal != null) {
         if (slot == 22) {
            this.openEditor(player, portal);
         } else {
            if (slot >= 10 && slot < 17) {
               List<String> presetNames = new ArrayList<>(this.effectsTask.getPresetNames());
               int idx = slot - 10;
               if (idx >= 0 && idx < presetNames.size()) {
                  String selectedPreset = presetNames.get(idx);
                  portal.setEffectPreset(selectedPreset);
                  this.openEffectsMenu(player, portal);
               }
            }
         }
      }
   }

   private static class GUISession {
      int viewState;
      int page;
      IPortal targetPortal;
      Inventory inventory;
      List<IPortal> portalsCache;
   }
}
