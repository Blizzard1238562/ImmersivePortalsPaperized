package org.envel.immersiveportalspaperized.proxy;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Configuration values required by the proxy-side portal server.
 */
public interface IProxyConfig {
   InetSocketAddress getBindAddress();

   UUID getKey();
}
