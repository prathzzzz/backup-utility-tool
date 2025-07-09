package com.pratham.backuputility.service;

import com.pratham.backuputility.entity.TransferLog;
import com.pratham.backuputility.model.FileDelta;
import com.pratham.backuputility.model.FileSnapshot;
import com.pratham.backuputility.repository.TransferLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enhanced service for logging transfer operations with SQLite persistence
 */
@Service
@Transactional
public class TransferLogService {

    private static final Logger logger = LoggerFactory.getLogger(TransferLogService.class);

    @Autowired
    private TransferLogRepository repository;

    @Value("${app.max-log-retention-days:30}")
    private int maxLogRetentionDays;

    /**
     * Log a successful transfer operation
     */
    public void logTransfer(String fileName, FileSnapshot snapshot, FileDelta delta) {
        logTransfer(fileName, snapshot, delta, "INCREMENTAL");
    }

    /**
     * Log a successful transfer operation with operation type
     */
    public void logTransfer(String fileName, FileSnapshot snapshot, FileDelta delta, String operationType) {
        try {
            long startTime = System.currentTimeMillis();
            
            TransferLog log = new TransferLog(fileName, snapshot.getSize(), "SUCCESS", operationType);
            log.setLastModified(snapshot.getLastModified());
            log.setChecksum(snapshot.getFileHash());
            
            if (delta != null) {
                log.setErrorMessage(String.format("Incremental: %d/%d blocks updated (%.1f%% efficiency)",
                    delta.getChangedBlocks(), delta.getTotalBlocks(), delta.getEfficiencyPercentage()));
                log.setBytesTransferred((long) (delta.getChangedBlocks() * 4096)); // Approximate
                log.setCompressionRatio(delta.getEfficiencyPercentage() / 100.0);
            } else {
                log.setBytesTransferred(snapshot.getSize());
                log.setCompressionRatio(1.0);
            }
            
            long endTime = System.currentTimeMillis();
            log.setTransferDurationMs(endTime - startTime);

            repository.save(log);

        } catch (Exception e) {
            logger.error("Failed to log transfer for file: {}", fileName, e);
        }
    }

    /**
     * Log a full file transfer (no delta)
     */
    public void logFullTransfer(String fileName, long fileSize, String checksum) {
        try {
            TransferLog log = new TransferLog(fileName, fileSize, "SUCCESS", "FULL");
            log.setLastModified(LocalDateTime.now());
            log.setChecksum(checksum);
            log.setBytesTransferred(fileSize);
            log.setCompressionRatio(1.0);

            repository.save(log);

        } catch (Exception e) {
            logger.error("Failed to log full transfer for file: {}", fileName, e);
        }
    }

    /**
     * Log a failed transfer operation
     */
    public void logTransferError(String fileName, String errorMessage) {
        logTransferError(fileName, errorMessage, "UNKNOWN");
    }

    /**
     * Log a failed transfer operation with operation type
     */
    public void logTransferError(String fileName, String errorMessage, String operationType) {
        try {
            TransferLog log = new TransferLog(fileName, 0, "FAILED", operationType);
            log.setLastModified(LocalDateTime.now());
            log.setErrorMessage(errorMessage);

            repository.save(log);

        } catch (Exception e) {
            logger.error("Failed to log transfer error for file: {}", fileName, e);
        }
    }

    /**
     * Log a skipped file operation
     */
    public void logSkippedFile(String fileName, String reason) {
        try {
            TransferLog log = new TransferLog(fileName, 0, "SKIPPED", "SKIP");
            log.setLastModified(LocalDateTime.now());
            log.setErrorMessage(reason);

            repository.save(log);

        } catch (Exception e) {
            logger.error("Failed to log skipped file: {}", fileName, e);
        }
    }

    /**
     * Log a file deletion operation
     */
    public void logFileDeletion(String fileName) {
        try {
            TransferLog log = new TransferLog(fileName, 0, "DELETED", "DELETE");
            log.setLastModified(LocalDateTime.now());
            log.setErrorMessage("File deleted during sync");

            repository.save(log);

        } catch (Exception e) {
            logger.error("Failed to log file deletion for: {}", fileName, e);
        }
    }

    /**
     * Get recent transfer logs
     */
    public List<TransferLog> getRecentLogs(int limit) {
        return repository.findTopRecentLogs(limit);
    }

    /**
     * Get logs by status
     */
    public List<TransferLog> getLogsByStatus(String status) {
        return repository.findByStatus(status);
    }

    /**
     * Get logs by operation type
     */
    public List<TransferLog> getLogsByOperationType(String operationType) {
        return repository.findByOperationType(operationType);
    }

    /**
     * Get logs within a time range
     */
    public List<TransferLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return repository.findByTimeRange(startTime, endTime);
    }

    /**
     * Get transfer statistics
     */
    public TransferStatistics getStatistics() {
        try {
            long totalTransfers = repository.count();
            long successfulTransfers = repository.countByStatus("SUCCESS");
            long failedTransfers = repository.countByStatus("FAILED");
            long skippedTransfers = repository.countByStatus("SKIPPED");
            Long totalBytesTransferred = repository.getTotalBytesTransferred();
            Double averageTransferDuration = repository.getAverageTransferDuration();

            return new TransferStatistics(
                totalTransfers,
                successfulTransfers,
                failedTransfers,
                skippedTransfers,
                totalBytesTransferred != null ? totalBytesTransferred : 0L,
                averageTransferDuration != null ? averageTransferDuration : 0.0
            );
        } catch (Exception e) {
            logger.error("Failed to get transfer statistics", e);
            return new TransferStatistics(0, 0, 0, 0, 0L, 0.0);
        }
    }

    /**
     * Clean up old transfer logs
     */
    public void cleanupOldLogs() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(maxLogRetentionDays);
            repository.deleteOldLogs(cutoffTime);
            logger.info("Cleaned up transfer logs older than {}", cutoffTime);
        } catch (Exception e) {
            logger.error("Failed to cleanup old transfer logs", e);
        }
    }

    /**
     * Search logs by file name pattern
     */
    public List<TransferLog> searchLogsByFileName(String fileNamePattern) {
        return repository.findByFileNamePattern(fileNamePattern);
    }

    /**
     * Transfer statistics class
     */
    public static class TransferStatistics {
        private final long totalTransfers;
        private final long successfulTransfers;
        private final long failedTransfers;
        private final long skippedTransfers;
        private final long totalBytesTransferred;
        private final double averageTransferDuration;

        public TransferStatistics(long totalTransfers, long successfulTransfers, long failedTransfers, 
                                long skippedTransfers, long totalBytesTransferred, double averageTransferDuration) {
            this.totalTransfers = totalTransfers;
            this.successfulTransfers = successfulTransfers;
            this.failedTransfers = failedTransfers;
            this.skippedTransfers = skippedTransfers;
            this.totalBytesTransferred = totalBytesTransferred;
            this.averageTransferDuration = averageTransferDuration;
        }

        public long getTotalTransfers() { return totalTransfers; }
        public long getSuccessfulTransfers() { return successfulTransfers; }
        public long getFailedTransfers() { return failedTransfers; }
        public long getSkippedTransfers() { return skippedTransfers; }
        public long getTotalBytesTransferred() { return totalBytesTransferred; }
        public double getAverageTransferDuration() { return averageTransferDuration; }
        
        public double getSuccessRate() {
            return totalTransfers > 0 ? (double) successfulTransfers / totalTransfers * 100 : 0;
        }
    }
}
