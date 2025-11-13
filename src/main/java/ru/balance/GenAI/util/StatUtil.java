package ru.balance.GenAI.util;

import java.util.List;

public final class StatUtil {
    public static double jerk(List<Float> data) {
        if (data.size() < 4) {
            return 0.0D;
        }
        double total = 0.0D;

        for (int i = 3; i < data.size(); i++) {
            total += Math.abs(
                    data.get(i)
                            - 3.0F * data.get(i - 1)
                            + 3.0F * data.get(i - 2)
                            - data.get(i - 3)
            );
        }
        return total / (data.size() - 3);
    }
}