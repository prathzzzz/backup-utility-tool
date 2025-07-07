package com.pratham.backuputility.service;

import com.pratham.backuputility.model.*;
import com.pratham.backuputility.util.FileSystemUtil;
import com.pratham.backuputility.util.SnapshotPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main transfer service that orchestrates all transfer operations
 * Follows Single Responsibility Principle and uses composition
 */
@Service
public class TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);

    @Value("${app.dc-path}")
    private String dcPath;

    @Value("${app.dr-path}")
    private String drPath;

    // Service dependencies
    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private FileDetectionService fileDetectionService;

    @Autowired
    private DeltaCalculationService deltaCalculationService;

    @Autowired
    private DeltaApplicationService deltaApplicationService;

    @Autowired
    private ProgressTrackingService progressTrackingService;

    @Autowired
    private TransferLogService transferLogService;

    @Autowired
    private SnapshotPersistence snapshotPersistence;

    // State management
    private final AtomicBoolean transferInProgress = new AtomicBoolean(false);
    private final Map<String, FileSnapshot> snapshotCache = new ConcurrentHashMap<>();

    /**
     * Initialize the service
     */
    public void initialize() {
        try {
            loadSnapshots();
            logger.info("TransferService initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize TransferService", e);
        }
    }

    /**
     * Perform transfer operation
     */
    public List<String> performTransfer(String direction, String mode) {
        if (!transferInProgress.compareAndSet(false, true)) {
            return Arrays.asList("Transfer already in progress");
        }

        try {
            // Parse operation parameters
            TransferOperation.Direction dir = TransferOperation.parseDirection(direction);
            TransferOperation.Mode transferMode = TransferOperation.parseMode(mode);

            String sourcePath = (dir == TransferOperation.Direction.DC_TO_DR) ? dcPath : drPath;
            String targetPath = (dir == TransferOperation.Direction.DC_TO_DR) ? drPath : dcPath;

            TransferOperation operation = new TransferOperation(dir, transferMode, sourcePath, targetPath);

            return executeTransfer(operation);

        } catch (Exception e) {
            logger.error("Transfer failed", e);
            return Arrays.asList("Transfer failed: " + e.getMessage());
        } finally {
            transferInProgress.set(false);
        }
    }

    /**
     * Execute the transfer operation
     */
    private List<String> executeTransfer(TransferOperation operation) {
        List<String> results = new ArrayList<>();

        try {
            Path sourceBase = Paths.get(operation.getSourcePathStr());
            Path targetBase = Paths.get(operation.getTargetPathStr());

            logger.info("Starting {}: {} -> {}", operation.getOperationDescription(), sourceBase, targetBase);
            results.add("Mode: " + operation.getOperationDescription());

            // Detect files to process
            List<Path> filesToProcess = detectFilesToProcess(sourceBase, targetBase, operation);
            List<Path> filesToDelete = fileDetectionService.detectDeletedFiles(sourceBase, targetBase);

            if (filesToProcess.isEmpty() && filesToDelete.isEmpty()) {
                results.add("No files have changed since last sync");
                return results;
            }

            int totalFiles = filesToProcess.size() + filesToDelete.size();
            long totalBytes = FileSystemUtil.calculateTotalBytes(filesToProcess);

            logger.info("Found {} files to process ({} changes, {} deletions)",
                totalFiles, filesToProcess.size(), filesToDelete.size());
            results.add(String.format("Found %d files to process", totalFiles));

            // Start progress tracking
            progressTrackingService.startProgress(totalFiles, totalBytes, operation.getOperationDescription());

            // Process file transfers
            processFileTransfers(filesToProcess, sourceBase, targetBase, operation, results);

            // Process file deletions
            processFileDeletions(filesToDelete, targetBase, results);

            // Save snapshots
            saveSnapshots();

            // Complete progress tracking
            progressTrackingService.finishProgress("Transfer completed successfully");
            logger.info("Transfer completed successfully");

        } catch (Exception e) {
            logger.error("Transfer execution failed", e);
            results.add("Transfer failed: " + e.getMessage());
            progressTrackingService.sendError(e.getMessage());
        }

        return results;
    }

    /**
     * Detect files to process based on transfer mode
     */
    private List<Path> detectFilesToProcess(Path sourceBase, Path targetBase, TransferOperation operation) {
        if (operation.isFullMode()) {
            return fileDetectionService.detectAllFiles(sourceBase);
        } else {
            return fileDetectionService.detectChangedFiles(sourceBase, targetBase);
        }
    }

    /**
     * Process file transfers
     */
    private void processFileTransfers(List<Path> files, Path sourceBase, Path targetBase,
                                    TransferOperation operation, List<String> results) {
        int processed = 0;

        for (Path sourceFile : files) {
            try {
                processed++;
                String relativePath = sourceBase.relativize(sourceFile).toString();
                Path targetFile = targetBase.resolve(relativePath);

                String result = processFile(sourceFile, targetFile, relativePath, operation);
                results.add(result);

                // Update progress
                long fileSize = Files.exists(sourceFile) ? Files.size(sourceFile) : 0;
                String status = result.startsWith("✓") ? "Transferred" :
                               result.startsWith("○") ? "Unchanged" : "Processed";

                progressTrackingService.updateFileProgress(relativePath, status, fileSize);

                if (processed % 10 == 0) {
                    logger.info("Processed {}/{} files", processed, files.size());
                }

            } catch (Exception e) {
                String error = String.format("✗ ERROR: %s - %s",
                    sourceBase.relativize(sourceFile), e.getMessage());
                results.add(error);
                logger.error("Error processing file: {}", sourceFile, e);
                transferLogService.logTransferError(sourceFile.toString(), e.getMessage());
            }
        }
    }

    /**
     * Process a single file transfer
     */
    private String processFile(Path sourceFile, Path targetFile, String relativePath, TransferOperation operation) {
        try {
            // Create current snapshot
            FileSnapshot newSnapshot = snapshotService.createSnapshot(sourceFile, relativePath);
            FileSnapshot oldSnapshot = snapshotCache.get(relativePath);

            if (operation.isFullMode() || snapshotService.needsTransfer(newSnapshot, oldSnapshot)) {
                // Calculate and apply delta
                FileDelta delta = deltaCalculationService.calculateDelta(sourceFile, oldSnapshot, relativePath);
                deltaApplicationService.applyDelta(targetFile, delta);

                // Update snapshot cache
                snapshotCache.put(relativePath, newSnapshot);

                // Log the transfer
                transferLogService.logTransfer(relativePath, newSnapshot, delta);

                return String.format("✓ %s (%.1f%% efficiency)", relativePath, delta.getEfficiencyPercentage());
            } else {
                return String.format("○ %s (unchanged)", relativePath);
            }

        } catch (Exception e) {
            logger.error("Failed to process file: {}", sourceFile, e);
            throw new RuntimeException("File processing failed", e);
        }
    }

    /**
     * Process file deletions
     */
    private void processFileDeletions(List<Path> filesToDelete, Path targetBase, List<String> results) {
        for (Path targetFile : filesToDelete) {
            try {
                String relativePath = targetBase.relativize(targetFile).toString();

                Files.delete(targetFile);

                // Remove from snapshot cache
                snapshotCache.remove(relativePath);

                // Log deletion
                transferLogService.logFileDeletion(relativePath);

                // Update progress
                progressTrackingService.updateFileProgress(relativePath, "Deleted", 0);

                results.add(String.format("✗ %s (deleted)", relativePath));

            } catch (Exception e) {
                String error = String.format("✗ ERROR deleting %s - %s",
                    targetBase.relativize(targetFile), e.getMessage());
                results.add(error);
                logger.error("Error deleting file: {}", targetFile, e);
            }
        }
    }

    /**
     * Load snapshots from disk
     */
    private void loadSnapshots() {
        try {
            Map<String, FileSnapshot> loadedSnapshots = snapshotPersistence.loadSnapshots();
            snapshotCache.clear();
            snapshotCache.putAll(loadedSnapshots);
            logger.info("Loaded {} snapshots from disk", loadedSnapshots.size());
        } catch (Exception e) {
            logger.error("Failed to load snapshots", e);
        }
    }

    /**
     * Save snapshots to disk
     */
    private void saveSnapshots() {
        try {
            snapshotPersistence.saveSnapshots(snapshotCache);
            logger.debug("Saved {} snapshots to disk", snapshotCache.size());
        } catch (Exception e) {
            logger.error("Failed to save snapshots", e);
        }
    }

    /**
     * Get sync status
     */
    public Map<String, String> getSyncStatus() {
        Map<String, String> status = new LinkedHashMap<>();

        try {
            Path dcBase = Paths.get(dcPath);
            Path drBase = Paths.get(drPath);

            if (!FileSystemUtil.isDirectory(dcBase) || !FileSystemUtil.isDirectory(drBase)) {
                status.put("error", "One or both directories do not exist");
                return status;
            }

            // Get changed files
            List<Path> dcChangedFiles = fileDetectionService.detectChangedFiles(dcBase, drBase);
            List<Path> drChangedFiles = fileDetectionService.detectChangedFiles(drBase, dcBase);
            List<Path> dcDeletedFiles = fileDetectionService.detectDeletedFiles(dcBase, drBase);
            List<Path> drDeletedFiles = fileDetectionService.detectDeletedFiles(drBase, dcBase);

            // Calculate unique files that need syncing
            Set<String> allChangedPaths = new HashSet<>();
            dcChangedFiles.forEach(f -> allChangedPaths.add(dcBase.relativize(f).toString()));
            drChangedFiles.forEach(f -> allChangedPaths.add(drBase.relativize(f).toString()));
            dcDeletedFiles.forEach(f -> allChangedPaths.add(drBase.relativize(f).toString()));
            drDeletedFiles.forEach(f -> allChangedPaths.add(dcBase.relativize(f).toString()));

            int outOfSyncCount = allChangedPaths.size();
            int totalUniqueFiles = FileSystemUtil.countUniqueFiles(dcBase, drBase);
            int syncedFiles = Math.max(0, totalUniqueFiles - outOfSyncCount);

            status.put("synced_files", String.valueOf(syncedFiles));
            status.put("out_of_sync_files", String.valueOf(outOfSyncCount));
            status.put("total_snapshots", String.valueOf(snapshotCache.size()));
            status.put("mode", "Incremental (DC→DR), Full (DR→DC)");

        } catch (Exception e) {
            status.put("error", "Failed to check sync: " + e.getMessage());
            logger.error("Failed to get sync status", e);
        }

        return status;
    }

    /**
     * Get detailed sync status
     */
    public Map<String, Object> getDetailedSyncStatus() {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> fileStatuses = new LinkedHashMap<>();

        try {
            Path dcBase = Paths.get(dcPath);
            Path drBase = Paths.get(drPath);

            if (!FileSystemUtil.isDirectory(dcBase) || !FileSystemUtil.isDirectory(drBase)) {
                result.put("error", "One or both directories do not exist");
                return result;
            }

            // Get basic status
            result.putAll(getSyncStatus());

            // Get file-level status
            List<Path> dcChangedFiles = fileDetectionService.detectChangedFiles(dcBase, drBase);
            List<Path> drChangedFiles = fileDetectionService.detectChangedFiles(drBase, dcBase);

            // Add file statuses using traditional loops to avoid lambda issues
            for (Path f : dcChangedFiles) {
                String relativePath = dcBase.relativize(f).toString();
                fileStatuses.put(relativePath, "MISSING_IN_DR");
            }

            for (Path f : drChangedFiles) {
                String relativePath = drBase.relativize(f).toString();
                fileStatuses.put(relativePath, "MISSING_IN_DC");
            }

            // Limit results for UI
            if (fileStatuses.size() > 50) {
                Map<String, String> limitedStatuses = new LinkedHashMap<>();
                fileStatuses.entrySet().stream()
                    .limit(50)
                    .forEach(entry -> limitedStatuses.put(entry.getKey(), entry.getValue()));
                limitedStatuses.put("...", String.format("... and %d more files", fileStatuses.size() - 50));
                fileStatuses = limitedStatuses;
            }

            result.put("files", fileStatuses);

        } catch (Exception e) {
            result.put("error", "Failed to get detailed sync status: " + e.getMessage());
            logger.error("Failed to get detailed sync status", e);
        }

        return result;
    }

    /**
     * Check if transfer is in progress
     */
    public boolean isTransferInProgress() {
        return transferInProgress.get();
    }
}
