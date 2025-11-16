package ru.balance.GenAI.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.util.PacketUtil;

public class AimJ extends Check implements PacketCheck {

    public AimJ(APlayer aPlayer) {
        super("AimJ", aPlayer);
    }

    private static final double scale = 1 << 16;
    private static final float minDelta = 0.05f;
    private static final float maxDelta = 60.0f;
    private static final double minStepDeg = 0.002;
    private static final double maxStepDeg = 0.30;
    private static final int windowSize = 20;
    private static final int minNonZero = 10;
    private static final double stepChangeRel = 0.25;
    private static final int candidateLock = 3;
    private static final int changesNeed = 2;
    private static final int ticksWindow = 240;
    private final long[] qBuf = new long[windowSize];
    private int idx = 0;
    private int count = 0;
    private long currentStep = 0;
    private long candidateStep = 0;
    private int candidateTicks = 0;
    private int changeCount = 0;
    private int ticksSinceFirstChange = 0;
    private boolean changeWindowOpen = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat()) return;
        if (!PacketUtil.isRotation(event)) return;

        if (changeWindowOpen) {
            ticksSinceFirstChange++;
            if (ticksSinceFirstChange > ticksWindow) {
                changeCount = 0;
                changeWindowOpen = false;
                ticksSinceFirstChange = 0;
            }
        }

        double ady = Math.abs(aPlayer.rotationData.deltaYaw);
        if (ady < minDelta || ady > maxDelta) {
            push(0);
            return;
        }

        long q = Math.max(1, Math.round(ady * scale));
        push(q);

        long g = 0;
        int nonzero = 0;
        for (int i = 0; i < count; i++) {
            long v = qBuf[i];
            if (v == 0) continue;
            nonzero++;
            g = (g == 0) ? v : gcd(g, v);
            if (g == 1) break;
        }
        if (nonzero < minNonZero || g <= 1) return;

        double stepDeg = g / scale;
        if (stepDeg < minStepDeg || stepDeg > maxStepDeg) return;

        if (currentStep == 0) {
            currentStep = g;
            candidateStep = 0;
            candidateTicks = 0;
            return;
        }

        double rel = Math.abs((double) g - currentStep) / currentStep;

        if (rel >= stepChangeRel) {
            if (candidateStep == 0 || Math.abs((double) g - candidateStep) / candidateStep <= 0.05) {
                candidateStep = g;
                candidateTicks++;
                if (candidateTicks >= candidateLock) {
                    currentStep = candidateStep;
                    candidateStep = 0;
                    candidateTicks = 0;

                    if (!changeWindowOpen) {
                        changeWindowOpen = true;
                        ticksSinceFirstChange = 0;
                        changeCount = 1;
                    } else {
                        changeCount++;
                        if (changeCount >= changesNeed) {
                            flag();
                            changeWindowOpen = false;
                            changeCount = 0;
                            ticksSinceFirstChange = 0;
                        }
                    }
                }
            } else {
                candidateStep = g;
                candidateTicks = 1;
            }
        } else {
            candidateStep = 0;
            candidateTicks = 0;
        }
    }

    private void push(long v) {
        if (count < windowSize) {
            qBuf[count++] = v;
        } else {
            qBuf[idx] = v;
            idx = (idx + 1) % windowSize;
        }
    }

    private long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return Math.abs(a);
    }
}
