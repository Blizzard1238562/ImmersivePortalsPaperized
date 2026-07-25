package org.envel.immersiveportalspaperized.bukkit.command.framework;

import org.bukkit.command.CommandSender;

public interface ICommand {
   boolean execute(CommandSender sender, String pathToCall, String[] args) throws CommandException;
}
