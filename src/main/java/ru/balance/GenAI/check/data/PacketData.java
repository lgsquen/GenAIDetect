package ru.balance.GenAI.check.data;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import lombok.Getter;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;

@Getter
public class PacketData extends Check implements PacketCheck {
    private long lastWindowClick = -1;
    private long lastWindowClose = -1;
    private long lastSprintStart = -1;
    private long lastSprintStop = -1;
    private long lastElytraStart = -1;

    public PacketData(APlayer aPlayer) {
        super("PacketData", aPlayer);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        long nowNano = System.nanoTime();

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            lastWindowClick = nowNano;
        }

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            lastWindowClose = nowNano;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            switch (wrapper.getAction()) {
                case START_SPRINTING:
                    lastSprintStart = nowNano;
                    break;
                case STOP_SPRINTING:
                    lastSprintStop = nowNano;
                    break;
                case START_FLYING_WITH_ELYTRA:
                    lastElytraStart = nowNano;
                    break;
                default:
                    break;
            }
        }
    }

    public long getLastClickWindowMillis() {
        return lastWindowClick / 1_000_000;
    }

    public long getMillisSinceLastClickWindow() {
        return lastWindowClick == -1 ? Long.MAX_VALUE : (System.nanoTime() - lastWindowClick) / 1_000_000;
    }

    public long getLastWindowCloseMillis() {
        return lastWindowClose / 1_000_000;
    }

    public long getMillisSinceLastWindowClose() {
        return lastWindowClose == -1 ? Long.MAX_VALUE : (System.nanoTime() - lastWindowClose) / 1_000_000;
    }

    public long getLastSprintStartMillis() {
        return lastSprintStart / 1_000_000;
    }

    public long getMillisSinceLastSprintStart() {
        return lastSprintStart == -1 ? Long.MAX_VALUE : (System.nanoTime() - lastSprintStart) / 1_000_000;
    }

    public long getLastSprintStopMillis() {
        return lastSprintStop / 1_000_000;
    }

    public long getMillisSinceLastSprintStop() {
        return lastSprintStop == -1 ? Long.MAX_VALUE : (System.nanoTime() - lastSprintStop) / 1_000_000;
    }

    public long getLastElytraStartMillis() {
        return lastElytraStart / 1_000_000;
    }

    public long getMillisSinceLastElytraStart() {
        return lastElytraStart == -1 ? Long.MAX_VALUE : (System.nanoTime() - lastElytraStart) / 1_000_000;
    }
}
