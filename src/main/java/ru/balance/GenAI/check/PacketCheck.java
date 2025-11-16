package ru.balance.GenAI.check;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public interface PacketCheck {
    default void onPacketReceive(PacketReceiveEvent event) {
    }

    default void onPacketSend(PacketSendEvent event) {
    }

    default void onReload() {
    }
}

