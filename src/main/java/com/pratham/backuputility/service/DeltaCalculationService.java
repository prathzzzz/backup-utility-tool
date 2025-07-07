package com.pratham.backuputility.service;

import com.pratham.backuputility.model.BlockDelta;
import com.pratham.backuputility.model.FileDelta;
import com.pratham.backuputility.model.FileSnapshot;
import com.pratham.backuputility.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Service responsible for calculating file deltas for incremental transfers
 * Follows Single Responsibility Principle
 */
@Service
public class DeltaCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(DeltaCalculationService.class);

    @Value("${app.block-size:4096}")
    private int blockSize;

    /**
     * Calculate delta between source file and existing snapshot
     */
    public FileDelta calculateDelta(Path sourceFile, FileSnapshot oldSnapshot, String relativePath) {
        try {
            FileDelta delta = new FileDelta(relativePath);
            long sourceFileSize = Files.size(sourceFile);
            long sourceLastModified = Files.getLastModifiedTime(sourceFile).toMillis();

            try (InputStream sourceStream = Files.newInputStream(sourceFile)) {
                byte[] buffer = new byte[blockSize];
                int bytesRead;
                long blockIndex = 0;

                while ((bytesRead = sourceStream.read(buffer)) != -1) {
                    String blockHash = HashUtil.calculateSha256Hash(buffer, bytesRead);

                    // Check if this block needs to be transferred
                    boolean blockChanged = true;
                    if (oldSnapshot != null && blockIndex < oldSnapshot.getBlockHashes().size()) {
                        blockChanged = !blockHash.equals(oldSnapshot.getBlockHashes().get((int) blockIndex));
                    }

                    if (blockChanged) {
                        byte[] blockData = Arrays.copyOf(buffer, bytesRead);
                        delta.addBlockDelta(new BlockDelta(blockIndex, blockData, blockHash));
                    }

                    blockIndex++;
                }

                delta.setTotalBlocks(blockIndex);
                delta.setChangedBlocks(delta.getBlockDeltas().size());
                delta.setSourceFileSize(sourceFileSize);
                delta.setSourceLastModified(sourceLastModified);
            }

            logger.debug("Calculated delta for {}: {}/{} blocks changed ({}% efficiency)",
                relativePath, delta.getChangedBlocks(), delta.getTotalBlocks(),
                String.format("%.1f", delta.getEfficiencyPercentage()));

            return delta;

        } catch (Exception e) {
            logger.error("Failed to calculate delta for file: {}", sourceFile, e);
            throw new RuntimeException("Failed to calculate file delta", e);
        }
    }
}
