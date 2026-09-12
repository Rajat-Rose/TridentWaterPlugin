package com.example.tridentwater;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TridentListener implements Listener {

    private final TridentWaterPlugin plugin;
    private final Set<UUID> fallProtectedPlayers = new HashSet<>();

    // Active Mining tracking for custom block hardness
    private final Map<UUID, BukkitTask> activeMiningTasks = new HashMap<>();
    private final Map<UUID, Location> activeMiningBlocks = new HashMap<>();

    public TridentListener(TridentWaterPlugin plugin) {
        this.plugin = plugin;
    }

    private Enchantment getEfficiencyEnchant() {
        Enchantment eff = Enchantment.getByKey(NamespacedKey.minecraft("efficiency"));
        if (eff == null) {
            eff = Enchantment.getByName("DIG_SPEED");
        }
        return eff;
    }

    private Enchantment getSilkTouchEnchant() {
        Enchantment st = Enchantment.getByKey(NamespacedKey.minecraft("silk_touch"));
        if (st == null) {
            st = Enchantment.getByName("SILK_TOUCH");
        }
        return st;
    }

    private Enchantment getRiptideEnchant() {
        Enchantment rip = Enchantment.getByKey(NamespacedKey.minecraft("riptide"));
        if (rip == null) {
            rip = Enchantment.getByName("RIPTIDE");
        }
        return rip;
    }

    private boolean isKaktaPickaxe(ItemStack tool) {
        if (tool == null || tool.getType() != Material.WOODEN_PICKAXE || !tool.hasItemMeta()) return false;
        ItemMeta meta = tool.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        if (!meta.getDisplayName().equalsIgnoreCase("kakta")) return false;
        Enchantment eff = getEfficiencyEnchant();
        return eff != null && tool.getEnchantmentLevel(eff) == 2;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 1. Break Bedrock / Illegal Blocks with "kakta" Wooden Pickaxe (Efficiency II) with Mining Delay
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getType() != Material.AIR) {
                ItemStack tool = player.getInventory().getItemInMainHand();
                if (isKaktaPickaxe(tool)) {
                    startMiningBlock(player, clickedBlock);
                }
            }
        }

        // 2. Launchpad Logic (Disabled in Nether)
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (plugin.isLaunchpadEnabled(player.getUniqueId()) && player.isSneaking()) {
                ItemStack item = player.getInventory().getItemInMainHand();
                Enchantment ripEnchant = getRiptideEnchant();
                if (item != null && item.getType() == Material.TRIDENT && ripEnchant != null && item.getEnchantmentLevel(ripEnchant) >= 3) {
                    create1x1Launchpad(player.getLocation(), 40L);
                }
            }
        }
    }

    private void startMiningBlock(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        Location targetLoc = block.getLocation();

        // Avoid re-starting if already mining this block
        if (activeMiningBlocks.containsKey(uuid) && activeMiningBlocks.get(uuid).equals(targetLoc)) {
            return;
        }

        stopMining(player);
        activeMiningBlocks.put(uuid, targetLoc);

        BukkitTask task = new BukkitRunnable() {
            int ticksElapsed = 0;
            final int totalTicksNeeded = 23; // ~1.15 seconds (Wooden Pickaxe speed on Stone)

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    stopMining(player);
                    this.cancel();
                    return;
                }

                ItemStack tool = player.getInventory().getItemInMainHand();
                Block currentTarget = player.getTargetBlockExact(5);

                // Stop mining if player looks away or changes tool
                if (!isKaktaPickaxe(tool) || currentTarget == null || !currentTarget.getLocation().equals(targetLoc)) {
                    stopMining(player);
                    this.cancel();
                    return;
                }

                ticksElapsed++;
                float progress = (float) ticksElapsed / (float) totalTicksNeeded;

                // Send block crack texture animation
                sendBlockCrackAnimation(targetLoc, progress);

                // Play mining hit sound every 4 ticks
                if (ticksElapsed % 4 == 0) {
                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_STONE_HIT, 1.0f, 1.0f);
                }

                // Complete mining
                if (ticksElapsed >= totalTicksNeeded) {
                    sendBlockCrackAnimation(targetLoc, 0.0f);

                    World world = targetLoc.getWorld();
                    world.playSound(targetLoc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
                    world.playEffect(targetLoc, Effect.STEP_SOUND, targetLoc.getBlock().getType());

                    if (targetLoc.getBlock().getType() != Material.AIR) {
                        world.dropItemNaturally(targetLoc.clone().add(0.5, 0.5, 0.5), new ItemStack(targetLoc.getBlock().getType()));
                        targetLoc.getBlock().setType(Material.AIR);
                    }

                    stopMining(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeMiningTasks.put(uuid, task);
    }

    private void stopMining(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeMiningTasks.containsKey(uuid)) {
            activeMiningTasks.get(uuid).cancel();
            activeMiningTasks.remove(uuid);
        }
        if (activeMiningBlocks.containsKey(uuid)) {
            Location loc = activeMiningBlocks.remove(uuid);
            if (loc != null && loc.getWorld() != null) {
                sendBlockCrackAnimation(loc, 0.0f);
            }
        }
    }

    private void sendBlockCrackAnimation(Location loc, float progress) {
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= 1024) {
                p.sendBlockDamage(loc, progress);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopMining(event.getPlayer());
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
            if (Math.abs(flyVector.getY()) < 0.15) {
                flyVector.setY(0.05);
            }
        }

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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item != null && item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            if (item.getItemMeta().getDisplayName().equalsIgnoreCase("Doble kr deneka")) {
                if (event.getBlockPlaced().getState() instanceof ShulkerBox shulker) {
                    shulker.setCustomName("Doble kr deneka");
                    shulker.update();
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getState() instanceof ShulkerBox shulker) {
            String customName = shulker.getCustomName();
            if (customName != null && customName.equalsIgnoreCase("Doble kr deneka")) {
                Player player = event.getPlayer();
                ItemStack tool = player.getInventory().getItemInMainHand();
                Enchantment stEnchant = getSilkTouchEnchant();

                if (tool != null && tool.getType() == Material.WOODEN_PICKAXE && stEnchant != null && tool.getEnchantmentLevel(stEnchant) > 0) {
                    event.setDropItems(false);

                    ItemStack originalShulker = new ItemStack(event.getBlock().getType());
                    BlockStateMeta originalMeta = (BlockStateMeta) originalShulker.getItemMeta();
                    if (originalMeta != null) {
                        originalMeta.setBlockState(shulker);
                        originalMeta.setDisplayName("Doble kr deneka");
                        originalShulker.setItemMeta(originalMeta);
                    }

                    shulker.setCustomName(null);
                    ItemStack duplicateShulker = new ItemStack(event.getBlock().getType());
                    BlockStateMeta duplicateMeta = (BlockStateMeta) duplicateShulker.getItemMeta();
                    if (duplicateMeta != null) {
                        duplicateMeta.setBlockState(shulker);
                        duplicateShulker.setItemMeta(duplicateMeta);
                    }

                    Location dropLoc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
                    event.getBlock().getWorld().dropItemNaturally(dropLoc, originalShulker);
                    event.getBlock().getWorld().dropItemNaturally(dropLoc, duplicateShulker);
                }
            }
        }
    }

    private Vector getDirectionVector(String dir) {
        switch (dir.toLowerCase()) {
            case "+x": return new Vector(1.4, 0.05, 0);
            case "-x": return new Vector(-1.4, 0.05, 0);
            case "+z": return new Vector(0, 0.05, 1.4);
            case "-z": return new Vector(0, 0.05, -1.4);
            case "+y": return new Vector(0, 1.4, 0);
            case "-y": return new Vector(0, -1.4, 0);
            default: return new Vector(1.4, 0.05, 0);
        }
    }

    private void stopFlight(Player player) {
        if (player != null && player.isOnline()) {
            player.setGravity(true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false));
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

    private void create1x1Launchpad(Location centerLoc, long restoreDelayTicks) {
        Block block = centerLoc.getBlock();
        BlockData originalData = block.getBlockData().clone();

        block.setType(Material.WATER, false);

        new BukkitRunnable() {
            @Override
            public void run() {
                block.setBlockData(originalData, false);
                block.getState().update(true, false);
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
