package com.example.tridentwater;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class TridentListener implements Listener {

    private final TridentWaterPlugin plugin;

    public TridentListener(TridentWaterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        int riptideLevel = item.getEnchantmentLevel(Enchantment.RIPTIDE);
        boolean isCrouching = player.isSneaking() || player.getPose() == Pose.SNEAKING;

        // 1. Temporary Launchpad Water (Triggers if /tridentlaunchpad is ON + Crouch/Sneak + Riptide 3)
        if (plugin.isLaunchpadEnabled(player.getUniqueId()) && isCrouching && riptideLevel >= 3) {
            createTemporaryWaterGrid(player.getLocation(), 1, 60L); // 3 seconds temporary launch water
        }

        // 2. Permanent 3x3 Water Trail (Triggers if /zo is ON)
        if (plugin.isZoEnabled(player.getUniqueId())) {
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || player.isDead() || ticks > 30) {
                        this.cancel();
                        return;
                    }

                    // Places PERMANENT 3x3 flowing water blocks
                    createPermanentWaterGrid(player.getLocation(), 1);
                    ticks += 2;
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }
    }

    // Creates temporary water grid with physics (true) that restores original blocks
    private void createTemporaryWaterGrid(Location centerLoc, int radius, long restoreDelayTicks) {
        Map<Block, BlockData> originalBlocks = new HashMap<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = centerLoc.clone().add(x, 0, z).getBlock();
                if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                    originalBlocks.put(block, block.getBlockData().clone());
                    block.setType(Material.WATER, true); // true = normal flowing water physics
                }
            }
        }

        if (!originalBlocks.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Map.Entry<Block, BlockData> entry : originalBlocks.entrySet()) {
                        Block b = entry.getKey();
                        if (b.getType() == Material.WATER) {
                            b.setBlockData(entry.getValue(), true);
                        }
                    }
                }
            }.runTaskLater(plugin, restoreDelayTicks);
        }
    }

    // Creates PERMANENT 3x3 flowing water grid
    private void createPermanentWaterGrid(Location centerLoc, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = centerLoc.clone().add(x, 0, z).getBlock();
                if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                    block.setType(Material.WATER, true); // true = normal flowing water physics
                }
            }
        }
    }
}
