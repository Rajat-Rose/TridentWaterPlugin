package com.example.tridentwater;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class TridentListener implements Listener {

    private final TridentWaterPlugin plugin;

    public TridentListener(TridentWaterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (plugin.isLaunchpadEnabled(player.getUniqueId()) && player.isSneaking()) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() == Material.TRIDENT && item.getEnchantmentLevel(Enchantment.RIPTIDE) >= 3) {
                    createTemporaryWaterGrid(player.getLocation(), 1, 60L);
                }
            }
        }
    }

    @EventHandler
    public void onRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();

        if (!plugin.isZoEnabled(player.getUniqueId())) return;

        boolean isInfinite = plugin.isZoInfinite(player.getUniqueId());
        Vector flyVector = player.getLocation().getDirection().normalize().multiply(1.5);

        new BukkitRunnable() {
            int ticks = 0;
            float lastYaw = player.getLocation().getYaw();
            float lastPitch = player.getLocation().getPitch();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                if (isInfinite) {
                    float currentYaw = player.getLocation().getYaw();
                    float currentPitch = player.getLocation().getPitch();

                    // Detect deliberate sharp turn (> 30 deg in a single tick)
                    if (Math.abs(currentYaw - lastYaw) > 30.0f || Math.abs(currentPitch - lastPitch) > 30.0f) {
                        this.cancel();
                        return;
                    }

                    lastYaw = currentYaw;
                    lastPitch = currentPitch;

                    // Continuous smooth push & seamless 3D water trail
                    player.setVelocity(flyVector);
                    create3DWaterGrid(player.getLocation(), 1);
                } else {
                    if (ticks > 30) {
                        this.cancel();
                        return;
                    }
                    create3DWaterGrid(player.getLocation(), 1);
                    ticks += 2;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createTemporaryWaterGrid(Location centerLoc, int radius, long restoreDelayTicks) {
        Map<Block, BlockData> originalBlocks = new HashMap<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = centerLoc.clone().add(x, 0, z).getBlock();
                if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                    originalBlocks.put(block, block.getBlockData().clone());
                    block.setType(Material.WATER, true);
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

    private void create3DWaterGrid(Location centerLoc, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= 1; y++) {
                    Block block = centerLoc.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                        block.setType(Material.WATER, true);
                    }
                }
            }
        }
    }
}
