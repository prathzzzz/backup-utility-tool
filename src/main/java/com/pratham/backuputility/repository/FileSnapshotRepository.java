package com.pratham.backuputility.repository;

import com.pratham.backuputility.entity.FileSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing file snapshots in SQLite
 */
public interface FileSnapshotRepository extends JpaRepository<FileSnapshotEntity, Long> {
    
    /**
     * Find the most recent snapshot for a file
     */
    Optional<FileSnapshotEntity> findTopByFilePathOrderBySnapshotTimeDesc(String filePath);
    
    /**
     * Find all snapshots for a file
     */
    List<FileSnapshotEntity> findByFilePathOrderBySnapshotTimeDesc(String filePath);
    
    /**
     * Find snapshots older than specified date for cleanup
     */
    @Query("SELECT fs FROM FileSnapshotEntity fs WHERE fs.snapshotTime < :cutoffTime")
    List<FileSnapshotEntity> findSnapshotsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find all snapshots taken within a time range
     */
    @Query("SELECT fs FROM FileSnapshotEntity fs WHERE fs.snapshotTime >= :startTime AND fs.snapshotTime <= :endTime ORDER BY fs.snapshotTime DESC")
    List<FileSnapshotEntity> findSnapshotsInTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * Count snapshots for a file
     */
    long countByFilePath(String filePath);
    
    /**
     * Delete old snapshots for a file, keeping only the most recent N
     */
    @Query("DELETE FROM FileSnapshotEntity fs WHERE fs.filePath = :filePath AND fs.id NOT IN (SELECT f.id FROM FileSnapshotEntity f WHERE f.filePath = :filePath ORDER BY f.snapshotTime DESC LIMIT :keepCount)")
    void deleteOldSnapshots(@Param("filePath") String filePath, @Param("keepCount") int keepCount);
    
    /**
     * Get distinct file paths that have snapshots
     */
    @Query("SELECT DISTINCT fs.filePath FROM FileSnapshotEntity fs")
    List<String> findDistinctFilePaths();
}
