package com.pratham.backuputility.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a snapshot of a file with metadata and block-level hashes
 * for incremental backup and synchronization.
 */
public class FileSnapshot {
    private String filePath;
    private long size;
    private LocalDateTime lastModified;
    private String fileHash;
    private List<String> blockHashes;
    private LocalDateTime snapshotTime;

    public FileSnapshot() {}

    public FileSnapshot(String filePath, long size, LocalDateTime lastModified,
                       String fileHash, List<String> blockHashes) {
        this.filePath = filePath;
        this.size = size;
        this.lastModified = lastModified;
        this.fileHash = fileHash;
        this.blockHashes = blockHashes;
        this.snapshotTime = LocalDateTime.now();
    }

    // Getters and setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public List<String> getBlockHashes() { return blockHashes; }
    public void setBlockHashes(List<String> blockHashes) { this.blockHashes = blockHashes; }

    public LocalDateTime getSnapshotTime() { return snapshotTime; }
    public void setSnapshotTime(LocalDateTime snapshotTime) { this.snapshotTime = snapshotTime; }

    /**
     * Check if this snapshot indicates the file has changed compared to another snapshot
     */
    public boolean hasChangedFrom(FileSnapshot other) {
        if (other == null) {
            return true;
        }

        // Quick checks first
        if (this.size != other.size || !this.lastModified.equals(other.lastModified)) {
            return true;
        }

        // Hash comparison for definitive answer
        return !this.fileHash.equals(other.fileHash);
    }
}
