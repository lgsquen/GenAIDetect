package ru.balance.GenAI.check.impl.aura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.util.Config;

public class AuraE extends Check implements PacketCheck {
    public AuraE(APlayer aPlayer) {
        super("AuraE", aPlayer);
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 2);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.5);
    }

    private double buffer;
    private double maxBuffer;
    private double bufferDecrease;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat()) return;

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                if (aPlayer.packetData.getMillisSinceLastWindowClose() < 100) {
                    buffer++;
                    if (buffer > maxBuffer) {
                        flag("Attack shortly after inventory");
                        buffer = 0;
                    }
                } else {
                    if (buffer > 0) buffer -= bufferDecrease;
                }
            }
        }
    }

    @Override
    public void onReload() {
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 2);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.5);
    }
}

