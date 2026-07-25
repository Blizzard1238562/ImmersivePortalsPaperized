package org.envel.immersiveportalspaperized.shared.net;

import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.envel.immersiveportalspaperized.shared.net.requests.Request;

public interface IRequestHandler {
   void handleRequest(@NotNull Request request, @NotNull Consumer<Response> responseConsumer);
}
