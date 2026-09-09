package com.example.tridentwater;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TridentWaterPlugin extends JavaPlugin {

    private final Set<UUID> zoEnabled = new HashSet<>();
    private final Set<UUID> zoInfinite = new HashSet<>();
    private final Set<UUID> launchpadEnabled = new HashSet<>();

    @Override
    public void onEnable() {
        ZoCommand zoCommand = new ZoCommand(this);
        if (getCommand("zo") != null) {
            getCommand("zo").setExecutor(zoCommand);
            getCommand("zo").setTabCompleter(zoCommand);
        }

        LaunchpadCommand launchpadCommand = new LaunchpadCommand(this);
        if (getCommand("tridentlaunchpad") != null) {
            getCommand("tridentlaunchpad").setExecutor(launchpadCommand);
            getCommand("tridentlaunchpad").setTabCompleter(launchpadCommand);
        }

        getServer().getPluginManager().registerEvents(new TridentListener(this), this);

        getLogger().info("TridentWaterPlugin enabled successfully!");
    }

    public boolean isZoEnabled(UUID uuid) { return zoEnabled.contains(uuid); }
    public void setZoEnabled(UUID uuid, boolean enable) {
        if (enable) zoEnabled.add(uuid);
        else {
            zoEnabled.remove(uuid);
            zoInfinite.remove(uuid);
        }
    }

    public boolean isZoInfinite(UUID uuid) { return zoInfinite.contains(uuid); }
    public void setZoInfinite(UUID uuid, boolean enable) {
        if (enable) zoInfinite.add(uuid);
        else zoInfinite.remove(uuid);
    }

    public boolean isLaunchpadEnabled(UUID uuid) { return launchpadEnabled.contains(uuid); }
    public void setLaunchpadEnabled(UUID uuid, boolean enable) {
        if (enable) launchpadEnabled.add(uuid);
        else launchpadEnabled.remove(uuid);
    }
}
