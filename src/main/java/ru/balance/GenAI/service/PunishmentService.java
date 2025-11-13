package ru.balance.GenAI.service;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.balance.GenAI.GenAI;

public class PunishmentService {

    public static void playAnimationAndPunish(Player target) {
        GenAI plugin = GenAI.getInstance();
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("punishment-animation");

        if (config == null || !config.getBoolean("enabled", true)) {
            String command = config != null ? config.getString("command", "") : "";
            if (!command.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", target.getName()));
            }
            return;
        }

        final int durationTicks = config.getInt("duration", 5) * 20;
        final int levitationStrength = config.getInt("levitation-strength", 1) - 1;
        final double radius = config.getDouble("particle.radius", 1.5);
        final int particleCount = config.getInt("particle.count", 10);
        final String finalCommand = config.getString("command", "kick %player% Наказан GenAI");

        final Particle.DustOptions dustOptions = getDustOptions(config);
        Sound startSound = getSound(config.getString("sounds.start-sound"), "ENTITY_EVOKER_PREPARE_SUMMON");
        Sound endSound = getSound(config.getString("sounds.end-sound"), "ENTITY_WITHER_DEATH");

        final Location startLocation = target.getLocation();
        target.setInvulnerable(true);
        target.getWorld().playSound(startLocation, startSound, 1.0f, 1.0f);
        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, durationTicks, levitationStrength, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, durationTicks, 255, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, durationTicks, 128, false, false));

        new BukkitRunnable() {
            private int ticksPassed = 0;

            @Override
            public void run() {
                if (!target.isOnline() || ticksPassed >= durationTicks) {
                    this.cancel();
                    target.removePotionEffect(PotionEffectType.LEVITATION);
                    target.removePotionEffect(PotionEffectType.SLOW);
                    target.removePotionEffect(PotionEffectType.JUMP);
                    target.setInvulnerable(false);
                    target.teleport(startLocation);
                    target.getWorld().playSound(target.getLocation(), endSound, 1.0f, 1.0f);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (target.isOnline()) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand.replace("%player%", target.getName()));
                            }
                        }
                    }.runTask(plugin);
                    return;
                }

                Location currentLocation = target.getLocation();
                if (currentLocation.getX() != startLocation.getX() || currentLocation.getZ() != startLocation.getZ()) {
                    currentLocation.setX(startLocation.getX());
                    currentLocation.setZ(startLocation.getZ());
                    target.teleport(currentLocation);
                }

                Location center = target.getLocation().add(0, 1, 0);
                double angleIncrement = (2 * Math.PI) / particleCount;
                double rotation = ticksPassed * 0.05;

                for (int i = 0; i < particleCount; i++) {
                    double angle = i * angleIncrement + rotation;
                    double x1 = center.getX() + radius * Math.cos(angle);
                    double z1 = center.getZ() + radius * Math.sin(angle);
                    target.getWorld().spawnParticle(Particle.REDSTONE, new Location(center.getWorld(), x1, center.getY(), z1), 0, dustOptions);

                    double y2 = center.getY() + radius * Math.cos(angle);
                    double z2 = center.getZ() + radius * Math.sin(angle);
                    target.getWorld().spawnParticle(Particle.REDSTONE, new Location(center.getWorld(), center.getX(), y2, z2), 0, dustOptions);
                }
                ticksPassed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static Particle.DustOptions getDustOptions(ConfigurationSection config) {
        String colorName = config.getString("particle.color", "ORANGE").toUpperCase();
        float size = (float) config.getDouble("particle.size", 1.0);
        Color color;
        switch (colorName) {
            case "RED": color = Color.RED; break;
            case "ORANGE": color = Color.ORANGE; break;
            case "YELLOW": color = Color.YELLOW; break;
            case "LIME": color = Color.LIME; break;
            case "GREEN": color = Color.GREEN; break;
            case "AQUA": color = Color.AQUA; break;
            case "BLUE": color = Color.BLUE; break;
            case "PURPLE": color = Color.PURPLE; break;
            case "WHITE": color = Color.WHITE; break;
            case "BLACK": color = Color.BLACK; break;
            default: color = Color.ORANGE;
        }
        return new Particle.DustOptions(color, size);
    }

    private static Sound getSound(String soundName, String defaultSound) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (Exception e) {
            return Sound.valueOf(defaultSound);
        }
    }
}
