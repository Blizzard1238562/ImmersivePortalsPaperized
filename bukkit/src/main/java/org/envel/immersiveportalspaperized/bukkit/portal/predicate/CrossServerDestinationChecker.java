package org.envel.immersiveportalspaperized.bukkit.portal.predicate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.api.ImmersivePortal;
import org.envel.immersiveportalspaperized.api.PortalPredicate;
import org.envel.immersiveportalspaperized.bukkit.net.IPortalClient;
import org.envel.immersiveportalspaperized.bukkit.net.requests.CheckDestinationValidityRequest;
import org.envel.immersiveportalspaperized.bukkit.util.VersionUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.RequestException;

@Singleton
public class CrossServerDestinationChecker implements PortalPredicate {
   private static final int VALIDITY_CHECK_INTERVAL = 1;
   private static final long PRUNE_CHECK_INTERVAL_SECONDS = 10L;
   private static final long CACHE_ENTRY_MAX_AGE_SECONDS = 30L;
   private final Logger logger;
   private final IPortalClient portalClient;
   private final Map<ImmersivePortal, CrossServerDestinationChecker.CacheEntry> cache = new ConcurrentHashMap<>();
   private final Set<ImmersivePortal> ongoingRequest = ConcurrentHashMap.newKeySet();
   private boolean wasConnectedLastTick = true;
   private Instant lastPruneTime = Instant.now();

   @Inject
   public CrossServerDestinationChecker(Logger logger, IPortalClient portalClient) {
      this.logger = logger;
      this.portalClient = portalClient;
   }

   @Override
   public boolean test(@NotNull ImmersivePortal portal, @NotNull Player player) {
      if (!portal.isCrossServer()) {
         return true;
      } else {
         if (Duration.between(this.lastPruneTime, Instant.now()).getSeconds() > PRUNE_CHECK_INTERVAL_SECONDS) {
            this.pruneCache();
         }

         if (!this.portalClient.canReceiveRequests()) {
            if (this.wasConnectedLastTick) {
               this.wasConnectedLastTick = false;
               this.logger.warning("Cross server portals deactivating - disconnected from the proxy");
            }

            return false;
         } else {
            if (!this.wasConnectedLastTick) {
               this.logger.info("Cross-server portals reactivating! - proxy is connected");
               this.wasConnectedLastTick = true;
            }

            Boolean cachedValidityValue = this.checkCache(portal);
            if (cachedValidityValue != null) {
               return cachedValidityValue;
            } else {
               this.runValidityCheck(portal);
               CrossServerDestinationChecker.CacheEntry entry = this.cache.get(portal);
               return entry != null && entry.validity;
            }
         }
      }
   }

   @Nullable
   private Boolean checkCache(@NotNull ImmersivePortal portal) {
      CrossServerDestinationChecker.CacheEntry entry = this.cache.get(portal);
      if (entry == null) {
         return null;
      } else {
         double secondsElapsed = Duration.between(entry.lastChecked, Instant.now()).getSeconds();
         return secondsElapsed >= VALIDITY_CHECK_INTERVAL ? null : entry.validity;
      }
   }

   private void runValidityCheck(@NotNull ImmersivePortal portal) {
      if (!this.ongoingRequest.contains(portal)) {
         this.ongoingRequest.add(portal);
         this.logger.finest("Checking validity of portal %s", portal.getId());
         CheckDestinationValidityRequest request = new CheckDestinationValidityRequest();
         request.setOriginGameVersion(VersionUtil.getCurrentVersion());
         request.setDestinationWorldId(portal.getDestPos().getWorldId());
         request.setDestinationWorldName(portal.getDestPos().getWorldName());
         this.portalClient.sendRequestToServer(request, portal.getDestPos().getServerName(), response -> {
            try {
               response.checkForErrors();
               this.putValidityValue(portal, true);
               this.logger.finest("Destination validity OK!");
            } catch (RequestException var5) {
               CrossServerDestinationChecker.CacheEntry entry = this.cache.get(portal);
               if (entry == null || entry.validity) {
                  this.logger.warning("Not activating cross server portal - destination is invalid: %s", var5.getMessage());
               }

               this.putValidityValue(portal, false);
            }

            this.ongoingRequest.remove(portal);
         });
      }
   }

   private void putValidityValue(@NotNull ImmersivePortal portal, boolean newValue) {
      this.cache.put(portal, new CrossServerDestinationChecker.CacheEntry(newValue, Instant.now()));
   }

   public void clear() {
      this.cache.clear();
      this.ongoingRequest.clear();
      this.wasConnectedLastTick = true;
   }

   private void pruneCache() {
      Instant now = Instant.now();
      if (Duration.between(this.lastPruneTime, now).getSeconds() > PRUNE_CHECK_INTERVAL_SECONDS) {
         this.lastPruneTime = now;
         Instant threshold = now.minus(Duration.ofSeconds(CACHE_ENTRY_MAX_AGE_SECONDS));
         this.cache.entrySet().removeIf(entry -> {
            if (entry.getValue().lastChecked.isBefore(threshold)) {
               this.ongoingRequest.remove(entry.getKey());
               return true;
            } else {
               return false;
            }
         });
      }
   }

   private static class CacheEntry {
      final boolean validity;
      final Instant lastChecked;

      CacheEntry(boolean validity, Instant lastChecked) {
         this.validity = validity;
         this.lastChecked = lastChecked;
      }
   }
}
