package com.pratham.backuputility.repository;

import com.pratham.backuputility.entity.BlockHashEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for managing block hashes in SQLite
 */
public interface BlockHashRepository extends JpaRepository<BlockHashEntity, Long> {
    
    /**
     * Find all block hashes for a file snapshot, ordered by block index
     */
    List<BlockHashEntity> findByFileSnapshotIdOrderByBlockIndex(Long fileSnapshotId);
    
    /**
     * Find a specific block hash by snapshot ID and block index
     */
    BlockHashEntity findByFileSnapshotIdAndBlockIndex(Long fileSnapshotId, int blockIndex);
    
    /**
     * Delete all block hashes for a file snapshot
     */
    void deleteByFileSnapshotId(Long fileSnapshotId);
    
    /**
     * Get count of blocks for a file snapshot
     */
    long countByFileSnapshotId(Long fileSnapshotId);
    
    /**
     * Find block hashes by hash value (for deduplication)
     */
    @Query("SELECT bh FROM BlockHashEntity bh WHERE bh.hash = :hash")
    List<BlockHashEntity> findByHash(@Param("hash") String hash);
}
