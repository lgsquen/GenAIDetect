package ru.balance.GenAI.entity;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.scheduler.BukkitTask;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.service.AnalysisService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Data
@RequiredArgsConstructor
public final class PlayerEntity {
    private final UUID uuid;
    private final String name;

    private float lastYaw = 0.0F;
    private float lastPitch = 0.0F;
    private float lastDeltaYaw = 0.0F;
    private float lastDeltaPitch = 0.0F;
    private float lastAccelYaw = 0.0F;
    private float lastAccelPitch = 0.0F;
    private final List<Frame> frames = new LinkedList<>();
    private List<Frame> lastAnalyzedFrames = null;

    private volatile boolean isProcessingFlag = false;
    private long combatTagUntil = 0L;
    private BukkitTask postCombatAnalysisTask = null;

    public void setLastAnalyzedFrames(List<Frame> frames) {
        this.lastAnalyzedFrames = new ArrayList<>(frames);
    }

    public boolean isInCombat() {
        return System.currentTimeMillis() < combatTagUntil;
    }

    public void tagCombat(long durationTicks) {
        this.combatTagUntil = System.currentTimeMillis() + (durationTicks * 50);

        if (postCombatAnalysisTask != null) {
            postCombatAnalysisTask.cancel();
        }

        postCombatAnalysisTask = GenAI.getInstance().getServer().getScheduler().runTaskLaterAsynchronously(
                GenAI.getInstance(),
                () -> {
                    int framesToAnalyze = GenAI.getInstance().getConfig().getInt("ml-check.frames-to-analyze", 150);
                    if (this.getFrames().size() >= framesToAnalyze) {
                        AnalysisService.analyze(this);
                    }
                    this.getFrames().clear();
                },
                durationTicks
        );
    }
}
