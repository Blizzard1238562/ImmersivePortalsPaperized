package org.envel.immersiveportalspaperized.bukkit.command;

import org.bukkit.command.CommandSender;
import org.envel.immersiveportalspaperized.bukkit.ImmersivePortalsPaperized;
import org.envel.immersiveportalspaperized.bukkit.command.framework.CommandException;
import org.envel.immersiveportalspaperized.bukkit.command.framework.CommandTree;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Command;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Description;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Path;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.RequiresPermissions;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.config.ProxyConfig;
import org.envel.immersiveportalspaperized.bukkit.net.IClientReconnectHandler;
import org.envel.immersiveportalspaperized.bukkit.net.IPortalClient;
import org.envel.immersiveportalspaperized.bukkit.util.performance.OperationTimer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * MainCommands.
 */
@Singleton
public class MainCommands {
   private final ImmersivePortalsPaperized pl;
   private final Logger logger;
   private final MessageConfig messageConfig;
   private final IPortalClient portalClient;
   private final ProxyConfig proxyConfig;
   private final IClientReconnectHandler reconnectHandler;

   @Inject
   public MainCommands(
      ImmersivePortalsPaperized pl,
      Logger logger,
      MessageConfig messageConfig,
      CommandTree commandTree,
      IPortalClient portalClient,
      ProxyConfig proxyConfig,
      IClientReconnectHandler reconnectHandler
   ) {
      this.pl = pl;
      this.logger = logger;
      this.messageConfig = messageConfig;
      this.portalClient = portalClient;
      this.proxyConfig = proxyConfig;
      this.reconnectHandler = reconnectHandler;
      commandTree.registerCommands(this);
      // NOTE: plugin.yml registers "bp" (and "p") as the actual Bukkit-level command aliases,
      // which route into onCommand()/onTabComplete() with that exact string as the `label`
      // parameter. The internal CommandTree only resolves labels it has explicitly been told
      // about via addAlias() - this used to only register "p" here, even though "bp" is the
      // alias used in every permission description and the one players actually type. That
      // mismatch meant every "/bp ..." command silently did nothing (execute() falls into the
      // root ParentCommand's else-branch, which is a no-op since isRoot=true) and "/bp " + Tab
      // returned no completions, while "/p" was never reachable at all since Bukkit didn't know
      // about it (fixed in plugin.yml alongside this).
      commandTree.addAlias("immersiveportalspaperized", "p");
      commandTree.addAlias("immersiveportalspaperized", "bp");
   }

   @Command
   @Path("immersiveportalspaperized/reload")
   @Description("Reloads the plugin and the config file")
   @RequiresPermissions({"immersiveportalspaperized.reload"})
   public boolean reload(CommandSender sender) {
      OperationTimer timer = new OperationTimer();
      this.pl.softReload();
      sender.sendMessage(String.format("%s (%.03fms)", this.messageConfig.getChatMessage(sender, "reload"), timer.getTimeTakenMillis()));
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/reconnect")
   @Description("Reconnects to the proxy if disconnect")
   @RequiresPermissions({"immersiveportalspaperized.reconnect"})
   public boolean reconnect(CommandSender sender) throws CommandException {
      if (!this.proxyConfig.isEnabled()) {
         throw new CommandException(this.messageConfig.getErrorMessage(sender, "proxyDisabled"));
      } else if (this.portalClient.isConnectionOpen()) {
         throw new CommandException(this.messageConfig.getErrorMessage(sender, "alreadyConnected"));
      } else {
         sender.sendMessage(this.messageConfig.getChatMessage(sender, "startedReconnection"));
         this.reconnectHandler.prematureReconnect();
         return true;
      }
   }
}


