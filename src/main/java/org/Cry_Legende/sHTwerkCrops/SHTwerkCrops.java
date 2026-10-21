package org.Cry_Legende.sHTwerkCrops;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SHTwerkCrops extends JavaPlugin implements Listener {

    // Stockage des cooldowns par joueur
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    // Paramètres chargés depuis la config
    private int radius;
    // La chance est désormais exprimée sur 100
    private int chance;
    private long cooldownTime; // en millisecondes
    private Set<String> blacklist;
    private Set<String> worldBlacklist;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SHTwerkCrops plugin enabled!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        radius = config.getInt("radius", 2);
        // Par défaut, 50% de chance
        chance = config.getInt("chance", 50);
        cooldownTime = config.getLong("cooldown", 5000);
        blacklist = new HashSet<>(config.getStringList("blacklist"));
        worldBlacklist = new HashSet<>(config.getStringList("world-blacklist"));
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        // Exécuter uniquement lorsque le joueur commence à s'accroupir
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        World world = player.getWorld();
        // Si le monde du joueur est blacklisté, on ne fait rien
        if (worldBlacklist.contains(world.getName())) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        // Vérifier le cooldown
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < cooldownTime) {
            return;
        }
        boolean success = false;

        // Parcourir les blocs dans un cube autour du joueur
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) { // limiter la hauteur
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = player.getLocation().clone().add(dx, dy, dz).getBlock();
                    if (block == null) continue;
                    Material type = block.getType();
                    // Si le type est dans la blacklist, ignorer ce bloc
                    if (blacklist.contains(type.toString())) continue;
                    // Vérifier que le bloc est ageable
                    if (block.getBlockData() instanceof Ageable) {
                        Ageable ageable = (Ageable) block.getBlockData();
                        if (ageable.getAge() < ageable.getMaximumAge()) {
                            // Appliquer la chance (échelle sur 100)
                            if (ThreadLocalRandom.current().nextInt(100) < chance) {
                                // Augmenter l'âge d'un cran sans dépasser le maximum
                                ageable.setAge(Math.min(ageable.getAge() + 1, ageable.getMaximumAge()));
                                block.setBlockData(ageable);
                                // Afficher les particules directement sur la crop (1 bloc plus bas)
                                block.getWorld().spawnParticle(
                                    Particle.HAPPY_VILLAGER,
                                    block.getLocation().add(0.5, 0, 0.5),
                                    5, 0.2, 0.2, 0.2, 0.0
                                );
                                success = true;
                            }
                        }
                    }
                }
            }
        }
        // Mettre à jour le cooldown si une ou plusieurs cultures ont été stimulées
        if (success) {
            cooldowns.put(uuid, now);
        }
    }
}
