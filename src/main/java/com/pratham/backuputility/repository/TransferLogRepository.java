package com.pratham.backuputility.repository;

import com.pratham.backuputility.entity.TransferLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransferLogRepository extends JpaRepository<TransferLog, Long> {
    
    Optional<TransferLog> findByFileName(String fileName);
    
    Page<TransferLog> findAll(Pageable pageable);

    @Query("SELECT t FROM TransferLog t WHERE t.transferredAt >= :since ORDER BY t.transferredAt DESC")
    List<TransferLog> findRecentLogs(@Param("since") LocalDateTime since, Pageable pageable);

    @Query(value = "SELECT * FROM transfer_logs ORDER BY transferred_at DESC LIMIT :limit", nativeQuery = true)
    List<TransferLog> findTopRecentLogs(@Param("limit") int limit);
    
    // Additional queries for enhanced functionality
    
    @Query("SELECT t FROM TransferLog t WHERE t.status = :status ORDER BY t.transferredAt DESC")
    List<TransferLog> findByStatus(@Param("status") String status);
    
    @Query("SELECT t FROM TransferLog t WHERE t.operationType = :operationType ORDER BY t.transferredAt DESC")
    List<TransferLog> findByOperationType(@Param("operationType") String operationType);
    
    @Query("SELECT t FROM TransferLog t WHERE t.transferredAt >= :startTime AND t.transferredAt <= :endTime ORDER BY t.transferredAt DESC")
    List<TransferLog> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT COUNT(t) FROM TransferLog t WHERE t.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT SUM(t.bytesTransferred) FROM TransferLog t WHERE t.status = 'SUCCESS'")
    Long getTotalBytesTransferred();
    
    @Query("SELECT AVG(t.transferDurationMs) FROM TransferLog t WHERE t.status = 'SUCCESS' AND t.transferDurationMs IS NOT NULL")
    Double getAverageTransferDuration();
    
    @Query("SELECT t FROM TransferLog t WHERE t.fileName LIKE %:fileNamePattern% ORDER BY t.transferredAt DESC")
    List<TransferLog> findByFileNamePattern(@Param("fileNamePattern") String fileNamePattern);
    
    // Clean up old logs
    @Query("DELETE FROM TransferLog t WHERE t.transferredAt < :cutoffTime")
    void deleteOldLogs(@Param("cutoffTime") LocalDateTime cutoffTime);
}
