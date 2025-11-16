package ru.balance.GenAI.check.data;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.entity.Player;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.PlayerIdRegistry;
import ru.balance.GenAI.check.util.Config;
import ru.balance.GenAI.check.util.PacketUtil;

public class ActionData extends Check implements PacketCheck {

    private long lastAttack = -1L;
    private boolean attack = false;
    private boolean interact = false;
    private boolean swing = false;
    private boolean startSprint = false;
    private boolean stopSprint = false;
    private int combatTicks;
    private Player pTarget;

    public ActionData(APlayer aPlayer) {
        super("ActionData", aPlayer);
        this.combatTicks = Config.getInt(getConfigPath() + ".combat-ticks", 60);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (PacketUtil.isAttack(event)) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            lastAttack = System.nanoTime();
            attack = true;
            pTarget = PlayerIdRegistry.get(wrapper.getEntityId());
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            interact = true;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            switch (wrapper.getAction()) {
                case START_SPRINTING:
                    startSprint = true;
                    stopSprint = false;
                    break;
                case STOP_SPRINTING:
                    stopSprint = true;
                    startSprint = false;
                    break;
                default:
                    break;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            swing = true;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            attack = false;
            interact = false;
            startSprint = false;
            stopSprint = false;
            swing = false;
        }
    }

    public boolean hasAttackedSince(long timeMillis) {
        if (lastAttack == -1) return false;
        long elapsedNanos = System.nanoTime() - lastAttack;
        return (elapsedNanos / 1_000_000) < timeMillis;
    }

    public long getLastAttackMillis() {
        return lastAttack / 1_000_000;
    }

    public long getLastAttackNanos() {
        return lastAttack;
    }

    public boolean attack() {
        return attack;
    }

    public boolean swing() {
        return swing;
    }

    public boolean interact() {
        return interact;
    }

    public boolean startSprint() {
        return startSprint;
    }

    public boolean stopSprint() {
        return stopSprint;
    }

    public Player getPTarget() {
        return pTarget;
    }

    public boolean inCombat() {
        if (lastAttack == -1) return false;
        long elapsedMillis = (System.nanoTime() - lastAttack) / 1_000_000;
        return elapsedMillis < combatTicks * 50L;
    }

    @Override
    public void onReload() {
        this.combatTicks = Config.getInt(getConfigPath() + ".combat-ticks", 60);
    }
}
