package com.pratham.backuputility.entity;

import jakarta.persistence.*;

/**
 * Entity to store individual block hashes for file snapshots
 */
@Entity
@Table(name = "block_hashes", indexes = {
    @Index(name = "idx_block_snapshot", columnList = "file_snapshot_id, block_index"),
    @Index(name = "idx_block_hash", columnList = "hash")
})
public class BlockHashEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_snapshot_id", nullable = false)
    private FileSnapshotEntity fileSnapshot;
    
    @Column(name = "block_index", nullable = false)
    private int blockIndex;
    
    @Column(name = "hash", length = 64, nullable = false)
    private String hash;
    
    // Constructors
    public BlockHashEntity() {}
    
    public BlockHashEntity(FileSnapshotEntity fileSnapshot, int blockIndex, String hash) {
        this.fileSnapshot = fileSnapshot;
        this.blockIndex = blockIndex;
        this.hash = hash;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public FileSnapshotEntity getFileSnapshot() { return fileSnapshot; }
    public void setFileSnapshot(FileSnapshotEntity fileSnapshot) { this.fileSnapshot = fileSnapshot; }
    
    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int blockIndex) { this.blockIndex = blockIndex; }
    
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockHashEntity)) return false;
        BlockHashEntity that = (BlockHashEntity) o;
        return blockIndex == that.blockIndex && 
               fileSnapshot.getId().equals(that.fileSnapshot.getId());
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(fileSnapshot.getId(), blockIndex);
    }
}
