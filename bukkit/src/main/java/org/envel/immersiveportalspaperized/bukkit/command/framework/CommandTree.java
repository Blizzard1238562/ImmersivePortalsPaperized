package org.envel.immersiveportalspaperized.bukkit.command.framework;

import io.foxserver.common.locale.LocaleAPI;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.envel.immersiveportalspaperized.bukkit.command.TestingCommands;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Aliases;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Command;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Path;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerDataManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

@Singleton
public class CommandTree {
   private final MessageConfig messages;
   private final Logger logger;
   private final IPlayerDataManager playerDataManager;
   private final TestingCommands testingCommands;
   private final LocaleAPI localeApi;
   private final ParentCommand rootNode;

   @Inject
   public CommandTree(MessageConfig messages, Logger logger, IPlayerDataManager playerDataManager, TestingCommands testingCommands, LocaleAPI localeApi) {
      this.messages = messages;
      this.logger = logger;
      this.playerDataManager = playerDataManager;
      this.testingCommands = testingCommands;
      this.localeApi = localeApi;
      this.rootNode = new ParentCommand(logger, messages, localeApi, true);
   }

   private void registerCommand(Object obj, Method method) {
      String[] aliases = new String[0];
      String path = "";

      for (Annotation annotation : method.getAnnotations()) {
         if (annotation instanceof Path) {
            path = ((Path)annotation).value();
         } else if (annotation instanceof Aliases) {
            aliases = ((Aliases)annotation).value();
         }
      }

      if (path.length() == 0) {
         throw new InvalidCommandException("No valid Path annotation found");
      } else {
         path = path.toLowerCase();
         String[] pathElements = path.split("/");
         SubCommand command = new SubCommand(obj, method, this.messages, this.logger, this.playerDataManager);
         this.rootNode.recursivelyAdd(pathElements, command);

         for (String alias : aliases) {
            this.addAlias(path, alias);
         }
      }
   }

   public void registerCommands(Object obj) {
      this.logger.fine("Registering commands on Object of type %s", obj.getClass().getName());

      for (Method method : obj.getClass().getMethods()) {
         for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof Command) {
               this.registerCommand(obj, method);
            }
         }
      }
   }

   public void addAlias(String path, String alias) {
      this.rootNode.addCommandAlias(path.split("/"), alias);
   }

   public boolean onGlobalCommand(CommandSender sender, String label, String[] args) {
      List<String> argsList = new ArrayList<>(Arrays.asList(args));
      argsList.add(0, label);
      args = argsList.toArray(new String[0]);

      try {
         return this.rootNode.execute(sender, "/", args);
      } catch (CommandException var6) {
         sender.sendMessage(ChatColor.RED + var6.getMessage());
         return false;
      }
   }

   public List<String> onGlobalTabComplete(CommandSender sender, String label, String[] args) {
      List<String> argsList = new ArrayList<>(Arrays.asList(args));
      argsList.add(0, label);
      if (argsList.get(argsList.size() - 1).isBlank()) {
         argsList.remove(argsList.size() - 1);
      }

      args = argsList.toArray(new String[0]);
      return this.rootNode.tabComplete(sender, args);
   }

   public void registerTestCommands() {
      this.logger.fine("Registering test commands");
      this.registerCommands(this.testingCommands);
   }
}
