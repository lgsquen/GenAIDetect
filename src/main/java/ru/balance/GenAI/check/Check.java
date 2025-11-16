package ru.balance.GenAI.check;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.check.util.Config;
import ru.balance.GenAI.entity.PlayerEntity;
import ru.balance.GenAI.registry.PlayerRegistry;

public abstract class Check {
    private final String name;
    protected final APlayer aPlayer;
    private boolean enabled;
    private final boolean experimental;
    private boolean alert;
    private int violations;
    private int maxViolations;
    public int hitCancelTicks;
    public int hitTicksToCancel;
    private final Plugin plugin;

    private BukkitTask decayTask;

    public Check(String name, APlayer aPlayer) {
        this.name = name;
        this.aPlayer = aPlayer;
        this.enabled = Config.getBoolean("checks." + name + ".enabled", true);
        this.experimental = name.contains("*");
        this.alert = Config.getBoolean("checks." + name + ".alert", true);
        this.maxViolations = Config.getInt("checks." + name + ".max-violations", 10);
        this.hitCancelTicks = Config.getInt("checks." + name + ".hit-cancel-ticks", 20);
        this.hitTicksToCancel = 0;
        this.plugin = GenAI.getInstance();

        startDecayTask();
    }

    protected void flag() {
        flag("");
    }

    protected void flag(String verbose) {
        if (!experimental) {
            this.hitTicksToCancel += hitCancelTicks;
        }

        if (violations < maxViolations) {
            violations++;
            aPlayer.globalVl++;
            aPlayer.kaNpcVl++;
        }

        PlayerEntity entity = PlayerRegistry.getPlayer(aPlayer.getBukkitPlayer().getUniqueId());
        if (entity != null) {
            GenAI.getInstance().getViolationManager().handleLegacyViolation(entity, name);
        }
    }

    private void startDecayTask() {
        long rawDelay = Config.getInt(getConfigPath() + ".remove-violations-after", 300);
        long delay = rawDelay * 20L;

        this.decayTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (violations > 0) {
                    int decay = Config.getInt(getConfigPath() + ".decay", 1);
                    violations -= decay;
                    if (violations < 0) violations = 0;
                }
            }
        }.runTaskTimer(plugin, delay, delay);
    }

    public void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void cancelDecayTask() {
        if (decayTask != null) {
            decayTask.cancel();
        }
    }

    public void resetViolations() {
        this.violations = 0;
        aPlayer.globalVl = 0;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getConfigPath() {
        return "checks." + name;
    }

    public boolean alert() {
        return alert;
    }

    public int getViolations() {
        return violations;
    }

    public int getMaxViolations() {
        return maxViolations;
    }

    public void reload() {
        this.enabled = Config.getBoolean(getConfigPath() + ".enabled", true);
        this.alert = Config.getBoolean(getConfigPath() + ".alert", true);
        this.maxViolations = Config.getInt(getConfigPath() + ".max-violations", 10);
        this.hitCancelTicks = Config.getInt(getConfigPath() + ".hit-cancel-ticks", 20);
    }
}
