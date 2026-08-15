package org.envel.immersiveportalspaperized.bukkit.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.bukkit.plugin.java.JavaPlugin;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * Checks Modrinth for newer released versions of the plugin.
 * <p>
 * The check runs once per {@code checkAsync()} call, always off the main thread (see
 * {@link SchedulerUtil#runAsync(Runnable)}), and never throws - a broken or unreachable Modrinth
 * API must never affect plugin startup or gameplay. Results are cached on this singleton so
 * {@link UpdateNotifyListener} can notify newly-joining admins without re-querying the API.
 * <p>
 * Only {@code release}-channel versions for the {@code paper} loader are considered - beta/alpha
 * builds are intentionally ignored so admins are not nagged about pre-releases they didn't opt
 * into.
 */
@Singleton
public class UpdateChecker {
   private static final String MODRINTH_PROJECT_SLUG = "immersiveportalspaperized";
   // Query string is pre-encoded (loaders=["paper"]) - java.net.URI.create() does not tolerate
   // raw '[', ']', '"' characters in the query component.
   private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/"
      + MODRINTH_PROJECT_SLUG
      + "/version?loaders=%5B%22paper%22%5D&include_changelog=false";
   private static final String DOWNLOAD_URL = "https://modrinth.com/plugin/" + MODRINTH_PROJECT_SLUG;
   private static final String RELEASE_CHANNEL = "release";
   private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10L);

   private final MiscConfig miscConfig;
   private final Logger logger;
   private final String currentVersion;
   private final String userAgent;
   private final HttpClient httpClient;

   private volatile boolean updateAvailable = false;
   private volatile String latestVersion;

   @Inject
   public UpdateChecker(JavaPlugin pl, MiscConfig miscConfig, Logger logger) {
      this.miscConfig = miscConfig;
      this.logger = logger;
      this.currentVersion = pl.getDescription().getVersion();
      // Modrinth requires a uniquely-identifying User-Agent on every API request (generic/HTTP
      // library user agents risk being blocked) - see https://docs.modrinth.com/api/#user-agents
      this.userAgent = "Envel/ImmersivePortalsPaperized/" + this.currentVersion + " (https://github.com/OpenCommunity-Original/ImmersivePortalsPaperized)";
      this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
   }

   /**
    * Kicks off an asynchronous update check. Safe to call from the main thread - the actual HTTP
    * request always runs on a background thread/scheduler.
    */
   public void checkAsync() {
      if (!this.miscConfig.isUpdateCheckEnabled()) {
         return;
      }

      SchedulerUtil.runAsync(this::performCheck);
   }

   public boolean isUpdateAvailable() {
      return this.updateAvailable;
   }

   @NotNull
   public String getCurrentVersion() {
      return this.currentVersion;
   }

   /**
    * The newest known release version number, or {@code null} if no update has been found (yet).
    */
   public String getLatestVersion() {
      return this.latestVersion;
   }

   @NotNull
   public String getDownloadUrl() {
      return DOWNLOAD_URL;
   }

   private void performCheck() {
      try {
         HttpRequest request = HttpRequest.newBuilder(URI.create(MODRINTH_API_URL))
            .header("User-Agent", this.userAgent)
            .header("Accept", "application/json")
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
         HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
         if (response.statusCode() != 200) {
            this.logger.fine("Update check skipped: Modrinth returned HTTP %d", response.statusCode());
            return;
         }

         String best = this.findLatestReleaseVersion(response.body());
         if (best == null) {
            this.logger.fine("Update check: no release versions found on Modrinth for the paper loader.");
         } else if (isNewerVersion(best, this.currentVersion)) {
            this.latestVersion = best;
            this.updateAvailable = true;
            this.logger
               .warning(
                  "A new version of ImmersivePortalsPaperized is available: %s (you are running %s). Download: %s",
                  best,
                  this.currentVersion,
                  DOWNLOAD_URL
               );
         } else {
            this.logger.fine("ImmersivePortalsPaperized is up to date (running %s, latest release is %s).", this.currentVersion, best);
         }
      } catch (IOException e) {
         this.logger.fine("Update check failed: could not reach Modrinth: %s", e.getMessage());
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         this.logger.fine("Update check was interrupted.");
      } catch (RuntimeException e) {
         // Covers JSON parsing failures (malformed/unexpected response body) and anything else
         // unforeseen - a broken update check must never take the rest of the plugin down with it.
         this.logger.fine("Update check failed unexpectedly: %s", e.getMessage());
      }
   }

   private String findLatestReleaseVersion(String responseBody) {
      JsonArray versions = JsonParser.parseString(responseBody).getAsJsonArray();
      String best = null;

      for (JsonElement element : versions) {
         if (!element.isJsonObject()) {
            continue;
         }

         JsonObject versionObj = element.getAsJsonObject();
         if (versionObj.has("version_type") && !RELEASE_CHANNEL.equals(versionObj.get("version_type").getAsString())) {
            continue;
         }

         if (!versionObj.has("version_number")) {
            continue;
         }

         String versionNumber = versionObj.get("version_number").getAsString();
         if (best == null || isNewerVersion(versionNumber, best)) {
            best = versionNumber;
         }
      }

      return best;
   }

   private static boolean isNewerVersion(@NotNull String candidate, @NotNull String currentBest) {
      return compareVersions(candidate, currentBest) > 0;
   }

   /**
    * Lightweight semantic-version-style comparison: dot-separated numeric segments are compared
    * left to right, and a version with a {@code -suffix} (pre-release, e.g. {@code 1.2.0-beta.1})
    * is treated as older than the same numeric core without one. Not a full SemVer implementation,
    * but sufficient for this project's plain {@code MAJOR.MINOR.PATCH} tagging scheme.
    */
   private static int compareVersions(@NotNull String a, @NotNull String b) {
      String[] aMain = stripLeadingV(a).split("-", 2);
      String[] bMain = stripLeadingV(b).split("-", 2);
      int[] aParts = parseNumericParts(aMain[0]);
      int[] bParts = parseNumericParts(bMain[0]);
      int len = Math.max(aParts.length, bParts.length);

      for (int i = 0; i < len; i++) {
         int av = i < aParts.length ? aParts[i] : 0;
         int bv = i < bParts.length ? bParts[i] : 0;
         if (av != bv) {
            return Integer.compare(av, bv);
         }
      }

      boolean aHasSuffix = aMain.length > 1;
      boolean bHasSuffix = bMain.length > 1;
      if (aHasSuffix == bHasSuffix) {
         return 0;
      }

      return aHasSuffix ? -1 : 1;
   }

   private static int[] parseNumericParts(@NotNull String core) {
      String[] segments = core.split("\\.");
      int[] result = new int[segments.length];

      for (int i = 0; i < segments.length; i++) {
         result[i] = parseLeadingInt(segments[i]);
      }

      return result;
   }

   private static int parseLeadingInt(@NotNull String segment) {
      int end = 0;
      while (end < segment.length() && Character.isDigit(segment.charAt(end))) {
         end++;
      }

      if (end == 0) {
         return 0;
      }

      try {
         return Integer.parseInt(segment.substring(0, end));
      } catch (NumberFormatException e) {
         return 0;
      }
   }

   @NotNull
   private static String stripLeadingV(@NotNull String version) {
      return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
   }
}
