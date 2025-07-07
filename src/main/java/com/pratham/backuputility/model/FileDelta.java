package com.pratham.backuputility.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the delta information for incremental transfer of a file.
 * Contains all the block changes needed to update a target file.
 */
public class FileDelta {
    private String filePath;
    private List<BlockDelta> blockDeltas;
    private long totalBlocks;
    private long changedBlocks;
    private long sourceFileSize;
    private long sourceLastModified;

    public FileDelta() {
        this.blockDeltas = new ArrayList<>();
    }

    public FileDelta(String filePath) {
        this.filePath = filePath;
        this.blockDeltas = new ArrayList<>();
    }

    // Getters and setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public List<BlockDelta> getBlockDeltas() { return blockDeltas; }
    public void setBlockDeltas(List<BlockDelta> blockDeltas) { this.blockDeltas = blockDeltas; }

    public long getTotalBlocks() { return totalBlocks; }
    public void setTotalBlocks(long totalBlocks) { this.totalBlocks = totalBlocks; }

    public long getChangedBlocks() { return changedBlocks; }
    public void setChangedBlocks(long changedBlocks) { this.changedBlocks = changedBlocks; }

    public long getSourceFileSize() { return sourceFileSize; }
    public void setSourceFileSize(long sourceFileSize) { this.sourceFileSize = sourceFileSize; }

    public long getSourceLastModified() { return sourceLastModified; }
    public void setSourceLastModified(long sourceLastModified) { this.sourceLastModified = sourceLastModified; }

    /**
     * Add a block delta to this file delta
     */
    public void addBlockDelta(BlockDelta delta) {
        blockDeltas.add(delta);
    }

    /**
     * Check if this delta has any changes
     */
    public boolean hasChanges() {
        return !blockDeltas.isEmpty();
    }

    /**
     * Check if this represents a new file (target file doesn't exist)
     */
    public boolean isNewFile() {
        return totalBlocks == changedBlocks && hasChanges();
    }

    /**
     * Get the efficiency of this delta (percentage of blocks that changed)
     */
    public double getEfficiencyPercentage() {
        if (totalBlocks == 0) return 0.0;
        return (double) changedBlocks / totalBlocks * 100.0;
    }
}
