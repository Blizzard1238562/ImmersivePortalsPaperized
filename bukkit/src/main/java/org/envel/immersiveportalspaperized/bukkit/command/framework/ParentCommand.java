package org.envel.immersiveportalspaperized.bukkit.command.framework;

import io.foxserver.common.locale.LocaleAPI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.util.ArrayUtil;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

public class ParentCommand implements ICommand {
   private final Logger logger;
   private final MessageConfig messageConfig;
   private final LocaleAPI localeApi;
   private final Map<String, ICommand> subCommands = new HashMap<>();
   private final Set<String> aliases = new HashSet<>();
   private final boolean isRoot;

   ParentCommand(Logger logger, MessageConfig messageConfig, LocaleAPI localeApi, boolean isRoot) {
      this.logger = logger;
      this.messageConfig = messageConfig;
      this.localeApi = localeApi;
      this.isRoot = isRoot;
   }

   ParentCommand(Logger logger, MessageConfig messageConfig, LocaleAPI localeApi) {
      this(logger, messageConfig, localeApi, false);
   }

   @Override
   public boolean execute(CommandSender sender, String pathToCall, String[] args) throws CommandException {
      if (args.length != 0 && !args[0].equalsIgnoreCase("help") && this.subCommands.containsKey(args[0].toLowerCase())) {
         String subCommandName = args[0].toLowerCase();
         String newPathToCall = String.format("%s%s ", pathToCall, subCommandName);
         ICommand subCommand = this.subCommands.get(subCommandName);
         return subCommand.execute(sender, newPathToCall, ArrayUtil.removeFirstElement(args));
      } else {
         if (!this.isRoot) {
            int page = 1;
            if (args.length > 0 && args[0].equalsIgnoreCase("help") && args.length > 1) {
               try {
                  page = Integer.parseInt(args[1]);
               } catch (NumberFormatException var7) {
               }
            }

            this.displayHelp(sender, pathToCall, page);
         }

         return false;
      }
   }

   private Map<String, ICommand> filterSubCommands(CommandSender sender) {
      Map<String, ICommand> result = new HashMap<>();
      this.subCommands.forEach((name, command) -> {
         if (command instanceof ParentCommand) {
            result.put(name, command);
         } else {
            SubCommand subCommand = (SubCommand)command;
            if (subCommand.hasPermissions(sender)) {
               result.put(name, command);
            }
         }
      });
      return result;
   }

   List<String> tabComplete(CommandSender sender, String[] args) {
      if (args.length == 0) {
         return new ArrayList<>(this.filterSubCommands(sender).keySet());
      } else {
         String lastArg = args[0];
         ICommand validEnteredCommand = this.subCommands.get(lastArg);
         if (validEnteredCommand instanceof ParentCommand) {
            return ((ParentCommand)validEnteredCommand).tabComplete(sender, ArrayUtil.removeFirstElement(args));
         } else if (validEnteredCommand == null) {
            List<String> result = new ArrayList<>();

            for (String command : this.filterSubCommands(sender).keySet()) {
               if (command.startsWith(lastArg)) {
                  result.add(command);
               }
            }

            return result;
         } else {
            return new ArrayList<>();
         }
      }
   }

   private void displayHelp(CommandSender sender, String pathToCall, int page) {
      Map<String, ICommand> filtered = this.filterSubCommands(sender);
      if (filtered.isEmpty()) {
         sender.sendMessage(this.messageConfig.getChatMessage("noCommands"));
      } else {
         List<ParentCommand.HelpEntry> entries = new ArrayList<>();
         filtered.forEach((name, subCommand) -> {
            if (!this.aliases.contains(name)) {
               entries.add(new ParentCommand.HelpEntry(name, subCommand));
            }
         });
         entries.sort(Comparator.comparing(ParentCommand.HelpEntry::getName));
         int itemsPerPage = 6;
         int totalPages = (int)Math.ceil((double)entries.size() / itemsPerPage);
         if (totalPages == 0) {
            totalPages = 1;
         }

         if (page < 1) {
            page = 1;
         }

         if (page > totalPages) {
            page = totalPages;
         }

         int startIndex = (page - 1) * itemsPerPage;
         int endIndex = Math.min(startIndex + itemsPerPage, entries.size());
         MiniMessage mm = MiniMessage.miniMessage();
         Player player = sender instanceof Player ? (Player)sender : null;
         String headerStr = this.localeApi.getRaw(player, "help_header");
         if (headerStr == null) {
            headerStr = "<gold><bold>ImmersivePortalsPaperized Help</bold></gold> <gray>(Page {page}/{total_pages})</gray>";
         }

         headerStr = headerStr.replace("{page}", String.valueOf(page)).replace("{total_pages}", String.valueOf(totalPages));
         sender.sendMessage(mm.deserialize(headerStr));
         sender.sendMessage(mm.deserialize("<yellow>──────────────────────────────────────────────────</yellow>"));

         for (int i = startIndex; i < endIndex; i++) {
            ParentCommand.HelpEntry entry = entries.get(i);
            String name = entry.getName();
            ICommand subCommand = entry.getSubCommand();
            String path = pathToCall.trim();
            if (path.startsWith("/")) {
               path = path.substring(1);
            }

            String cleanCommand = "/" + path + " " + name;
            String label = "<gold>• <yellow>/" + path + " <white>" + name + "</white>";
            String desc = "";
            String argsUsage = "";
            if (subCommand instanceof SubCommand sc) {
               argsUsage = sc.getArgumentsUsage();
               desc = sc.getDescription();
            }

            String pathForTranslation = pathToCall.trim();
            if (pathForTranslation.startsWith("/")) {
               pathForTranslation = pathForTranslation.substring(1);
            }

            if (pathForTranslation.startsWith("p")) {
               pathForTranslation = "immersiveportalspaperized" + pathForTranslation.substring(1);
            }

            String keyName = (pathForTranslation + "_" + name).replace(" ", "_").replace("/", "_").toLowerCase();
            String localizedDesc = this.localeApi.getRaw(player, "commands." + keyName + ".description");
            if (localizedDesc == null) {
               String camelKeyName = keyName;
               if (keyName.equals("immersiveportalspaperized_linkportals")) {
                  camelKeyName = "immersiveportalspaperized_linkPortals";
               } else if (keyName.equals("immersiveportalspaperized_linkexternalportals")) {
                  camelKeyName = "immersiveportalspaperized_linkExternalPortals";
               } else if (keyName.equals("immersiveportalspaperized_getallownonplayerteleportation")) {
                  camelKeyName = "immersiveportalspaperized_getallowNonPlayerTeleportation";
               } else if (keyName.equals("immersiveportalspaperized_setallownonplayerteleportation")) {
                  camelKeyName = "immersiveportalspaperized_setAllowNonPlayerTeleportation";
               } else if (keyName.equals("immersiveportalspaperized_setportalname")) {
                  camelKeyName = "immersiveportalspaperized_setPortalName";
               } else if (keyName.equals("immersiveportalspaperized_setorigin")) {
                  camelKeyName = "immersiveportalspaperized_setOrigin";
               } else if (keyName.equals("immersiveportalspaperized_setdestination")) {
                  camelKeyName = "immersiveportalspaperized_setDestination";
               }

               localizedDesc = this.localeApi.getRaw(player, "commands." + camelKeyName + ".description");
            }

            if (localizedDesc != null) {
               desc = localizedDesc;
            }

            if (!argsUsage.isEmpty()) {
               label = label + " <gray>" + argsUsage + "</gray>";
            }

            label = label + "</yellow>";
            if (!desc.isEmpty()) {
               label = label + " <dark_gray>-</dark_gray> <gray>" + desc + "</gray>";
            }

            String autofillStr = this.localeApi.getRaw(player, "autofill_suggest");
            if (autofillStr == null) {
               autofillStr = "Click to autofill command:";
            }

            String hoverText = "<yellow>" + autofillStr + "<br><white>" + cleanCommand + (argsUsage.isEmpty() ? "" : " " + argsUsage) + "</white>";
            if (!desc.isEmpty()) {
               hoverText = hoverText + "<br><br><gray>" + desc + "</gray>";
            }

            String miniMessageString = "<click:suggest_command:'" + cleanCommand + " '><hover:show_text:'" + hoverText + "'>" + label + "</hover></click>";
            sender.sendMessage(mm.deserialize(miniMessageString));
         }

         sender.sendMessage(mm.deserialize("<yellow>──────────────────────────────────────────────────</yellow>"));
         if (totalPages > 1) {
            String footer = "";
            String prevText = this.localeApi.getRaw(player, "help_previous");
            if (prevText == null) {
               prevText = "[◀ Previous]";
            }

            String nextText = this.localeApi.getRaw(player, "help_next");
            if (nextText == null) {
               nextText = "[Next ▶]";
            }

            String goToPageText = this.localeApi.getRaw(player, "help_go_to_page");
            if (goToPageText == null) {
               goToPageText = "<green>Go to page {page}";
            }

            if (page > 1) {
               String hoverPrev = goToPageText.replace("{page}", String.valueOf(page - 1));
               footer = footer
                  + "<click:run_command:'/bp help "
                  + (page - 1)
                  + "'><hover:show_text:'"
                  + hoverPrev
                  + "'><gold><b>"
                  + prevText
                  + "</b></gold></hover></click>";
            } else {
               footer = footer + "<dark_gray>" + prevText + "</dark_gray>";
            }

            String pageIndicator = this.localeApi.getRaw(player, "help_page_indicator");
            if (pageIndicator == null) {
               pageIndicator = "<yellow>Page {page} of {total_pages}</yellow>";
            }

            pageIndicator = pageIndicator.replace("{page}", String.valueOf(page)).replace("{total_pages}", String.valueOf(totalPages));
            footer = footer + "  " + pageIndicator + "  ";
            if (page < totalPages) {
               String hoverNext = goToPageText.replace("{page}", String.valueOf(page + 1));
               footer = footer
                  + "<click:run_command:'/bp help "
                  + (page + 1)
                  + "'><hover:show_text:'"
                  + hoverNext
                  + "'><gold><b>"
                  + nextText
                  + "</b></gold></hover></click>";
            } else {
               footer = footer + "<dark_gray>" + nextText + "</dark_gray>";
            }

            sender.sendMessage(mm.deserialize("<gray>" + footer + "</gray>"));
         } else {
            String clickInfo = this.localeApi.getRaw(player, "help_click_info");
            if (clickInfo == null) {
               clickInfo = "<gray>\ud83d\udca1 Click on any command to copy it to your chat box.</gray>";
            }

            sender.sendMessage(mm.deserialize(clickInfo));
         }
      }
   }

   void addCommandAlias(String[] remainingElements, String aliasName) {
      String originalName = remainingElements[0];
      if (remainingElements.length > 1) {
         ICommand nextCommand = this.subCommands.get(originalName);
         if (!(nextCommand instanceof ParentCommand)) {
            throw new IllegalArgumentException("Invalid original name for alias");
         }

         ((ParentCommand)nextCommand).addCommandAlias(ArrayUtil.removeFirstElement(remainingElements), aliasName);
      } else {
         ICommand toBeAliased = this.subCommands.get(originalName);
         if (toBeAliased == null) {
            throw new IllegalArgumentException("Invalid original name for alias");
         }

         if (this.subCommands.containsKey(aliasName)) {
            this.logger.warning("Override existing command with alias");
         }

         this.subCommands.put(aliasName, toBeAliased);
         this.aliases.add(aliasName);
      }
   }

   void recursivelyAdd(String[] remainingElements, SubCommand command) {
      String currentName = remainingElements[0];
      if (remainingElements.length == 1) {
         if (this.subCommands.containsKey(currentName)) {
            this.logger.warning("Overriding previously existing command");
         }

         this.subCommands.put(currentName, command);
      } else {
         if (!this.subCommands.containsKey(currentName)) {
            this.subCommands.put(currentName, new ParentCommand(this.logger, this.messageConfig, this.localeApi));
         }

         ParentCommand nextInLine = (ParentCommand)this.subCommands.get(currentName);
         nextInLine.recursivelyAdd(ArrayUtil.removeFirstElement(remainingElements), command);
      }
   }

   private static class HelpEntry {
      private final String name;
      private final ICommand subCommand;

      public HelpEntry(String name, ICommand subCommand) {
         this.name = name;
         this.subCommand = subCommand;
      }

      public String getName() {
         return this.name;
      }

      public ICommand getSubCommand() {
         return this.subCommand;
      }
   }
}
