package ru.balance.GenAI.service;

import org.bukkit.Bukkit;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.data.PlayerStats;
import ru.balance.GenAI.data.ViolationRecord;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class DatabaseService {

    private final GenAI plugin;
    private String jdbcUrl;

    public DatabaseService(GenAI plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File dbFile = new File(plugin.getDataFolder(), "genai.db");
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }

        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        createTables();
    }

    private Connection openConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(jdbcUrl);
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS violations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "player_name VARCHAR(16) NOT NULL," +
                "probability DOUBLE NOT NULL," +
                "timestamp BIGINT NOT NULL);";

        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException ignored) {}
    }

    public void logViolationAsync(UUID uuid, String name, double probability) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO violations (player_uuid, player_name, probability, timestamp) VALUES (?, ?, ?, ?);";
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setDouble(3, probability);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        });
    }

    public void getPlayerHistoryAsync(UUID uuid, Consumer<List<ViolationRecord>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ViolationRecord> history = new ArrayList<>();
            String sql = "SELECT player_name, probability, timestamp FROM violations WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT 50;";
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    history.add(new ViolationRecord(rs.getString("player_name"), rs.getDouble("probability"), rs.getLong("timestamp")));
                }
            } catch (SQLException ignored) {}
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(history));
        });
    }

    public void getPlayerStatsAsync(UUID uuid, Consumer<PlayerStats> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT COUNT(*), AVG(probability), MAX(timestamp) FROM violations WHERE player_uuid = ?;";
            PlayerStats stats = new PlayerStats(0, 0, 0);
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats = new PlayerStats(rs.getInt(1), rs.getDouble(2), rs.getLong(3));
                }
            } catch (SQLException ignored) {}
            final PlayerStats finalStats = stats;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalStats));
        });
    }

    public void countRecentViolationsAsync(UUID uuid, long timeWindowMillis, Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int count = 0;
            long sinceTimestamp = System.currentTimeMillis() - timeWindowMillis;
            String sql = "SELECT COUNT(*) FROM violations WHERE player_uuid = ? AND timestamp > ?;";
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, sinceTimestamp);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            } catch (SQLException ignored) {}
            final int finalCount = count;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalCount));
        });
    }

    public void clearRecentViolationsAsync(UUID uuid, long timeWindowMillis) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long sinceTimestamp = System.currentTimeMillis() - timeWindowMillis;
            String sql = "DELETE FROM violations WHERE player_uuid = ? AND timestamp > ?;";
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, sinceTimestamp);
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        });
    }

    public void close() {
    }
}
