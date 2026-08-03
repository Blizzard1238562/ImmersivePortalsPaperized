package org.envel.immersiveportalspaperized.bukkit.config;

import io.foxserver.common.locale.LocaleAPI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.envel.immersiveportalspaperized.bukkit.nms.NBTTagUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * MessageConfig.
 */
@Singleton
public class MessageConfig {
   private static final String PORTAL_WAND_TAG = "portalWand";
   private final Logger logger;
   private final LocaleAPI localeApi;
   private final Map<String, String> messageMap = new HashMap<>();
   private String portalWandName;
   @Getter
   private String prefix;
   @Getter
   private String messageColor;
   private ItemStack portalWand = null;

   @Inject
   public MessageConfig(Logger logger, LocaleAPI localeApi) {
      this.logger = logger;
      this.localeApi = localeApi;
   }

   public void load(FileConfiguration file) {
      ConfigurationSection messagesSection = file.getConfigurationSection("chatMessages");
      if (messagesSection != null) {
         for (String key : messagesSection.getKeys(false)) {
            this.messageMap.put(key, this.translateColorCodes(messagesSection.getString(key)));
         }

         this.prefix = this.getRawMessage("prefix");
         this.messageColor = this.translateColorCodes(messagesSection.getString("messageColor"));
      }

      if (this.prefix == null) {
         this.prefix = this.translateColorCodes("&7[&aImmersivePortalsPaperized&7]&a ");
      }

      if (this.messageColor == null) {
         this.messageColor = this.translateColorCodes("&a");
      }

      this.portalWandName = this.translateColorCodes(Objects.requireNonNull(file.getString("portalWandName"), "Missing portalWandName"));
   }

   @NotNull
   private String translateColorCodes(@NotNull String message) {
      message = LegacyComponentSerializer.legacyAmpersand().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
      return this.translateHexColors(message);
   }

   @NotNull
   private String translateHexColors(@NotNull String message) {
      StringBuilder result = new StringBuilder();
      StringBuilder currentSegment = null;

      for (char c : message.toCharArray()) {
         if (c == '{' && currentSegment == null) {
            currentSegment = new StringBuilder();
         }

         if (currentSegment == null) {
            result.append(c);
         } else {
            currentSegment.append(c);
         }

         if (c == '}' && currentSegment != null) {
            String segment = currentSegment.toString();
            boolean parsingFailed = true;
            if (segment.charAt(1) == '(' && segment.charAt(segment.length() - 2) == ')') {
               String hexString = segment.substring(2, segment.length() - 2);
               TextColor color = TextColor.fromHexString("#" + hexString);
               if (color != null) {
                  result.append(this.legacyColorCode(color));
                  parsingFailed = false;
               } else {
                  this.logger.warning("Failed to parse hex colour: %s", hexString);
               }
            }

            if (parsingFailed) {
               result.append(segment);
            }

            currentSegment = null;
         }
      }

      return result.toString();
   }

   @NotNull
   private String legacyColorCode(@NotNull TextColor color) {
      String serialized = LegacyComponentSerializer.legacySection().serialize(Component.text('\u0000').color(color));
      return serialized.substring(0, serialized.length() - 1);
   }

   @NotNull
   public ItemStack getPortalWand() {
      if (this.portalWand == null) {
         this.portalWand = new ItemStack(Material.BLAZE_ROD);
         ItemMeta meta = this.portalWand.getItemMeta();
         if (meta == null) {
            this.logger.warning("Failed to get ItemMeta for portal wand - this should never happen!");
         } else {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(this.portalWandName));
            this.portalWand.setItemMeta(meta);
         }

         this.portalWand = NBTTagUtil.addMarkerTag(this.portalWand, "portalWand");
      }

      return this.portalWand;
   }

   public boolean isPortalWand(ItemStack item) {
      return NBTTagUtil.hasMarkerTag(item, "portalWand");
   }

   public String formatMiniMessage(String message) {
      if (message != null && !message.isEmpty()) {
         try {
            return LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(message));
         } catch (Exception var3) {
            return message;
         }
      } else {
         return "";
      }
   }

   public String getChatMessage(CommandSender sender, String name) {
      Player player = sender instanceof Player ? (Player)sender : null;
      String raw = this.localeApi.getRaw(player, name);
      if (raw == null) {
         raw = this.getRawMessage(name);
      }

      return raw == null ? "" : this.formatMiniMessage(this.getPrefix(player) + raw);
   }

   public String getChatMessage(String name) {
      return this.getChatMessage(null, name);
   }

   public String getErrorMessage(CommandSender sender, String name) {
      Player player = sender instanceof Player ? (Player)sender : null;
      String raw = this.localeApi.getRaw(player, name);
      if (raw == null) {
         raw = this.getRawMessage(name);
      }

      return raw == null ? "" : this.formatMiniMessage(raw);
   }

   public String getErrorMessage(String name) {
      return this.getErrorMessage(null, name);
   }

   public String getWarningMessage(CommandSender sender, String name) {
      Player player = sender instanceof Player ? (Player)sender : null;
      String raw = this.localeApi.getRaw(player, name);
      if (raw == null) {
         raw = this.getRawMessage(name);
      }

      return raw != null && !raw.isEmpty() ? this.formatMiniMessage("<yellow>" + raw + "</yellow>") : "";
   }

   public String getWarningMessage(String name) {
      return this.getWarningMessage(null, name);
   }

   public String getPrefix(Player player) {
      String rawPrefix = this.localeApi.getRaw(player, "prefix");
      return rawPrefix != null && !rawPrefix.equals("<gray>[<green>ImmersivePortalsPaperized</green>]</gray> ")
         ? rawPrefix
         : "<bold><gradient:#00FFA0:#00BFFF>ImmersivePortalsPaperized</gradient></bold> <gray>Â»</gray> ";
   }

   public String getRawMessage(String name) {
      return this.messageMap.get(name);
   }
}


