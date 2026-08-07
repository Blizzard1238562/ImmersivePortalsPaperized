package org.envel.immersiveportalspaperized.bukkit.block.fetch;

import org.envel.immersiveportalspaperized.bukkit.config.RenderConfig;
import org.envel.immersiveportalspaperized.bukkit.net.IPortalClient;
import org.envel.immersiveportalspaperized.bukkit.portal.IPortal;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * BlockDataFetcherFactory.
 */
@Singleton
public class BlockDataFetcherFactory {
   private final Logger logger;
   private final IPortalClient portalClient;
   private final RenderConfig renderConfig;

   @Inject
   public BlockDataFetcherFactory(Logger logger, IPortalClient portalClient, RenderConfig renderConfig) {
      this.logger = logger;
      this.portalClient = portalClient;
      this.renderConfig = renderConfig;
   }

   public IBlockDataFetcher create(IPortal portal) {
      return (IBlockDataFetcher)(portal.isCrossServer()
         ? new ExternalBlockDataFetcher(this.logger, this.portalClient, this.renderConfig, portal)
         : new LocalBlockDataFetcher(portal, this.renderConfig));
   }
}


