package com.example.tridentwater;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ZoCommand implements CommandExecutor, TabCompleter {

    private final TridentWaterPlugin plugin;

    public ZoCommand(TridentWaterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        // Show red error if only /zo on or /zo off is used
        if (args.length == 1 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
            player.sendMessage(ChatColor.RED + "Command not set");
            return true;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("super")) {
            boolean isInfinite = args.length >= 3 && args[2].equalsIgnoreCase("infinite");

            if (args[0].equalsIgnoreCase("on")) {
                plugin.setZoEnabled(player.getUniqueId(), true);
                plugin.setZoInfinite(player.getUniqueId(), isInfinite);
                player.sendMessage(ChatColor.GREEN + "ZO Super " + (isInfinite ? "Infinite " : "") + "ENABLED!");
            } else if (args[0].equalsIgnoreCase("off")) {
                plugin.setZoEnabled(player.getUniqueId(), false);
                plugin.setZoInfinite(player.getUniqueId(), false);
                player.sendMessage(ChatColor.RED + "ZO Super DISABLED!");
            } else {
                player.sendMessage(ChatColor.RED + "Command not set");
            }
            return true;
        }

        player.sendMessage(ChatColor.RED + "Command not set");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off");
        } else if (args.length == 2) {
            return List.of("super");
        } else if (args.length == 3) {
            return List.of("infinite");
        }
        return List.of();
    }
}
