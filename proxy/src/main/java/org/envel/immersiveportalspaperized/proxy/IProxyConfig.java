package org.envel.immersiveportalspaperized.proxy;

import java.net.InetSocketAddress;
import java.util.UUID;

public interface IProxyConfig {
   InetSocketAddress getBindAddress();

   UUID getKey();
}
