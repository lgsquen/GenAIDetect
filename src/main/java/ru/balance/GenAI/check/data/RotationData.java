package ru.balance.GenAI.check.data;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.stats.Statistics;
import ru.balance.GenAI.check.util.GraphUtil;
import ru.balance.GenAI.check.util.PacketUtil;

import java.util.*;

public class RotationData extends Check implements PacketCheck {

    public float yaw;
    public float pitch;
    public float lastYaw;
    public float lastPitch;
    public float deltaYaw;
    public float deltaPitch;
    public float lastDeltaYaw;
    public float lastDeltaPitch;
    public float lastLastDeltaYaw;
    public float lastLastDeltaPitch;

    private long lastSmooth = 0L;
    private long lastHighRate = 0L;
    private double lastDeltaXRot = 0.0;
    private double lastDeltaYRot = 0.0;
    private final List<Double> yawSamples = new ArrayList<>();
    private final List<Double> pitchSamples = new ArrayList<>();
    private boolean cinematicRotation = false;
    private int isTotallyNotCinematic = 0;

    public RotationData(APlayer aPlayer) {
        super("RotationData", aPlayer);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (PacketUtil.isRotation(event)) {
            WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
            updateRotation(wrapper.getYaw(), wrapper.getPitch());
        }
    }

    private void updateRotation(float newYaw, float newPitch) {
        lastYaw = yaw;
        lastPitch = pitch;
        yaw = newYaw;
        pitch = newPitch;

        float newDeltaYaw = yaw - lastYaw;
        float newDeltaPitch = pitch - lastPitch;

        lastLastDeltaYaw = lastDeltaYaw;
        lastLastDeltaPitch = lastDeltaPitch;

        lastDeltaYaw = deltaYaw;
        lastDeltaPitch = deltaPitch;

        deltaYaw = newDeltaYaw;
        deltaPitch = newDeltaPitch;

        processCinematic();
    }

    private void processCinematic() {
        long now = System.currentTimeMillis();

        double differenceYaw = Math.abs(deltaYaw - lastDeltaXRot);
        double differencePitch = Math.abs(deltaPitch - lastDeltaYRot);

        double joltYaw = Math.abs(differenceYaw - deltaYaw);
        double joltPitch = Math.abs(differencePitch - deltaPitch);

        boolean cinematic = (now - lastHighRate > 250L) || (now - lastSmooth < 9000L);

        if (joltYaw > 1.0 && joltPitch > 1.0) {
            lastHighRate = now;
        }

        yawSamples.add((double) deltaYaw);
        pitchSamples.add((double) deltaPitch);

        if (yawSamples.size() >= 20 && pitchSamples.size() >= 20) {
            Set<Double> shannonYaw = new HashSet<>();
            Set<Double> shannonPitch = new HashSet<>();
            List<Double> stackYaw = new ArrayList<>();
            List<Double> stackPitch = new ArrayList<>();

            for (Double yawSample : yawSamples) {
                stackYaw.add(yawSample);
                if (stackYaw.size() >= 10) {
                    shannonYaw.add(Statistics.getShannonEntropy(stackYaw));
                    stackYaw.clear();
                }
            }

            for (Double pitchSample : pitchSamples) {
                stackPitch.add(pitchSample);
                if (stackPitch.size() >= 10) {
                    shannonPitch.add(Statistics.getShannonEntropy(stackPitch));
                    stackPitch.clear();
                }
            }

            if (shannonYaw.size() != 1 || shannonPitch.size() != 1 ||
                    !shannonYaw.toArray()[0].equals(shannonPitch.toArray()[0])) {
                isTotallyNotCinematic = 20;
            }

            GraphUtil.GraphResult resultsYaw = GraphUtil.getGraph(yawSamples);
            GraphUtil.GraphResult resultsPitch = GraphUtil.getGraph(pitchSamples);

            int negativesYaw = resultsYaw.getNegatives();
            int negativesPitch = resultsPitch.getNegatives();
            int positivesYaw = resultsYaw.getPositives();
            int positivesPitch = resultsPitch.getPositives();

            if (positivesYaw > negativesYaw || positivesPitch > negativesPitch) {
                lastSmooth = now;
            }

            yawSamples.clear();
            pitchSamples.clear();
        }

        if (isTotallyNotCinematic > 0) {
            isTotallyNotCinematic--;
            cinematicRotation = false;
        } else {
            cinematicRotation = cinematic;
        }

        lastDeltaXRot = deltaYaw;
        lastDeltaYRot = deltaPitch;
    }

    public boolean isCinematicRotation() {
        return cinematicRotation;
    }
}

