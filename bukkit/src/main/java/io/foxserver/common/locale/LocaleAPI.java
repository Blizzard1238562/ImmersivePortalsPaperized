package io.foxserver.common.locale;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class LocaleAPI implements Listener {
   public static final String LANG_FOLDER = "lang";
   private static final MiniMessage MM = MiniMessage.miniMessage();
   private final Plugin plugin;
   private final String defaultLocaleTag;
   private final boolean autoTranslate;
   private final Set<String> supportedLocales = new LinkedHashSet<>();
   private final Map<String, String> prefixIndex = new HashMap<>();
   private final Map<UUID, String> playerLocales = new ConcurrentHashMap<>();
   private final Cache<String, YamlConfiguration> configCache = Caffeine.newBuilder().expireAfterAccess(5L, TimeUnit.MINUTES).build();
   private static final List<String> BUNDLED_LOCALES = Arrays.asList("en_US", "ru_RU");

   public LocaleAPI(Plugin plugin, String defaultLocale, boolean autoTranslate) {
      this.plugin = plugin;
      this.defaultLocaleTag = defaultLocale;
      this.autoTranslate = autoTranslate;
   }

   public void load() {
      File langDir = this.langDir();
      langDir.mkdirs();
      this.extractBundledFiles(langDir);
      this.discoverLocales(langDir);
      this.buildPrefixIndex();
      if (this.supportedLocales.isEmpty()) {
         throw new IllegalStateException("[" + this.plugin.getName() + "] No lang files found in " + langDir.getPath());
      } else {
         this.plugin.getLogger().info("Loaded " + this.supportedLocales.size() + " locale(s): " + String.join(", ", this.supportedLocales));
      }
   }

   public void reload() {
      this.configCache.invalidateAll();
      this.supportedLocales.clear();
      this.prefixIndex.clear();
      this.load();
   }

   public Component getComponent(Player player, String key, TagResolver... resolvers) {
      String raw = this.getRaw(player, key);
      if (raw == null) {
         return Component.empty();
      } else {
         return resolvers.length == 0 ? MM.deserialize(raw) : MM.deserialize(raw, TagResolver.resolver(resolvers));
      }
   }

   public Component getComponent(String key, TagResolver... resolvers) {
      return this.getComponent(null, key, resolvers);
   }

   public String getRaw(Player player, String key) {
      return this.fallbackChain(this.resolveLocaleTag(player), key);
   }

   public List<String> getStringList(Player player, String key) {
      String tag = this.resolveLocaleTag(player);
      YamlConfiguration cfg = this.configCache.get(tag, this::loadConfig);
      if ((cfg == null || !cfg.contains(key)) && !tag.equals(this.defaultLocaleTag)) {
         cfg = this.configCache.get(this.defaultLocaleTag, this::loadConfig);
      }

      return cfg != null ? cfg.getStringList(key) : Collections.emptyList();
   }

   public String getMessage(Player player, String key, Object... replacements) {
      String msg = this.getRaw(player, key);
      if (msg != null && replacements.length != 0) {
         for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
         }

         return msg;
      } else {
         return msg;
      }
   }

   public static TagResolver placeholder(String name, Object value) {
      return Placeholder.unparsed(name, String.valueOf(value));
   }

   public String getLocaleTag(Player player) {
      return this.resolveLocaleTag(player);
   }

   public Set<String> getSupportedLocales() {
      return Collections.unmodifiableSet(this.supportedLocales);
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onLocaleChange(PlayerLocaleChangeEvent event) {
      if (this.autoTranslate) {
         String tag = this.mapClientLocale(event.locale().toLanguageTag());
         this.playerLocales.put(event.getPlayer().getUniqueId(), tag);
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onQuit(PlayerQuitEvent event) {
      this.playerLocales.remove(event.getPlayer().getUniqueId());
   }

   private String resolveLocaleTag(Player player) {
      return player != null && this.autoTranslate
         ? this.playerLocales.computeIfAbsent(player.getUniqueId(), uuid -> this.mapClientLocale(player.locale().toLanguageTag()))
         : this.defaultLocaleTag;
   }

   private String mapClientLocale(String clientTag) {
      String normalised = normalise(clientTag);
      if (this.supportedLocales.contains(normalised)) {
         return normalised;
      } else {
         String lang = normalised.contains("_") ? normalised.substring(0, normalised.indexOf(95)) : normalised;
         String indexed = this.prefixIndex.get(lang);
         return indexed != null ? indexed : this.defaultLocaleTag;
      }
   }

   private static String normalise(String raw) {
      if (raw != null && !raw.isEmpty()) {
         String s = raw.replace('-', '_');
         int idx = s.indexOf(95);
         return idx < 0 ? s.toLowerCase(Locale.ROOT) : s.substring(0, idx).toLowerCase(Locale.ROOT) + "_" + s.substring(idx + 1).toUpperCase(Locale.ROOT);
      } else {
         return "en_US";
      }
   }

   private void buildPrefixIndex() {
      for (String tag : this.supportedLocales) {
         String lang = tag.contains("_") ? tag.substring(0, tag.indexOf(95)) : tag;
         this.prefixIndex.putIfAbsent(lang, tag);
      }
   }

   private String fallbackChain(String primaryTag, String key) {
      String msg = this.fromConfig(primaryTag, key);
      if (msg != null) {
         return msg;
      } else {
         if (!primaryTag.equals(this.defaultLocaleTag)) {
            msg = this.fromConfig(this.defaultLocaleTag, key);
            if (msg != null) {
               return msg;
            }
         }

         for (String locale : this.supportedLocales) {
            msg = this.fromConfig(locale, key);
            if (msg != null) {
               return msg;
            }
         }

         return null;
      }
   }

   private String fromConfig(String tag, String key) {
      if (!this.supportedLocales.contains(tag)) {
         return null;
      } else {
         YamlConfiguration cfg = this.configCache.get(tag, this::loadConfig);
         return cfg != null ? cfg.getString(key) : null;
      }
   }

   private YamlConfiguration loadConfig(String tag) {
      File file = new File(this.langDir(), tag + ".yml");
      if (!file.exists()) {
         return null;
      } else {
         try {
            return YamlConfiguration.loadConfiguration(file);
         } catch (Exception var4) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to load lang/" + tag + ".yml", (Throwable)var4);
            return null;
         }
      }
   }

   private void extractBundledFiles(File langDir) {
      for (String locale : BUNDLED_LOCALES) {
         File target = new File(langDir, locale + ".yml");
         if (!target.exists()) {
            try (InputStream in = this.plugin.getResource("lang/" + locale + ".yml")) {
               if (in != null) {
                  target.getParentFile().mkdirs();
                  Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
               }
            } catch (Exception var20) {
               this.plugin.getLogger().log(Level.WARNING, "Failed to extract bundled resource lang file: " + locale, (Throwable)var20);
            }
         }
      }

      try {
         File jarFile = new File(this.plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
         if (jarFile.isFile()) {
            try (ZipFile zip = new ZipFile(jarFile)) {
               String prefix = "lang/";
               Enumeration<? extends ZipEntry> entries = zip.entries();

               while (entries.hasMoreElements()) {
                  ZipEntry entry = entries.nextElement();
                  String name = entry.getName();
                  if (name.startsWith(prefix) && !entry.isDirectory()) {
                     String fileName = name.substring(prefix.length());
                     if (!fileName.isEmpty()) {
                        File target = new File(langDir, fileName);
                        if (!target.exists()) {
                           target.getParentFile().mkdirs();

                           try (InputStream inx = zip.getInputStream(entry)) {
                              Files.copy(inx, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                           }
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var18) {
         this.plugin
            .getLogger()
            .log(
               Level.WARNING,
               "Could not auto-extract lang files from jar (unusual ClassLoader?): " + var18.getMessage() + " — place lang/*.yml files manually if needed."
            );
      }
   }

   private void discoverLocales(File langDir) {
      File[] files = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
      if (files != null) {
         Arrays.sort(files, Comparator.comparing(File::getName));

         for (File f : files) {
            this.supportedLocales.add(f.getName().replace(".yml", ""));
         }
      }
   }

   private File langDir() {
      return new File(this.plugin.getDataFolder(), "lang");
   }
}
