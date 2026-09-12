package com.example.tridentwater;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class LaunchpadCommand implements CommandExecutor, TabCompleter {

    private final TridentWaterPlugin plugin;

    public LaunchpadCommand(TridentWaterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("on")) {
                plugin.setLaunchpadEnabled(player.getUniqueId(), true);
                player.sendMessage(ChatColor.GREEN + "Launchpad ENABLED!");
                return true;
            } else if (args[0].equalsIgnoreCase("off")) {
                plugin.setLaunchpadEnabled(player.getUniqueId(), false);
                player.sendMessage(ChatColor.RED + "Launchpad DISABLED!");
                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "Usage: /launchpad <on|off>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off");
        }
        return List.of();
    }
}
