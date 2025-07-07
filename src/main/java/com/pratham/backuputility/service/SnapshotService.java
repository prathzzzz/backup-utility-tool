package com.pratham.backuputility.service;

import com.pratham.backuputility.model.FileSnapshot;
import com.pratham.backuputility.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for creating and managing file snapshots
 * Follows Single Responsibility Principle
 */
@Service
public class SnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotService.class);

    @Value("${app.block-size:4096}")
    private int blockSize;

    /**
     * Create a snapshot of a file with block-level hashes
     */
    public FileSnapshot createSnapshot(Path filePath, String relativePath) {
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

            FileSnapshot snapshot = new FileSnapshot(relativePath, fileSize, lastModified, fileHash, blockHashes);
            logger.debug("Created snapshot for {}: {} blocks, {} bytes", relativePath, blockHashes.size(), fileSize);

            return snapshot;

        } catch (Exception e) {
            logger.error("Failed to create snapshot for file: {}", filePath, e);
            throw new RuntimeException("Failed to create snapshot", e);
        }
    }

    /**
     * Check if a file needs to be transferred based on snapshot comparison
     */
    public boolean needsTransfer(FileSnapshot newSnapshot, FileSnapshot oldSnapshot) {
        if (oldSnapshot == null) {
            logger.debug("File {} needs transfer: no previous snapshot", newSnapshot.getFilePath());
            return true;
        }

        boolean hasChanged = newSnapshot.hasChangedFrom(oldSnapshot);
        if (hasChanged) {
            logger.debug("File {} needs transfer: file has changed", newSnapshot.getFilePath());
        }

        return hasChanged;
    }
}
