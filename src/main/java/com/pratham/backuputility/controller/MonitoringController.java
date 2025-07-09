package com.pratham.backuputility.controller;

import com.pratham.backuputility.entity.FileSnapshotEntity;
import com.pratham.backuputility.entity.TransferLog;
import com.pratham.backuputility.service.SQLiteSnapshotService;
import com.pratham.backuputility.service.TransferLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for monitoring snapshots and transfer logs
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    @Autowired
    private SQLiteSnapshotService snapshotService;

    @Autowired
    private TransferLogService transferLogService;

    /**
     * Get snapshot statistics
     */
    @GetMapping("/snapshots/statistics")
    public ResponseEntity<SQLiteSnapshotService.SnapshotStatistics> getSnapshotStatistics() {
        SQLiteSnapshotService.SnapshotStatistics stats = snapshotService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get transfer statistics
     */
    @GetMapping("/transfers/statistics")
    public ResponseEntity<TransferLogService.TransferStatistics> getTransferStatistics() {
        TransferLogService.TransferStatistics stats = transferLogService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get recent snapshots for a file
     */
    @GetMapping("/snapshots/{filePath}")
    public ResponseEntity<List<FileSnapshotEntity>> getSnapshotsForFile(@PathVariable String filePath) {
        List<FileSnapshotEntity> snapshots = snapshotService.getSnapshotsForFile(filePath);
        return ResponseEntity.ok(snapshots);
    }

    /**
     * Get latest snapshot for a file
     */
    @GetMapping("/snapshots/{filePath}/latest")
    public ResponseEntity<FileSnapshotEntity> getLatestSnapshot(@PathVariable String filePath) {
        Optional<FileSnapshotEntity> snapshot = snapshotService.getLatestSnapshot(filePath);
        return snapshot.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get recent transfer logs
     */
    @GetMapping("/transfers/recent")
    public ResponseEntity<List<TransferLog>> getRecentTransferLogs(@RequestParam(defaultValue = "100") int limit) {
        List<TransferLog> logs = transferLogService.getRecentLogs(limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get transfer logs by status
     */
    @GetMapping("/transfers/status/{status}")
    public ResponseEntity<List<TransferLog>> getTransferLogsByStatus(@PathVariable String status) {
        List<TransferLog> logs = transferLogService.getLogsByStatus(status);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get transfer logs by operation type
     */
    @GetMapping("/transfers/operation/{operationType}")
    public ResponseEntity<List<TransferLog>> getTransferLogsByOperationType(@PathVariable String operationType) {
        List<TransferLog> logs = transferLogService.getLogsByOperationType(operationType);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get transfer logs within a time range
     */
    @GetMapping("/transfers/range")
    public ResponseEntity<List<TransferLog>> getTransferLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<TransferLog> logs = transferLogService.getLogsByTimeRange(startTime, endTime);
        return ResponseEntity.ok(logs);
    }

    /**
     * Search transfer logs by file name pattern
     */
    @GetMapping("/transfers/search")
    public ResponseEntity<List<TransferLog>> searchTransferLogs(@RequestParam String fileNamePattern) {
        List<TransferLog> logs = transferLogService.searchLogsByFileName(fileNamePattern);
        return ResponseEntity.ok(logs);
    }

    /**
     * Trigger cleanup of old snapshots
     */
    @PostMapping("/snapshots/cleanup")
    public ResponseEntity<String> cleanupOldSnapshots() {
        try {
            snapshotService.cleanupOldSnapshots();
            return ResponseEntity.ok("Snapshot cleanup completed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Snapshot cleanup failed: " + e.getMessage());
        }
    }

    /**
     * Trigger cleanup of old transfer logs
     */
    @PostMapping("/transfers/cleanup")
    public ResponseEntity<String> cleanupOldTransferLogs() {
        try {
            transferLogService.cleanupOldLogs();
            return ResponseEntity.ok("Transfer log cleanup completed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Transfer log cleanup failed: " + e.getMessage());
        }
    }
}
