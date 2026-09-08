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

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /zo <on|off>");
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            plugin.setZoEnabled(player.getUniqueId(), true);
            player.sendMessage(ChatColor.GREEN + "Permanent 3x3 Water Trail ENABLED!");
        } else if (args[0].equalsIgnoreCase("off")) {
            plugin.setZoEnabled(player.getUniqueId(), false);
            player.sendMessage(ChatColor.RED + "Permanent 3x3 Water Trail DISABLED!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Usage: /zo <on|off>");
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
