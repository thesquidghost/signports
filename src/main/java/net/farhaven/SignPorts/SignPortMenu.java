package net.farhaven.SignPorts;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SignPortMenu implements Listener {
    private final SignPorts plugin;
    private Map<String, SignPortSetup> signPorts;
    private final SignPortGUI gui;

    public SignPortMenu(SignPorts plugin) {
        this.plugin = plugin;
        this.signPorts = new HashMap<>();
        this.gui = new SignPortGUI(plugin);
    }

    public void setSignPorts(Map<String, SignPortSetup> signPorts) {
        this.signPorts = new HashMap<>(signPorts);
        plugin.getLogger().info("SignPorts set in menu. Total: " + signPorts.size());
        for (Map.Entry<String, SignPortSetup> entry : signPorts.entrySet()) {
            plugin.getLogger().info("SignPort in menu: " + entry.getValue().getName() + " by " + entry.getValue().getOwnerName());
        }
    }

    public void openSignPortMenu(Player player) {
        gui.openSignPortMenu(player, 1);
    }

    public void addSignPort(SignPortSetup setup) {
        signPorts.put(setup.getName().toLowerCase(), setup);
    }

    public Map<String, SignPortSetup> getSignPorts() {
        return new HashMap<>(signPorts); // Return a copy to prevent external modification
    }

    public void removeSignPort(String name) {
        signPorts.remove(name.toLowerCase());
    }

    public SignPortSetup getSignPortByLocation(Location location) {
        for (SignPortSetup setup : signPorts.values()) {
            if (setup.getSignLocation().equals(location)) {
                return setup;
            }
        }
        return null;
    }

    public SignPortSetup getSignPortByName(String name) {
        return signPorts.get(name.toLowerCase());
    }

    public SignPortSetup getSignPortByOwner(UUID ownerUUID) {
        for (SignPortSetup setup : signPorts.values()) {
            if (setup.getOwnerUUID().equals(ownerUUID)) {
                return setup;
            }
        }
        return null;
    }

    public void reloadSignPorts() {
        signPorts.clear();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, SignPortSetup> loadedSignPorts = plugin.getSignPortStorage().getSignPorts();
            for (SignPortSetup setup : loadedSignPorts.values()) {
                Bukkit.getScheduler().runTask(plugin, () -> addSignPort(setup));
            }
            plugin.getLogger().info("SignPorts reloaded successfully. Total: " + signPorts.size());
        });
    }

    public void handleSignPortClick(Player player, String signPortName) {
        SignPortSetup setup = getSignPortByName(signPortName);
        if (setup == null) {
            // Reload the configuration if the SignPort is not found
            plugin.reloadPluginConfig();

            // Re-check the SignPort existence after reloading
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SignPortSetup reloadedSetup = getSignPortByName(signPortName);
                if (reloadedSetup == null) {
                    player.sendMessage(ChatColor.RED + "That SignPort no longer exists.");
                    plugin.getLogger().info("SignPort not found: '" + signPortName + "' after reload.");
                } else {
                    // Handle the SignPort click normally
                    handleValidSignPortClick(player, reloadedSetup);
                }
            }, 20L); // Delay to ensure the reload has completed
        } else {
            // Handle the SignPort click normally
            handleValidSignPortClick(player, setup);
        }
    }

    private void handleValidSignPortClick(Player player, SignPortSetup setup) {
        Location location = setup.getSignLocation();
        if (plugin.isSafeLocation(location)) {
            if (plugin.checkCooldown(player)) {
                plugin.getLogger().info("Location is safe and cooldown passed, initiating teleport countdown");
                player.closeInventory(); // Close the GUI
                if (setup.isLocked()) {
                    player.sendMessage(ChatColor.RED + "This SignPort is locked and cannot be teleported to.");
                    return;
                }
                new TeleportTask(plugin, player, location, setup.getName()).runTaskTimer(plugin, 0L, 20L);
            } else {
                plugin.getLogger().info("Teleportation cancelled for " + player.getName() + " to " + setup.getName() + ". Cooldown active.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "The destination is not safe. Teleportation cancelled.");
            plugin.getLogger().info("Teleportation cancelled for " + player.getName() + " to " + setup.getName() + ". Unsafe location.");
        }
    }
}