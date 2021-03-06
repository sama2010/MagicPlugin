package com.elmakers.mine.bukkit.citizens;

import net.citizensnpcs.api.util.DataKey;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.logging.Level;

public class CommandCitizensTrait extends CitizensTrait {
    private String command;
    private boolean console = true;;
    private boolean op = false;

	public CommandCitizensTrait() {
		super("command");
	}

	public void load(DataKey data) {
        super.load(data);
        command = data.getString("command", null);
        console = data.getBoolean("console", true);
        op = data.getBoolean("op", false);
	}

	public void save(DataKey data) {
        super.save(data);
        data.setString("command", command);
        data.setBoolean("console", console);;
        data.setBoolean("op", op);
	}

    public boolean perform(net.citizensnpcs.api.event.NPCRightClickEvent event){
        if (command == null || command.isEmpty()) return false;

        CommandSender sender = event.getClicker();
        Player player = event.getClicker();
        boolean result = true;
        boolean isOp = sender.isOp();
        if (op && !isOp) {
            sender.setOp(true);
        }
        Location location = player.getLocation();
        CommandSender executor = console ? Bukkit.getConsoleSender() : player;

        try {
            String converted = command
                .replace("@pd", player.getDisplayName())
                .replace("@pn", player.getName())
                .replace("@p", player.getName())
                .replace("@uuid", player.getUniqueId().toString())
                .replace("@world", location.getWorld().getName())
                .replace("@x", Double.toString(location.getX()))
                .replace("@y", Double.toString(location.getY()))
                .replace("@z", Double.toString(location.getZ()));;

            api.getPlugin().getServer().dispatchCommand(executor, converted);
        } catch (Exception ex) {
            result = false;
            api.getLogger().log(Level.WARNING, "Error running command: " + command, ex);
        }
        if (op && !isOp) {
            sender.setOp(false);
        }
        return result;
    }

    public void describe(CommandSender sender)
    {
        super.describe(sender);
        String commandDescription = command == null ? (ChatColor.RED + "(None)") : (ChatColor.LIGHT_PURPLE + command);
        sender.sendMessage(ChatColor.DARK_PURPLE + "Command: " + commandDescription);
        String consoleDescription = console ? (ChatColor.GRAY + "Console") : (ChatColor.LIGHT_PURPLE + "Player");
        sender.sendMessage(ChatColor.DARK_PURPLE + "Executor: " + consoleDescription);
        String opDescription = console ? (ChatColor.RED + "YES") : (ChatColor.GRAY + "NO");
        sender.sendMessage(ChatColor.DARK_PURPLE + "Op Player: " + opDescription);
    }

    public void configure(CommandSender sender, String key, String value)
    {
        if (key == null)
        {
            return;
        }
        if (key.equalsIgnoreCase("command"))
        {
            command = value;
            if (value == null)
            {
                sender.sendMessage(ChatColor.RED + "Cleared command");
            }
            else
            {
                sender.sendMessage(ChatColor.DARK_PURPLE + "Set command to: " + ChatColor.LIGHT_PURPLE + command);
            }
        }
        else if (key.equalsIgnoreCase("op"))
        {
            if (value == null || !value.equalsIgnoreCase("true"))
            {
                sender.sendMessage(ChatColor.DARK_PURPLE + "Player commands run normally");
                op = false;
            }
            else
            {
                op = true;
                sender.sendMessage(ChatColor.DARK_PURPLE + "Player commands run as OP");
            }
            updatePotionEffects();
        }
        else if (key.equalsIgnoreCase("console"))
        {
            if (value == null || !value.equalsIgnoreCase("true"))
            {
                sender.sendMessage(ChatColor.DARK_PURPLE + "Set executor to player");
                console = false;
            }
            else
            {
                console = true;
                sender.sendMessage(ChatColor.DARK_PURPLE + "Set executor to console");
            }
            updatePotionEffects();
        }
        else
        {
            super.configure(sender, key, value);
        }
    }
}