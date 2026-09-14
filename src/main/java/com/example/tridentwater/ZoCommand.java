package com.example.tridentwater;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ZoCommand implements CommandExecutor, TabCompleter {

    private final TridentWaterPlugin plugin;

    public ZoCommand(TridentWaterPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isHoldingTrishul(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType() == Material.TRIDENT && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            return meta != null && meta.hasDisplayName() && meta.getDisplayName().equalsIgnoreCase("trishul");
        }
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /zo <on|off> [super] [infinite] [<direction>]");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("off")) {
            plugin.setZoEnabled(player.getUniqueId(), false);
            player.sendMessage(ChatColor.RED + "ZO Riptide mode DISABLED!");
            return true;
        }

        if (sub.equals("on")) {
            if (!isHoldingTrishul(player)) {
                player.sendMessage(ChatColor.RED + "You must hold a Trident named 'trishul' in your main hand to use /zo on!");
                return true;
            }

            plugin.setZoEnabled(player.getUniqueId(), true);

            boolean isInfinite = false;
            String direction = null;

            for (int i = 1; i < args.length; i++) {
                String arg = args[i].toLowerCase();
                if (arg.equals("infinite") || arg.equals("super")) {
                    isInfinite = true;
                } else if (arg.equals("+x") || arg.equals("-x") || arg.equals("+y") || arg.equals("-y") || arg.equals("+z") || arg.equals("-z")) {
                    direction = arg;
                }
            }

            plugin.setZoInfinite(player.getUniqueId(), isInfinite);
            plugin.setZoDirection(player.getUniqueId(), direction);

            String modeMsg = isInfinite ? " (INFINITE)" : "";
            String dirMsg = (direction != null) ? " [Dir: " + direction.toUpperCase() + "]" : "";
            player.sendMessage(ChatColor.GREEN + "ZO Riptide mode ENABLED!" + modeMsg + dirMsg);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /zo <on|off> [super] [infinite] [<direction>]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off");
        } else if (args.length >= 2) {
            return List.of("infinite", "super", "+x", "-x", "+y", "-y", "+z", "-z");
        }
        return List.of();
    }
}
