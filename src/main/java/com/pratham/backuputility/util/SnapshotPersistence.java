package com.pratham.backuputility.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pratham.backuputility.model.FileSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SnapshotPersistence {

    @Value("${app.snapshot-dir:${user.home}/.backup-utility/snapshots}")
    private String snapshotDir;

    @Value("${app.max-snapshot-age-days:30}")
    private int maxSnapshotAgeDays;

    private final ObjectMapper objectMapper;

    public SnapshotPersistence() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Save snapshots to disk
     */
    public void saveSnapshots(Map<String, FileSnapshot> snapshots) throws IOException {
        Path snapshotPath = getSnapshotPath();
        Files.createDirectories(snapshotPath.getParent());

        // Clean old snapshots first
        cleanOldSnapshots();

        // Save current snapshots
        Path snapshotFile = snapshotPath.resolve("snapshots.json");
        objectMapper.writeValue(snapshotFile.toFile(), snapshots);

        // Create backup copy
        Path backupFile = snapshotPath.resolve("snapshots.backup.json");
        Files.copy(snapshotFile, backupFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Load snapshots from disk
     */
    public Map<String, FileSnapshot> loadSnapshots() {
        try {
            Path snapshotPath = getSnapshotPath();
            Path snapshotFile = snapshotPath.resolve("snapshots.json");

            if (!Files.exists(snapshotFile)) {
                // Try backup file
                Path backupFile = snapshotPath.resolve("snapshots.backup.json");
                if (Files.exists(backupFile)) {
                    snapshotFile = backupFile;
                } else {
                    return new ConcurrentHashMap<>();
                }
            }

            TypeReference<Map<String, FileSnapshot>> typeRef = new TypeReference<Map<String, FileSnapshot>>() {};
            Map<String, FileSnapshot> snapshots = objectMapper.readValue(snapshotFile.toFile(), typeRef);

            // Clean expired snapshots
            return cleanExpiredSnapshots(snapshots);

        } catch (IOException e) {
            System.err.println("Failed to load snapshots: " + e.getMessage());
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * Clean old snapshot files
     */
    private void cleanOldSnapshots() {
        try {
            Path snapshotPath = getSnapshotPath();
            if (!Files.exists(snapshotPath)) {
                return;
            }

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(maxSnapshotAgeDays);

            Files.list(snapshotPath)
                .filter(file -> file.getFileName().toString().startsWith("snapshots_"))
                .filter(file -> {
                    try {
                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(file).toInstant(),
                            java.time.ZoneId.systemDefault()
                        );
                        return fileTime.isBefore(cutoffDate);
                    } catch (IOException e) {
                        return true; // Delete if we can't read the time
                    }
                })
                .forEach(file -> {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        System.err.println("Failed to delete old snapshot: " + file);
                    }
                });

        } catch (IOException e) {
            System.err.println("Failed to clean old snapshots: " + e.getMessage());
        }
    }

    /**
     * Clean expired snapshots from the map
     */
    private Map<String, FileSnapshot> cleanExpiredSnapshots(Map<String, FileSnapshot> snapshots) {
        if (snapshots == null) {
            return new ConcurrentHashMap<>();
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(maxSnapshotAgeDays);
        Map<String, FileSnapshot> cleanedSnapshots = new ConcurrentHashMap<>();

        for (Map.Entry<String, FileSnapshot> entry : snapshots.entrySet()) {
            FileSnapshot snapshot = entry.getValue();
            if (snapshot.getSnapshotTime() != null && snapshot.getSnapshotTime().isAfter(cutoffDate)) {
                cleanedSnapshots.put(entry.getKey(), snapshot);
            }
        }

        return cleanedSnapshots;
    }

    /**
     * Get snapshot directory path
     */
    private Path getSnapshotPath() {
        String expandedPath = snapshotDir.replace("${user.home}", System.getProperty("user.home"));
        return Paths.get(expandedPath);
    }

    /**
     * Create a timestamped backup of current snapshots
     */
    public void createTimestampedBackup(Map<String, FileSnapshot> snapshots) {
        try {
            Path snapshotPath = getSnapshotPath();
            Files.createDirectories(snapshotPath);

            String timestamp = LocalDateTime.now().toString().replace(":", "-");
            Path backupFile = snapshotPath.resolve("snapshots_" + timestamp + ".json");

            objectMapper.writeValue(backupFile.toFile(), snapshots);

        } catch (IOException e) {
            System.err.println("Failed to create timestamped backup: " + e.getMessage());
        }
    }

    /**
     * Get snapshot statistics
     */
    public Map<String, Object> getSnapshotStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            Path snapshotPath = getSnapshotPath();
            Map<String, FileSnapshot> snapshots = loadSnapshots();

            stats.put("totalSnapshots", snapshots.size());
            stats.put("snapshotDir", snapshotPath.toString());
            stats.put("maxAge", maxSnapshotAgeDays + " days");

            if (!snapshots.isEmpty()) {
                LocalDateTime oldest = snapshots.values().stream()
                    .map(FileSnapshot::getSnapshotTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

                LocalDateTime newest = snapshots.values().stream()
                    .map(FileSnapshot::getSnapshotTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

                stats.put("oldestSnapshot", oldest);
                stats.put("newestSnapshot", newest);
            }

            // Calculate disk usage
            if (Files.exists(snapshotPath)) {
                long totalSize = Files.walk(snapshotPath)
                    .filter(Files::isRegularFile)
                    .mapToLong(file -> {
                        try {
                            return Files.size(file);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();

                stats.put("diskUsageBytes", totalSize);
                stats.put("diskUsageMB", totalSize / (1024 * 1024));
            }

        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}
