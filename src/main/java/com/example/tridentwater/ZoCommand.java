package com.example.tridentwater;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ZoCommand implements CommandExecutor, TabCompleter {

    private final TridentWaterPlugin plugin;

    public ZoCommand(TridentWaterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return true;
        }

        if (args.length == 1 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
            player.sendMessage(ChatColor.RED + "Command not set");
            return true;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("super")) {
            if (args[0].equalsIgnoreCase("on")) {
                boolean isInfinite = args.length >= 3 && args[2].equalsIgnoreCase("infinite");
                String dir = (args.length >= 4) ? args[3] : null;

                if (dir != null && !isValidDir(dir)) {
                    player.sendMessage(ChatColor.RED + "Invalid direction! Use: +x, -x, +z, -z, +y, -y");
                    return true;
                }

                plugin.setZoEnabled(player.getUniqueId(), true);
                plugin.setZoInfinite(player.getUniqueId(), isInfinite);
                plugin.setZoDirection(player.getUniqueId(), dir);

                String msg = ChatColor.GREEN + "ZO Super " + (isInfinite ? "Infinite " : "") + (dir != null ? "[" + dir.toUpperCase() + "] " : "") + "ENABLED!";
                player.sendMessage(msg);
            } else if (args[0].equalsIgnoreCase("off")) {
                plugin.setZoEnabled(player.getUniqueId(), false);
                player.sendMessage(ChatColor.RED + "ZO Super DISABLED!");
            } else {
                player.sendMessage(ChatColor.RED + "Command not set");
            }
            return true;
        }

        player.sendMessage(ChatColor.RED + "Command not set");
        return true;
    }

    private boolean isValidDir(String dir) {
        return dir.equalsIgnoreCase("+x") || dir.equalsIgnoreCase("-x") ||
               dir.equalsIgnoreCase("+z") || dir.equalsIgnoreCase("-z") ||
               dir.equalsIgnoreCase("+y") || dir.equalsIgnoreCase("-y");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off");
        } else if (args.length == 2) {
            return List.of("super");
        } else if (args.length == 3) {
            return List.of("infinite");
        } else if (args.length == 4 && args[2].equalsIgnoreCase("infinite")) {
            return List.of("+x", "-x", "+z", "-z", "+y", "-y");
        }
        return List.of();
    }
}
