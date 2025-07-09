package com.pratham.backuputility.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity to persist file snapshots in SQLite database
 */
@Entity
@Table(name = "file_snapshots", indexes = {
    @Index(name = "idx_file_path", columnList = "filePath"),
    @Index(name = "idx_snapshot_time", columnList = "snapshotTime"),
    @Index(name = "idx_file_hash", columnList = "fileHash")
})
public class FileSnapshotEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_path", length = 1000, nullable = false)
    private String filePath;
    
    @Column(name = "file_size", nullable = false)
    private long size;
    
    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;
    
    @Column(name = "file_hash", length = 64, nullable = false)
    private String fileHash;
    
    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;
    
    @OneToMany(mappedBy = "fileSnapshot", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<BlockHashEntity> blockHashes = new ArrayList<>();
    
    // Constructors
    public FileSnapshotEntity() {}
    
    public FileSnapshotEntity(String filePath, long size, LocalDateTime lastModified, String fileHash) {
        this.filePath = filePath;
        this.size = size;
        this.lastModified = lastModified;
        this.fileHash = fileHash;
        this.snapshotTime = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    
    public LocalDateTime getSnapshotTime() { return snapshotTime; }
    public void setSnapshotTime(LocalDateTime snapshotTime) { this.snapshotTime = snapshotTime; }
    
    public List<BlockHashEntity> getBlockHashes() { return blockHashes; }
    public void setBlockHashes(List<BlockHashEntity> blockHashes) { this.blockHashes = blockHashes; }
    
    /**
     * Convenience method to add a block hash
     */
    public void addBlockHash(int blockIndex, String hash) {
        BlockHashEntity blockHash = new BlockHashEntity(this, blockIndex, hash);
        blockHashes.add(blockHash);
    }
    
    /**
     * Check if this snapshot indicates the file has changed compared to another snapshot
     */
    public boolean hasChangedFrom(FileSnapshotEntity other) {
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
