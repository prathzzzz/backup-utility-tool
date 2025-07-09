package com.pratham.backuputility.service;

import com.pratham.backuputility.entity.BlockHashEntity;
import com.pratham.backuputility.entity.FileSnapshotEntity;
import com.pratham.backuputility.model.FileSnapshot;
import com.pratham.backuputility.repository.BlockHashRepository;
import com.pratham.backuputility.repository.FileSnapshotRepository;
import com.pratham.backuputility.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Enhanced service for managing file snapshots with SQLite persistence
 */
@Service
@Transactional
public class SQLiteSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteSnapshotService.class);

    @Value("${app.block-size:4096}")
    private int blockSize;

    @Value("${app.max-snapshot-age-days:30}")
    private int maxSnapshotAgeDays;

    @Autowired
    private FileSnapshotRepository fileSnapshotRepository;

    @Autowired
    private BlockHashRepository blockHashRepository;

    /**
     * Create and persist a snapshot of a file with block-level hashes
     */
    public FileSnapshotEntity createAndSaveSnapshot(Path filePath, String relativePath) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            long fileSize = attrs.size();
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());

            List<String> blockHashes = new ArrayList<>();
            MessageDigest fileDigest = HashUtil.createSha256Digest();

            try (InputStream fis = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[blockSize];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    // Hash individual block
                    String blockHash = HashUtil.calculateSha256Hash(buffer, bytesRead);
                    blockHashes.add(blockHash);

                    // Update file hash
                    fileDigest.update(buffer, 0, bytesRead);
                }
            }

            String fileHash = HashUtil.bytesToHex(fileDigest.digest());

            // Create and save the snapshot entity
            FileSnapshotEntity snapshotEntity = new FileSnapshotEntity(relativePath, fileSize, lastModified, fileHash);
            snapshotEntity = fileSnapshotRepository.save(snapshotEntity);

            // Save block hashes
            for (int i = 0; i < blockHashes.size(); i++) {
                BlockHashEntity blockHashEntity = new BlockHashEntity(snapshotEntity, i, blockHashes.get(i));
                blockHashRepository.save(blockHashEntity);
            }

            logger.debug("Created and saved snapshot for {}: {} blocks, {} bytes", relativePath, blockHashes.size(), fileSize);
            return snapshotEntity;

        } catch (Exception e) {
            logger.error("Failed to create snapshot for file: {}", filePath, e);
            throw new RuntimeException("Failed to create snapshot", e);
        }
    }

    /**
     * Get the most recent snapshot for a file
     */
    public Optional<FileSnapshotEntity> getLatestSnapshot(String filePath) {
        return fileSnapshotRepository.findTopByFilePathOrderBySnapshotTimeDesc(filePath);
    }

    /**
     * Get all snapshots for a file
     */
    public List<FileSnapshotEntity> getSnapshotsForFile(String filePath) {
        return fileSnapshotRepository.findByFilePathOrderBySnapshotTimeDesc(filePath);
    }

    /**
     * Get block hashes for a snapshot
     */
    public List<String> getBlockHashes(FileSnapshotEntity snapshot) {
        return blockHashRepository.findByFileSnapshotIdOrderByBlockIndex(snapshot.getId())
                .stream()
                .map(BlockHashEntity::getHash)
                .collect(Collectors.toList());
    }

    /**
     * Convert FileSnapshotEntity to FileSnapshot model
     */
    public FileSnapshot convertToModel(FileSnapshotEntity entity) {
        List<String> blockHashes = getBlockHashes(entity);
        FileSnapshot snapshot = new FileSnapshot(
                entity.getFilePath(),
                entity.getSize(),
                entity.getLastModified(),
                entity.getFileHash(),
                blockHashes
        );
        snapshot.setSnapshotTime(entity.getSnapshotTime());
        return snapshot;
    }

    /**
     * Check if a file needs to be transferred based on snapshot comparison
     */
    public boolean needsTransfer(Path filePath, String relativePath) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            long fileSize = attrs.size();
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());

            Optional<FileSnapshotEntity> latestSnapshot = getLatestSnapshot(relativePath);
            
            if (latestSnapshot.isEmpty()) {
                logger.debug("File {} needs transfer: no previous snapshot", relativePath);
                return true;
            }

            FileSnapshotEntity snapshot = latestSnapshot.get();
            
            // Quick checks first
            if (fileSize != snapshot.getSize() || !lastModified.equals(snapshot.getLastModified())) {
                logger.debug("File {} needs transfer: size or modification time changed", relativePath);
                return true;
            }

            // If size and modification time match, assume no change
            logger.debug("File {} does not need transfer: size and modification time unchanged", relativePath);
            return false;

        } catch (Exception e) {
            logger.error("Failed to check if file needs transfer: {}", filePath, e);
            // If we can't determine, assume it needs transfer
            return true;
        }
    }

    /**
     * Clean up old snapshots
     */
    public void cleanupOldSnapshots() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(maxSnapshotAgeDays);
            List<FileSnapshotEntity> oldSnapshots = fileSnapshotRepository.findSnapshotsOlderThan(cutoffTime);
            
            logger.info("Cleaning up {} old snapshots older than {}", oldSnapshots.size(), cutoffTime);
            
            for (FileSnapshotEntity snapshot : oldSnapshots) {
                // Delete associated block hashes first
                blockHashRepository.deleteByFileSnapshotId(snapshot.getId());
                // Delete the snapshot
                fileSnapshotRepository.delete(snapshot);
            }
            
            logger.info("Cleanup completed");
        } catch (Exception e) {
            logger.error("Failed to cleanup old snapshots", e);
        }
    }

    /**
     * Get statistics about snapshots
     */
    public SnapshotStatistics getStatistics() {
        try {
            long totalSnapshots = fileSnapshotRepository.count();
            long totalBlockHashes = blockHashRepository.count();
            List<String> distinctFilePaths = fileSnapshotRepository.findDistinctFilePaths();
            
            return new SnapshotStatistics(totalSnapshots, totalBlockHashes, distinctFilePaths.size());
        } catch (Exception e) {
            logger.error("Failed to get snapshot statistics", e);
            return new SnapshotStatistics(0, 0, 0);
        }
    }

    /**
     * Simple statistics class
     */
    public static class SnapshotStatistics {
        private final long totalSnapshots;
        private final long totalBlockHashes;
        private final int uniqueFiles;

        public SnapshotStatistics(long totalSnapshots, long totalBlockHashes, int uniqueFiles) {
            this.totalSnapshots = totalSnapshots;
            this.totalBlockHashes = totalBlockHashes;
            this.uniqueFiles = uniqueFiles;
        }

        public long getTotalSnapshots() { return totalSnapshots; }
        public long getTotalBlockHashes() { return totalBlockHashes; }
        public int getUniqueFiles() { return uniqueFiles; }
    }
}
