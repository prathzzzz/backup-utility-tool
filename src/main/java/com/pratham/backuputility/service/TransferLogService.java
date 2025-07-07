package com.pratham.backuputility.service;

import com.pratham.backuputility.entity.TransferLog;
import com.pratham.backuputility.model.FileDelta;
import com.pratham.backuputility.model.FileSnapshot;
import com.pratham.backuputility.repository.TransferLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service responsible for logging transfer operations
 * Follows Single Responsibility Principle
 */
@Service
public class TransferLogService {

    private static final Logger logger = LoggerFactory.getLogger(TransferLogService.class);

    @Autowired
    private TransferLogRepository repository;

    /**
     * Log a successful transfer operation
     */
    public void logTransfer(String fileName, FileSnapshot snapshot, FileDelta delta) {
        try {
            TransferLog log = new TransferLog();
            log.setFileName(fileName);
            log.setFileSize(snapshot.getSize());
            log.setLastModified(snapshot.getLastModified());
            log.setStatus("SUCCESS");
            log.setErrorMessage(String.format("Incremental: %d/%d blocks updated (%.1f%% efficiency)",
                delta.getChangedBlocks(), delta.getTotalBlocks(), delta.getEfficiencyPercentage()));
            log.setTransferredAt(LocalDateTime.now());

            repository.save(log);

        } catch (Exception e) {
            logger.error("Failed to log transfer for file: {}", fileName, e);
        }
    }

    /**
     * Log a failed transfer operation
     */
    public void logTransferError(String fileName, String errorMessage) {
        try {
            TransferLog log = new TransferLog();
            log.setFileName(fileName);
            log.setFileSize(0);
            log.setLastModified(LocalDateTime.now());
            log.setStatus("FAILED");
            log.setErrorMessage(errorMessage);
            log.setTransferredAt(LocalDateTime.now());

            repository.save(log);

        } catch (Exception e) {
            logger.error("Failed to log transfer error for file: {}", fileName, e);
        }
    }

    /**
     * Log a file deletion operation
     */
    public void logFileDeletion(String fileName) {
        try {
            TransferLog log = new TransferLog();
            log.setFileName(fileName);
            log.setFileSize(0);
            log.setLastModified(LocalDateTime.now());
            log.setStatus("DELETED");
            log.setErrorMessage("File deleted during sync");
            log.setTransferredAt(LocalDateTime.now());

            repository.save(log);

        } catch (Exception e) {
            logger.error("Failed to log file deletion for: {}", fileName, e);
        }
    }
}
