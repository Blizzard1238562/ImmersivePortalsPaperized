package org.envel.immersiveportalspaperized.bukkit.block.external;

import java.util.function.Consumer;
import org.envel.immersiveportalspaperized.bukkit.net.requests.GetBlockDataChangesRequest;
import org.envel.immersiveportalspaperized.shared.net.Response;

public interface IExternalBlockWatcherManager {
   double CLEAR_TIME = 10.0;

   void onRequestReceived(GetBlockDataChangesRequest request, Consumer<Response> onFinish);

   void update();

   void clear();
}
