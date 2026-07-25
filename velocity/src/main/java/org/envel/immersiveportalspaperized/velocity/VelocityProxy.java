package org.envel.immersiveportalspaperized.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.proxy.IProxy;

@Singleton
public class VelocityProxy implements IProxy {
   private final ProxyServer proxyServer;
   private final String pluginVersion;

   @Inject
   public VelocityProxy(ProxyServer proxyServer, @Named("pluginVersion") String pluginVersion) {
      this.proxyServer = proxyServer;
      this.pluginVersion = pluginVersion;
   }

   @Deprecated
   @Nullable
   @Override
   public String findServer(InetSocketAddress clientAddress) {
      for (RegisteredServer server : this.proxyServer.getAllServers()) {
         InetSocketAddress serverAddress = server.getServerInfo().getAddress();
         if (serverAddress.equals(clientAddress)) {
            return server.getServerInfo().getName();
         }
      }

      return null;
   }

   @Override
   public boolean serverExists(String serverName) {
      return this.proxyServer.getServer(serverName).isPresent();
   }

   @Override
   public boolean playerExists(UUID uid) {
      return this.proxyServer.getPlayer(uid).isPresent();
   }

   @Override
   public void changePlayerServer(UUID uid, String destinationServer) {
      Optional<Player> player = this.proxyServer.getPlayer(uid);
      if (player.isEmpty()) {
         throw new IllegalArgumentException(String.format("No player existed with UUID %s", uid));
      } else {
         Optional<RegisteredServer> server = this.proxyServer.getServer(destinationServer);
         if (server.isEmpty()) {
            throw new IllegalArgumentException(String.format("No server existed with the UUID %s", destinationServer));
         } else {
            player.get().createConnectionRequest(server.get()).fireAndForget();
         }
      }
   }

   @Generated
   @Override
   public String getPluginVersion() {
      return this.pluginVersion;
   }
}
