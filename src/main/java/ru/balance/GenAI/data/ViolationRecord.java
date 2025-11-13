package ru.balance.GenAI.data;

import lombok.Value;

@Value
public class ViolationRecord {
    String playerName;
    double probability;
    long timestamp;
}