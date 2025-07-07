package com.pratham.backuputility.service;

import com.pratham.backuputility.model.BlockDelta;
import com.pratham.backuputility.model.FileDelta;
import com.pratham.backuputility.util.FileSystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Service responsible for applying file deltas to target files
 * Follows Single Responsibility Principle
 */
@Service
public class DeltaApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(DeltaApplicationService.class);

    @Value("${app.block-size:4096}")
    private int blockSize;

    /**
     * Apply delta to target file
     */
    public void applyDelta(Path targetFile, FileDelta delta) {
        try {
            // Ensure target directory exists
            FileSystemUtil.ensureDirectoryExists(targetFile.getParent());

            if (!Files.exists(targetFile)) {
                createNewFile(targetFile, delta);
            } else {
                updateExistingFile(targetFile, delta);
            }

            // Preserve modification time from source file
            if (delta.getSourceLastModified() > 0) {
                Files.setLastModifiedTime(targetFile, FileTime.fromMillis(delta.getSourceLastModified()));
            }

            logger.debug("Applied delta to {}: {} block changes",
                targetFile, delta.getChangedBlocks());

        } catch (Exception e) {
            logger.error("Failed to apply delta to file: {}", targetFile, e);
            throw new RuntimeException("Failed to apply file delta", e);
        }
    }

    private void createNewFile(Path targetFile, FileDelta delta) throws Exception {
        if (delta.getBlockDeltas().isEmpty()) {
            // Empty file - just create it
            Files.createFile(targetFile);
            logger.debug("Created empty file: {}", targetFile);
        } else {
            // File with content
            try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                for (BlockDelta blockDelta : delta.getBlockDeltas()) {
                    fos.write(blockDelta.getData());
                }
            }
            logger.debug("Created new file: {} with {} blocks", targetFile, delta.getBlockDeltas().size());
        }
    }

    private void updateExistingFile(Path targetFile, FileDelta delta) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(targetFile.toFile(), "rw")) {
            // Apply block changes
            for (BlockDelta blockDelta : delta.getBlockDeltas()) {
                long position = blockDelta.getBlockIndex() * blockSize;
                raf.seek(position);
                raf.write(blockDelta.getData());
            }

            // Always truncate file to match source file size (including 0 for empty files)
            raf.setLength(delta.getSourceFileSize());
        }
        logger.debug("Updated existing file: {} (truncated to {} bytes)",
            targetFile, delta.getSourceFileSize());
    }
}
