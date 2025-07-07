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
}
