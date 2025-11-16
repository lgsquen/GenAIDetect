package ru.balance.GenAI.check.util;

import java.util.Collection;
import java.util.List;

public class MathUtil {
    public static double correlation(List<Double> actual, List<Double> predicted) {
        int n = actual.size();
        if (n == 0 || predicted.size() != n) return 0.0;

        double meanActual = actual.stream().mapToDouble(d -> d).average().orElse(0.0);
        double meanPredicted = predicted.stream().mapToDouble(d -> d).average().orElse(0.0);

        double numerator = 0.0;
        double denominatorActual = 0.0;
        double denominatorPredicted = 0.0;

        for (int i = 0; i < n; i++) {
            double aDiff = actual.get(i) - meanActual;
            double pDiff = predicted.get(i) - meanPredicted;
            numerator += aDiff * pDiff;
            denominatorActual += aDiff * aDiff;
            denominatorPredicted += pDiff * pDiff;
        }

        double denominator = java.lang.Math.sqrt(denominatorActual * denominatorPredicted);
        if (denominator == 0) return 0.0;

        return numerator / denominator;
    }

    public static double jerk(List<Double> values) {
        if (values.size() < 4) return 0.0;

        double jerk = 0.0;
        for (int i = 3; i < values.size(); i++) {
            double j = values.get(i) - 3 * values.get(i - 1) + 3 * values.get(i - 2) - values.get(i - 3);
            jerk += Math.abs(j);
        }

        return jerk;
    }

    public static double getMean(final Collection<? extends Number> data) {
        if (data == null || data.isEmpty()) return 0.0;

        double sum = 0.0;
        int count = 0;

        for (Number number : data) {
            sum += number.doubleValue();
            count++;
        }

        return count == 0 ? 0.0 : sum / count;
    }

    public static double autoCorrelation(List<Double> data, int lag) {
        int n = data.size();
        if (n <= lag) return 0;

        double mean = data.stream().mapToDouble(d -> d).average().orElse(0.0);

        double numerator = 0.0;
        double denominator = 0.0;

        for (int i = 0; i < n - lag; i++) {
            numerator += (data.get(i) - mean) * (data.get(i + lag) - mean);
        }
        for (int i = 0; i < n; i++) {
            denominator += Math.pow(data.get(i) - mean, 2);
        }

        return denominator == 0 ? 0 : numerator / denominator;
    }

    public static double getKurtosis(List<Double> values) {
        int n = values.size();

        double mean = 0;
        for (double v : values) {
            mean += v;
        }
        mean /= n;

        double sum2 = 0;
        for (double v : values) {
            sum2 += Math.pow(v - mean, 2);
        }
        double variance = sum2 / (n - 1);
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0) {
            return 0;
        }

        double sum4 = 0;
        for (double v : values) {
            sum4 += Math.pow(v - mean, 4);
        }
        double m4 = sum4 / n;

        double kurtosis = m4 / Math.pow(stdDev, 4) - 3;

        return kurtosis;
    }

    public static double r2Linearity(List<Double> deltas) {
        int n = deltas.size();
        if (n < 2) return 1.0;

        double sumX = 0;
        double sumY = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += deltas.get(i);
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        double ssXX = 0;
        double ssYY = 0;
        double ssXY = 0;
        for (int i = 0; i < n; i++) {
            double dx = i - meanX;
            double dy = deltas.get(i) - meanY;
            ssXX += dx * dx;
            ssYY += dy * dy;
            ssXY += dx * dy;
        }
        if (ssXX == 0 || ssYY == 0) return 1.0;

        double slope = ssXY / ssXX;
        double intercept = meanY - slope * meanX;

        double ssr = 0;
        for (int i = 0; i < n; i++) {
            double pred = slope * i + intercept;
            double resid = deltas.get(i) - pred;
            ssr += resid * resid;
        }

        double r2 = 1.0 - (ssr / ssYY);
        if (Double.isNaN(r2)) r2 = 1.0;
        return Math.max(0.0, Math.min(1.0, r2));
    }

    public static double averageDeltaChange(List<Double> deltas) {
        if (deltas.size() < 2) return 0;
        double sum = 0;
        for (int i = 1; i < deltas.size(); i++) {
            sum += Math.abs(deltas.get(i) - deltas.get(i - 1));
        }
        return sum / (deltas.size() - 1);
    }

    public static double cosineSimilarity(List<Double> deltas) {
        if (deltas.size() < 2) return 1.0;
        double sumCos = 0;
        int count = 0;
        for (int i = 1; i < deltas.size(); i++) {
            double prev = deltas.get(i - 1);
            double curr = deltas.get(i);
            if (prev == 0 && curr == 0) continue;
            double cos = (prev * curr) / (Math.sqrt(prev * prev) * Math.sqrt(curr * curr));
            sumCos += cos;
            count++;
        }
        return count == 0 ? 1.0 : sumCos / count;
    }

    public static boolean isNearlySame(double d1, double d2, double number) {
        return Math.abs(d1 - d2) < number;
    }

    public static double getGCD(double a, double b) {
        if (a == 0 || b == 0) return 0;
        a = Math.abs(a);
        b = Math.abs(b);
        while (b > 1.0E-9) {
            double temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static double mean(double[] arr) {
        if (arr == null || arr.length == 0) return 0.0;
        double sum = 0.0;
        for (double v : arr) {
            sum += v;
        }
        return sum / arr.length;
    }

    public static double symmetry(double[] arr) {
        if (arr == null || arr.length < 4) return 1.0;
        double mean = mean(arr);
        double sym = 0.0;
        int pairs = 0;
        for (int i = 0; i < arr.length / 2; i++) {
            double sum = arr[i] + arr[arr.length - 1 - i];
            sym += Math.abs(sum - 2 * mean);
            pairs++;
        }
        return pairs > 0 ? sym / pairs : 1.0;
    }

    public static double[] diff(List<Double> values) {
        if (values == null || values.size() < 2) {
            return new double[0];
        }
        double[] d = new double[values.size() - 1];
        for (int i = 1; i < values.size(); i++) {
            d[i - 1] = values.get(i) - values.get(i - 1);
        }
        return d;
    }
}

