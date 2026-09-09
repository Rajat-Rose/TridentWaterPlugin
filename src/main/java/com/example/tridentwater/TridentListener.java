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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TridentListener implements Listener {

    private final TridentWaterPlugin plugin;
    private final Set<UUID> fallProtectedPlayers = new HashSet<>();

    public TridentListener(TridentWaterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (plugin.isLaunchpadEnabled(player.getUniqueId()) && player.isSneaking()) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null && item.getType() == Material.TRIDENT && item.getEnchantmentLevel(Enchantment.RIPTIDE) >= 3) {
                    // 1x1 Launchpad water at exact player feet location
                    create1x1Launchpad(player.getLocation(), 60L);
                }
            }
        }
    }

    @EventHandler
    public void onRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();

        if (!plugin.isZoEnabled(player.getUniqueId())) return;

        boolean isInfinite = plugin.isZoInfinite(player.getUniqueId());
        String customDir = plugin.getZoDirection(player.getUniqueId());

        Vector flyVector;
        if (customDir != null) {
            flyVector = getDirectionVector(customDir);
        } else {
            flyVector = player.getLocation().getDirection().normalize().multiply(1.4);
        }

        // Disable gravity during ZO flight to stop falling
        player.setGravity(false);

        new BukkitRunnable() {
            int ticks = 0;
            float lastYaw = player.getLocation().getYaw();
            float lastPitch = player.getLocation().getPitch();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    stopFlight(player);
                    this.cancel();
                    return;
                }

                player.setFallDistance(0.0f);

                if (isInfinite) {
                    float currentYaw = player.getLocation().getYaw();
                    float currentPitch = player.getLocation().getPitch();

                    // Cancel infinite flight on sharp camera turn (> 25 degrees)
                    if (Math.abs(currentYaw - lastYaw) > 25.0f || Math.abs(currentPitch - lastPitch) > 25.0f) {
                        stopFlight(player);
                        this.cancel();
                        return;
                    }

                    lastYaw = currentYaw;
                    lastPitch = currentPitch;

                    player.setVelocity(flyVector);
                    create3DWaterGrid(player.getLocation(), 2);
                    create3DWaterGrid(player.getLocation().clone().add(flyVector), 2);
                } else {
                    if (ticks > 35) {
                        stopFlight(player);
                        this.cancel();
                        return;
                    }
                    player.setVelocity(flyVector);
                    create3DWaterGrid(player.getLocation(), 2);
                    create3DWaterGrid(player.getLocation().clone().add(flyVector), 2);
                    ticks += 2;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Vector getDirectionVector(String dir) {
        return switch (dir.toLowerCase()) {
            case "+x" -> new Vector(1.4, 0, 0);
            case "-x" -> new Vector(-1.4, 0, 0);
            case "+z" -> new Vector(0, 0, 1.4);
            case "-z" -> new Vector(0, 0, -1.4);
            case "+y" -> new Vector(0, 1.4, 0);
            case "-y" -> new Vector(0, -1.4, 0);
            default -> new Vector(1.4, 0, 0);
        };
    }

    private void stopFlight(Player player) {
        if (player != null && player.isOnline()) {
            player.setGravity(true);
            grantFallProtection(player.getUniqueId());
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (fallProtectedPlayers.contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    fallProtectedPlayers.remove(player.getUniqueId());
                }
            }
        }
    }

    private void grantFallProtection(UUID uuid) {
        fallProtectedPlayers.add(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {
                fallProtectedPlayers.remove(uuid);
            }
        }.runTaskLater(plugin, 100L);
    }

    // 1x1 Feet Launchpad Fix with reliable Block Restoration
    private void create1x1Launchpad(Location centerLoc, long restoreDelayTicks) {
        Block block = centerLoc.getBlock();
        BlockData originalData = block.getBlockData().clone();

        block.setType(Material.WATER, false);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Force physics update on restore to cleanly clear water graphics
                block.setBlockData(originalData, true);
            }
        }.runTaskLater(plugin, restoreDelayTicks);
    }

    private void create3DWaterGrid(Location centerLoc, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -2; y <= 2; y++) {
                    Block block = centerLoc.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                        block.setType(Material.WATER, true);
                    }
                }
            }
        }
    }
}
