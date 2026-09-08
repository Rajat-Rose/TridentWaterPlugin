package com.example.tridentwater;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LaunchpadCommand implements CommandExecutor, TabCompleter {

    private final TridentWaterPlugin plugin;

    public LaunchpadCommand(TridentWaterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /tridentlaunchpad <on|off>");
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            plugin.setLaunchpadEnabled(player.getUniqueId(), true);
            player.sendMessage(ChatColor.GREEN + "Crouch Trident Launchpad ENABLED!");
        } else if (args[0].equalsIgnoreCase("off")) {
            plugin.setLaunchpadEnabled(player.getUniqueId(), false);
            player.sendMessage(ChatColor.RED + "Crouch Trident Launchpad DISABLED!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Usage: /tridentlaunchpad <on|off>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if ("on".startsWith(args[0].toLowerCase())) options.add("on");
            if ("off".startsWith(args[0].toLowerCase())) options.add("off");
            return options;
        }
        return List.of();
    }
}
