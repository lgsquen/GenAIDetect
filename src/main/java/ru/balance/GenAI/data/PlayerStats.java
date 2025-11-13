package ru.balance.GenAI.data;

import lombok.Value;

@Value
public class PlayerStats {
    int totalViolations;
    double averageProbability;
    long lastViolationTimestamp;
}