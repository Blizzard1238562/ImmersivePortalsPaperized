package org.envel.immersiveportalspaperized.bukkit.block.fetch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.block.data.BlockData;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.net.IPortalClient;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetBlockDataChangesRequest;
import org.envel.immersiveportalspaperized.bukkit.nms.BlockDataUtil;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.RequestException;

/**
 * ExternalBlockDataFetcher.
 */
public class ExternalBlockDataFetcher implements IBlockDataFetcher {
   private final Logger logger;
   private final IPortalClient portalClient;
   private final GetBlockDataChangesRequest request;
   private final String destServerName;
   private final Map<IntVector, BlockData> currentStates = new HashMap<>();
   private volatile boolean hasFirstRequestFinished = false;
   private volatile boolean hasPreviousRequestFinished = true;

   public ExternalBlockDataFetcher(Logger logger, IPortalClient portalClient, RenderConfig renderConfig, IPortal portal) {
      this.logger = logger;
      this.portalClient = portalClient;
      this.destServerName = portal.getDestPos().getServerName();
      this.request = new GetBlockDataChangesRequest();
      this.request.setYRadius((int)renderConfig.getMaxY());
      this.request.setXAndZRadius((int)renderConfig.getMaxXZ());
      this.request.setChangeSetId(UUID.randomUUID());
      this.request.setWorldName(portal.getDestPos().getWorldName());
      this.request.setWorldId(portal.getDestPos().getWorldId());
      this.request.setPosition(new IntVector(portal.getDestPos().getVector()));
      this.request.setRotateOriginToDest(portal.getTransformations().getRotateToDestination());
   }

   @Override
   public void update() {
      if (!this.hasPreviousRequestFinished) {
         this.logger.fine("Still awaiting block data response");
      } else {
         this.hasPreviousRequestFinished = false;
         this.portalClient.sendRequestToServer(this.request, this.destServerName, response -> {
            this.hasPreviousRequestFinished = true;

            try {
               this.logger.finer("Received response to get block data request");
               Map<IntVector, Integer> serializedChanges = (Map<IntVector, Integer>)response.getResult();
               serializedChanges.forEach((position, newValue) -> this.currentStates.put(position, BlockDataUtil.getByCombinedId(newValue)));
               this.hasFirstRequestFinished = true;
            } catch (RequestException e) {
               this.logger.warning("Failed to fetch block changes for external portal: %s", e.getMessage());
            }
         });
      }
   }

   @Override
   public boolean isReady() {
      return this.hasFirstRequestFinished;
   }

   @NotNull
   @Override
   public BlockData getData(@NotNull IntVector position) {
      return this.currentStates.get(position);
   }
}


