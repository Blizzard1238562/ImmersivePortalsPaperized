package org.envel.immersiveportalspaperized.bukkit.block.external;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetBlockDataChangesRequest;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.net.Response;

@Singleton
public class ExternalBlockWatcherManager implements IExternalBlockWatcherManager {
   private static final int BLOCK_WATCHER_CLEAR_DELAY = 5;
   private final Logger logger;
   private final IBlockChangeWatcher.Factory blockChangeWatcherFactory;
   private final Map<UUID, IBlockChangeWatcher> watchers = new HashMap<>();
   private final Map<UUID, Long> lastRequestedMillis = new HashMap<>();

   @Inject
   public ExternalBlockWatcherManager(Logger logger, IBlockChangeWatcher.Factory blockChangeWatcherFactory) {
      this.logger = logger;
      this.blockChangeWatcherFactory = blockChangeWatcherFactory;
   }

   @Override
   public void onRequestReceived(GetBlockDataChangesRequest request, Consumer<Response> onFinish) {
      this.logger.finer("Processing block changes with ID %s", request.getChangeSetId());
      UUID watcherId = request.getChangeSetId();
      IBlockChangeWatcher watcher = this.watchers.computeIfAbsent(watcherId, key -> this.blockChangeWatcherFactory.create(request));
      this.lastRequestedMillis.put(watcherId, System.currentTimeMillis());
      Response response = new Response();
      Map<IntVector, Integer> changes = watcher.checkForChanges();
      this.logger.finer("Change count: %d", changes.size());
      response.setResult(changes);
      onFinish.accept(response);
   }

   @Override
   public void update() {
      if (!this.lastRequestedMillis.isEmpty()) {
         long now = System.currentTimeMillis();
         long clearThresholdMs = BLOCK_WATCHER_CLEAR_DELAY * 1000L;
         Iterator<Entry<UUID, Long>> iterator = this.lastRequestedMillis.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<UUID, Long> entry = iterator.next();
            if (now - entry.getValue() > clearThresholdMs) {
               this.logger.fine("Clearing external block watcher due to inactivity");
               iterator.remove();
               this.watchers.remove(entry.getKey());
            }
         }
      }
   }

   @Override
   public void clear() {
      this.watchers.clear();
      this.lastRequestedMillis.clear();
   }
}
