package com.pratham.backuputility.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled maintenance tasks
 */
@Service
public class MaintenanceService {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceService.class);

    @Autowired
    private SQLiteSnapshotService snapshotService;

    @Autowired
    private TransferLogService transferLogService;

    /**
     * Clean up old snapshots daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldSnapshots() {
        logger.info("Starting scheduled cleanup of old snapshots");
        try {
            snapshotService.cleanupOldSnapshots();
            logger.info("Completed scheduled cleanup of old snapshots");
        } catch (Exception e) {
            logger.error("Failed to cleanup old snapshots", e);
        }
    }

    /**
     * Clean up old transfer logs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldTransferLogs() {
        logger.info("Starting scheduled cleanup of old transfer logs");
        try {
            transferLogService.cleanupOldLogs();
            logger.info("Completed scheduled cleanup of old transfer logs");
        } catch (Exception e) {
            logger.error("Failed to cleanup old transfer logs", e);
        }
    }

    /**
     * Log statistics every hour
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void logStatistics() {
        try {
            SQLiteSnapshotService.SnapshotStatistics snapshotStats = snapshotService.getStatistics();
            TransferLogService.TransferStatistics transferStats = transferLogService.getStatistics();

            logger.info("Snapshot Statistics - Total: {}, Block Hashes: {}, Unique Files: {}", 
                snapshotStats.getTotalSnapshots(), 
                snapshotStats.getTotalBlockHashes(), 
                snapshotStats.getUniqueFiles());

            logger.info("Transfer Statistics - Total: {}, Success: {}, Failed: {}, Success Rate: {:.1f}%", 
                transferStats.getTotalTransfers(), 
                transferStats.getSuccessfulTransfers(), 
                transferStats.getFailedTransfers(),
                transferStats.getSuccessRate());

        } catch (Exception e) {
            logger.error("Failed to log statistics", e);
        }
    }
}
