package org.envel.immersiveportalspaperized.bukkit.command.framework;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Argument;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Arguments;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Command;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Description;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.RequiresPermissions;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.RequiresPlayer;
import org.envel.immersiveportalspaperized.bukkit.config.MessageConfig;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerData;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerDataManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.envel.immersiveportalspaperized.shared.util.ReflectionUtil;

public class SubCommand implements ICommand {
   private final Object instance;
   private final Method method;
   private final MessageConfig messageConfig;
   private final Logger logger;
   private final IPlayerDataManager playerDataManager;
   private static final Map<Class<?>, Method> valueOfCache = new HashMap<>();
   private boolean requiresPlayer = false;
   private boolean usePlayerData;
   private String[] requiredPermissions = new String[0];
   private Argument[] arguments = new Argument[0];
   private Class<?>[] argumentTypes;
   @Getter
   private String description = "";
   @Getter
   private String usage;

   public String getArgumentsUsage() {
      StringBuilder builder = new StringBuilder();
      int argTypeIndex = 0;

      for (Argument argument : this.arguments) {
         if (this.argumentTypes[argTypeIndex].equals(Vector.class)) {
            builder.append(String.format(" <%sX> <%sY> <%sZ>", argument.name(), argument.name(), argument.name()));
         } else {
            boolean required = argument.defaultValue().equals("");
            if (required) {
               builder.append(String.format(" <%s>", argument.name()));
            } else {
               builder.append(String.format(" [%s]", argument.name()));
            }
         }

         argTypeIndex++;
      }

      return builder.toString().trim();
   }

   SubCommand(Object instance, Method method, MessageConfig messageConfig, Logger logger, IPlayerDataManager playerDataManager) {
      this.instance = instance;
      this.method = method;
      this.messageConfig = messageConfig;
      this.logger = logger;
      this.playerDataManager = playerDataManager;
      if (method.getReturnType() != boolean.class) {
         throw new InvalidCommandException("Command annotated methods must return a boolean");
      } else {
         this.loadFromMethod();
         this.checkArgTypes();
         this.generateUsage();
      }
   }

   private void loadFromMethod() {
      this.argumentTypes = Arrays.copyOfRange(this.method.getParameterTypes(), 1, this.method.getParameterCount());
      boolean commandAnnotationFound = false;

      for (Annotation annotation : this.method.getAnnotations()) {
         if (annotation instanceof Command) {
            commandAnnotationFound = true;
         } else if (annotation instanceof RequiresPlayer) {
            this.requiresPlayer = true;
         } else if (annotation instanceof RequiresPermissions) {
            this.requiredPermissions = ((RequiresPermissions)annotation).value();
         } else if (annotation instanceof Arguments) {
            this.arguments = ((Arguments)annotation).value();
         } else if (annotation instanceof Argument) {
            this.arguments = new Argument[]{(Argument)annotation};
         } else if (annotation instanceof Description) {
            this.description = ((Description)annotation).value();
         }
      }

      if (!commandAnnotationFound) {
         throw new InvalidCommandException("Command methods require the command annotation");
      }
   }

   private void checkArgTypes() {
      Parameter[] methodParams = this.method.getParameters();
      if (methodParams.length != this.arguments.length + 1) {
         throw new InvalidCommandException(
            "Incorrect number of arguments on command method. Commands must have 1 argument for the sender and one argument per annotated argument"
         );
      } else {
         Class<?> firstParamType = methodParams[0].getType();
         boolean isFirstArgValid = true;
         if (this.requiresPlayer) {
            if (firstParamType.isAssignableFrom(IPlayerData.class)) {
               this.usePlayerData = true;
            } else if (!firstParamType.isAssignableFrom(Player.class)) {
               isFirstArgValid = false;
            }
         } else if (!firstParamType.isAssignableFrom(CommandSender.class)) {
            isFirstArgValid = false;
         }

         if (!isFirstArgValid) {
            throw new InvalidCommandException(
               "The first argument for a command must be a CommandSender. (or a Player/IPlayerData if annotated with a player requirement)"
            );
         }
      }
   }

   private void generateUsage() {
      StringBuilder builder = new StringBuilder();
      int argTypeIndex = 0;

      for (Argument argument : this.arguments) {
         if (this.argumentTypes[argTypeIndex].equals(Vector.class)) {
            builder.append(String.format(" <%sX> <%sY> <%sZ>", argument.name(), argument.name(), argument.name()));
         } else {
            boolean required = argument.defaultValue().equals("");
            if (required) {
               builder.append(String.format(" <%s>", argument.name()));
            } else {
               builder.append(String.format(" [%s]", argument.name()));
            }
         }

         argTypeIndex++;
      }

      if (!this.description.isEmpty()) {
         builder.append(": ");
         builder.append(this.description);
      }

      this.usage = builder.toString().trim();
   }

   private Vector parseVector(CommandSender sender, String x, String y, String z) throws CommandException {
      this.logger.fine("Attempting to parse vector from %s, %s, %s", x, y, z);

      try {
         return this.normalizeLocalCoordinates(x, y, z, sender instanceof Player player ? player.getLocation() : null);
      } catch (NumberFormatException var6) {
         throw new CommandException(String.format(this.messageConfig.getErrorMessage("invalidCoordinates"), x, y, z));
      }
   }

   private World parseWorld(CommandSender sender, String worldName) throws CommandException {
      World world;
      if (sender instanceof Player player && "<local>".equals(worldName)) {
         world = player.getWorld();
      } else {
         world = Bukkit.getWorld(worldName);
      }

      if (world == null) {
         throw new CommandException(this.messageConfig.getErrorMessage("noWorldExistsWithGivenName").replace("{name}", worldName));
      } else {
         return world;
      }
   }

   private Vector normalizeLocalCoordinates(@NotNull String x, @NotNull String y, @NotNull String z, @Nullable Location playerLocation) throws CommandException, NumberFormatException {
      int coordinateType = 0;
      Vector normalizedLocation = new Vector(0, 0, 0);

      for (String coordinateToConvert : new String[]{x, y, z}) {
         if (coordinateToConvert.replace("~", "").replace("^", "").length() == 0) {
            coordinateToConvert = coordinateToConvert + "0";
         }

         if (coordinateToConvert.charAt(0) == '~') {
            if (playerLocation == null) {
               throw new CommandException(this.messageConfig.getErrorMessage("cannotUseRelativeCoordinatesWithoutPlayer"));
            }

            switch (coordinateType) {
               case 0:
                  normalizedLocation.setX(playerLocation.getBlockX() + Integer.parseInt(coordinateToConvert.replace("~", "")));
                  break;
               case 1:
                  normalizedLocation.setY(playerLocation.getBlockY() + Integer.parseInt(coordinateToConvert.replace("~", "")));
                  break;
               case 2:
                  normalizedLocation.setZ(playerLocation.getBlockZ() + Integer.parseInt(coordinateToConvert.replace("~", "")));
            }
         } else if (coordinateToConvert.charAt(0) == '^') {
            if (playerLocation == null) {
               throw new CommandException(this.messageConfig.getErrorMessage("cannotUseRelativeCoordinatesWithoutPlayer"));
            }

            if (coordinateType == 1) {
               normalizedLocation.setY(playerLocation.getBlockY() + Integer.parseInt(coordinateToConvert.replace("^", "")));
               continue;
            }

            float playerYaw = Location.normalizeYaw(playerLocation.getYaw());
            if (playerYaw >= -45.0F && playerYaw < 45.0F) {
               switch (coordinateType) {
                  case 0:
                     normalizedLocation.setX(playerLocation.getBlockX() + Integer.parseInt(coordinateToConvert.replace("^", "")));
                     break;
                  case 2:
                     normalizedLocation.setZ(playerLocation.getBlockZ() + Integer.parseInt(coordinateToConvert.replace("^", "")));
               }
            } else if (playerYaw >= 45.0F && playerYaw < 135.0F) {
               switch (coordinateType) {
                  case 0:
                     normalizedLocation.setZ(playerLocation.getBlockZ() + Integer.parseInt(coordinateToConvert.replace("^", "")));
                     break;
                  case 2:
                     normalizedLocation.setX(playerLocation.getBlockX() - Integer.parseInt(coordinateToConvert.replace("^", "")));
               }
            } else if (playerYaw >= 135.0F && playerYaw <= 180.0F || playerYaw >= -180.0F && playerYaw < -135.0F) {
               switch (coordinateType) {
                  case 0:
                     normalizedLocation.setX(playerLocation.getBlockX() - Integer.parseInt(coordinateToConvert.replace("^", "")));
                     break;
                  case 2:
                     normalizedLocation.setZ(playerLocation.getBlockZ() - Integer.parseInt(coordinateToConvert.replace("^", "")));
               }
            } else if (playerYaw >= -135.0F && playerYaw < -45.0F) {
               switch (coordinateType) {
                  case 0:
                     normalizedLocation.setZ(playerLocation.getBlockZ() - Integer.parseInt(coordinateToConvert.replace("^", "")));
                     break;
                  case 2:
                     normalizedLocation.setX(playerLocation.getBlockX() + Integer.parseInt(coordinateToConvert.replace("^", "")));
               }
            }
         } else {
            switch (coordinateType) {
               case 0:
                  normalizedLocation.setX(Integer.parseInt(coordinateToConvert));
                  break;
               case 1:
                  normalizedLocation.setY(Integer.parseInt(coordinateToConvert));
                  break;
               case 2:
                  normalizedLocation.setZ(Integer.parseInt(coordinateToConvert));
            }
         }

         coordinateType++;
      }

      return normalizedLocation;
   }

   private Object parseArgument(CommandSender sender, Class<?> type, String argument) throws CommandException {
      this.logger.fine("Attempting to parse string \"%s\" as type %s", argument, type.getName());

      try {
         if (type == String.class) {
            return argument;
         } else if (type == World.class) {
            return this.parseWorld(sender, argument);
         } else if (type == int.class) {
            return Integer.parseInt(argument);
         } else if (type == short.class) {
            return Short.parseShort(argument);
         } else if (type == long.class) {
            return Long.parseLong(argument);
         } else if (type == byte.class) {
            return Byte.parseByte(argument);
         } else if (type == boolean.class) {
            if (argument.equalsIgnoreCase("true") || argument.equalsIgnoreCase("yes")) {
               return true;
            } else if (!argument.equalsIgnoreCase("false") && !argument.equalsIgnoreCase("no")) {
               throw new CommandException(this.messageConfig.getErrorMessage("invalidBoolean").replace("{arg}", argument));
            } else {
               return false;
            }
         } else if (type.isPrimitive()) {
            throw new InvalidCommandException("Unknown primitive type on command argument");
         } else {
            return this.runValueOfMethod(type, argument);
         }
      } catch (IllegalArgumentException var5) {
         throw new CommandException(this.messageConfig.getErrorMessage("invalidArgs"), var5);
      }
   }

   private Object runValueOfMethod(Class<?> type, String argument) throws CommandException {
      Method method = valueOfCache.get(type);
      if (method == null) {
         method = ReflectionUtil.findMethod(type, "valueOf", String.class);
         valueOfCache.put(type, method);
      }

      try {
         return ReflectionUtil.invokeMethod(null, method, argument);
      } catch (IllegalArgumentException var5) {
         throw new CommandException(this.messageConfig.getErrorMessage("invalidArgs"), var5);
      }
   }

   private void displayUsage(String pathToCall) throws CommandException {
      throw new CommandException("Usage: " + pathToCall + this.usage);
   }

   public boolean hasPermissions(CommandSender sender) {
      for (String permission : this.requiredPermissions) {
         if (!sender.hasPermission(permission)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public boolean execute(CommandSender sender, String pathToCall, String[] args) throws CommandException {
      if (!this.hasPermissions(sender)) {
         throw new CommandException(this.messageConfig.getErrorMessage("notEnoughPerms"));
      } else if (this.requiresPlayer && !(sender instanceof Player)) {
         throw new CommandException(this.messageConfig.getErrorMessage("mustBePlayer"));
      } else {
         List<Object> parsedArgs = new ArrayList<>();
         if (this.usePlayerData) {
            IPlayerData playerData = this.playerDataManager.getPlayerData((Player)sender);
            if (playerData == null) {
               throw new IllegalStateException("Player called command without registered player data");
            }

            parsedArgs.add(playerData);
         } else {
            parsedArgs.add(sender);
         }

         int argumentIdx = 0;
         int argumentTypesIdx = 0;

         for (Argument argument : this.arguments) {
            Class<?> argumentType = this.argumentTypes[argumentTypesIdx];
            boolean isVector = argumentType.equals(Vector.class);
            int argsRequired = isVector ? 3 : 1;
            boolean wasEntered = argumentIdx + argsRequired - 1 < args.length;
            boolean isRequired = isVector || argument.defaultValue().equals("");
            if (isRequired && !wasEntered) {
               this.displayUsage(pathToCall);
            }

            if (isVector) {
               parsedArgs.add(this.parseVector(sender, args[argumentIdx], args[argumentIdx + 1], args[argumentIdx + 2]));
            } else {
               String givenValue = wasEntered ? args[argumentIdx] : argument.defaultValue();
               parsedArgs.add(this.parseArgument(sender, argumentType, givenValue));
            }

            argumentIdx += argsRequired;
            argumentTypesIdx++;
         }

         try {
            return (Boolean)this.method.invoke(this.instance, parsedArgs.toArray());
         } catch (IllegalAccessException var17) {
            throw new InvalidCommandException("Command annotated methods must be public");
         } catch (InvocationTargetException var18) {
            Throwable cause = var18.getCause();
            if (cause instanceof RuntimeException) {
               throw (RuntimeException)cause;
            } else if (cause instanceof CommandException) {
               throw (CommandException)cause;
            } else {
               throw new RuntimeException(cause);
            }
         }
      }
   }
}
