package ru.balance.GenAI.check.impl.aura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.util.Config;
import ru.balance.GenAI.check.util.PacketUtil;

public class AuraD extends Check implements PacketCheck {
    public AuraD(APlayer aPlayer) {
        super("AuraD", aPlayer);
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 1);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.25);
    }

    private int buffer;
    private double maxBuffer;
    private double bufferDecrease;

    private double deltaYaw;
    private double lastDeltaYaw;

    private final double max = 60;
    private final double min = 2.5;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat()) return;

        if (PacketUtil.isAttack(event)) {
            deltaYaw = Math.abs(aPlayer.rotationData.deltaYaw);
            lastDeltaYaw = Math.abs(aPlayer.rotationData.lastDeltaYaw);

            if (deltaYaw > max && lastDeltaYaw < min) {
                buffer++;
                if (buffer > maxBuffer) {
                    flag();
                    buffer = 0;
                }
            } else {
                if (buffer > 0) buffer -= bufferDecrease;
            }
        }
    }

    @Override
    public void onReload() {
        this.maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.25);
    }
}
