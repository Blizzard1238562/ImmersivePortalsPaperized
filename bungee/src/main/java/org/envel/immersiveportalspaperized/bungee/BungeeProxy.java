package org.envel.immersiveportalspaperized.bungee;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.UUID;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.proxy.IProxy;

/**
 * BungeeCord implementation of {@link IProxy}.
 */
@Singleton
public class BungeeProxy implements IProxy {
   private final Plugin pl;
   private final ProxyServer proxyServer;

   @Inject
   public BungeeProxy(Plugin pl) {
      this.pl = pl;
      this.proxyServer = pl.getProxy();
   }

   @Override
   public String getPluginVersion() {
      return this.pl.getDescription().getVersion();
   }

   @Nullable
   @Override
   public String findServer(InetSocketAddress clientAddress) {
      for (ServerInfo server : this.proxyServer.getServers().values()) {
         InetSocketAddress serverAddress = server.getAddress();
         if (serverAddress.equals(clientAddress)) {
            return server.getName();
         }
      }

      return null;
   }

   @Override
   public boolean serverExists(String serverName) {
      return this.proxyServer.getServerInfo(serverName) != null;
   }

   @Override
   public boolean playerExists(UUID uid) {
      return this.proxyServer.getPlayer(uid) != null;
   }

   @Override
   public void changePlayerServer(UUID uid, String destinationServer) {
      ProxiedPlayer playerInfo = this.proxyServer.getPlayer(uid);
      if (playerInfo == null) {
         throw new IllegalArgumentException(String.format("No player existed with UUID %s", uid));
      } else {
         ServerInfo serverInfo = this.proxyServer.getServerInfo(destinationServer);
         if (serverInfo == null) {
            throw new IllegalArgumentException(String.format("No server existed with the UUID %s", destinationServer));
         } else {
            playerInfo.connect(serverInfo);
         }
      }
   }
}
