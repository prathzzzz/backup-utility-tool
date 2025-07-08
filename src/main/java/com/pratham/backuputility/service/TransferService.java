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

            // Load existing snapshots
            loadSnapshots();

            // CRITICAL FIX: For full sync, clean up snapshot cache to prevent contamination
            if (operation.isFullMode()) {
                cleanupSnapshotCacheForFullSync(sourceBase, operation.getDirection().toString());
            }

            // Detect files to process
            List<Path> files = detectFilesToProcess(sourceBase, targetBase, operation);
            
            // Calculate total bytes for progress tracking
            long totalBytes = FileSystemUtil.calculateTotalBytes(files);
            
            // Start progress tracking
            progressTrackingService.startProgress(files.size(), totalBytes, operation.getDirection().toString());

            logger.info("Starting {} transfer: {} files, {} bytes", 
                operation.isFullMode() ? "full" : "incremental", files.size(), totalBytes);

            // Process transfers
            processFileTransfers(files, sourceBase, targetBase, operation, results);

            // Handle deletions for full mode
            if (operation.isFullMode()) {
                List<Path> filesToDelete = detectFilesToDelete(sourceBase, targetBase, operation);
                processFileDeletions(filesToDelete, targetBase, results);
            }

            // Save snapshots to disk
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
     * Clean up snapshot cache for full sync to prevent contamination from previous sync operations
     */
    private void cleanupSnapshotCacheForFullSync(Path sourceBase, String direction) {
        try {
            logger.info("Cleaning up snapshot cache for full sync in direction: {}", direction);
            
            // Get all current files in the source directory
            List<Path> currentSourceFiles = FileSystemUtil.getAllFiles(sourceBase);
            Set<String> currentSourcePaths = new HashSet<>();
            
            for (Path sourceFile : currentSourceFiles) {
                String relativePath = sourceBase.relativize(sourceFile).toString();
                currentSourcePaths.add(relativePath);
            }
            
            // Remove snapshot entries that don't exist in current source
            Set<String> toRemove = new HashSet<>();
            for (String cachedPath : snapshotCache.keySet()) {
                if (!currentSourcePaths.contains(cachedPath)) {
                    toRemove.add(cachedPath);
                }
            }
            
            for (String pathToRemove : toRemove) {
                snapshotCache.remove(pathToRemove);
                logger.debug("Removed stale snapshot cache entry: {}", pathToRemove);
            }
            
            logger.info("Cleaned up {} stale snapshot entries for full sync", toRemove.size());
            
        } catch (Exception e) {
            logger.error("Failed to cleanup snapshot cache for full sync", e);
            // Don't fail the operation, just log the error
        }
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
     * Detect files that need to be deleted from target (for full sync)
     */
    private List<Path> detectFilesToDelete(Path sourceBase, Path targetBase, TransferOperation operation) {
        List<Path> filesToDelete = new ArrayList<>();
        
        try {
            // Get all files in target directory
            List<Path> targetFiles = FileSystemUtil.getAllFiles(targetBase);
            
            // Get all files in source directory
            List<Path> sourceFiles = FileSystemUtil.getAllFiles(sourceBase);
            Set<String> sourceRelativePaths = new HashSet<>();
            
            for (Path sourceFile : sourceFiles) {
                String relativePath = sourceBase.relativize(sourceFile).toString();
                sourceRelativePaths.add(relativePath);
            }
            
            // Find target files that don't exist in source
            for (Path targetFile : targetFiles) {
                String relativePath = targetBase.relativize(targetFile).toString();
                if (!sourceRelativePaths.contains(relativePath)) {
                    filesToDelete.add(targetFile);
                }
            }
            
            logger.info("Found {} files to delete from target for full sync", filesToDelete.size());
            
        } catch (Exception e) {
            logger.error("Failed to detect files to delete", e);
        }
        
        return filesToDelete;
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
                
                if (Files.exists(targetFile)) {
                    Files.delete(targetFile);
                    results.add(String.format("✗ Deleted: %s", relativePath));
                    logger.info("Deleted file: {}", relativePath);
                    
                    // Remove from snapshot cache
                    snapshotCache.remove(relativePath);
                    
                    // Log the deletion
                    transferLogService.logTransferError(relativePath, "File deleted during full sync");
                }
                
            } catch (Exception e) {
                String error = String.format("✗ ERROR deleting %s: %s", 
                    targetBase.relativize(targetFile), e.getMessage());
                results.add(error);
                logger.error("Failed to delete file: {}", targetFile, e);
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

            logger.info("DC Path: {}", dcBase.toAbsolutePath());
            logger.info("DR Path: {}", drBase.toAbsolutePath());

            if (!FileSystemUtil.isDirectory(dcBase) || !FileSystemUtil.isDirectory(drBase)) {
                status.put("error", "One or both directories do not exist");
                return status;
            }

            // Get all files from both directories
            List<Path> dcFiles = FileSystemUtil.getAllFiles(dcBase);
            List<Path> drFiles = FileSystemUtil.getAllFiles(drBase);

            logger.info("Found {} files in DC and {} files in DR", dcFiles.size(), drFiles.size());

            // Create sets of relative paths for comparison
            Set<String> dcRelativePaths = new HashSet<>();
            Set<String> drRelativePaths = new HashSet<>();
            
            for (Path dcFile : dcFiles) {
                dcRelativePaths.add(dcBase.relativize(dcFile).toString());
            }
            
            for (Path drFile : drFiles) {
                drRelativePaths.add(drBase.relativize(drFile).toString());
            }

            // Find files that need synchronization
            Set<String> outOfSyncFiles = new HashSet<>();
            
            logger.info("DC has {} files, DR has {} files", dcFiles.size(), drFiles.size());
            
            // Check DC files that are missing in DR or different
            for (Path dcFile : dcFiles) {
                String relativePath = dcBase.relativize(dcFile).toString();
                Path correspondingDrFile = drBase.resolve(relativePath);
                
                boolean needsSync = false;
                String reason = "";
                
                if (!Files.exists(correspondingDrFile)) {
                    needsSync = true;
                    reason = "missing in DR";
                } else if (needsTransfer(dcFile, correspondingDrFile)) {
                    needsSync = true;
                    reason = "content different";
                }
                
                if (needsSync) {
                    outOfSyncFiles.add(relativePath);
                    logger.info("File {} needs sync: {}", relativePath, reason);
                }
            }
            
            logger.info("Found {} DC files that need sync", outOfSyncFiles.size());
            
            // Check DR files that are missing in DC
            int drOnlyFiles = 0;
            for (Path drFile : drFiles) {
                String relativePath = drBase.relativize(drFile).toString();
                Path correspondingDcFile = dcBase.resolve(relativePath);
                
                if (!Files.exists(correspondingDcFile)) {
                    outOfSyncFiles.add(relativePath);
                    drOnlyFiles++;
                    logger.info("File {} exists in DR but not in DC", relativePath);
                }
            }
            
            logger.info("Found {} DR-only files", drOnlyFiles);

            // Calculate totals
            Set<String> allUniqueFiles = new HashSet<>();
            allUniqueFiles.addAll(dcRelativePaths);
            allUniqueFiles.addAll(drRelativePaths);
            
            int totalUniqueFiles = allUniqueFiles.size();
            int outOfSyncCount = outOfSyncFiles.size();
            int syncedFiles = Math.max(0, totalUniqueFiles - outOfSyncCount);

            logger.info("Sync status: {} total files, {} out of sync, {} synced", 
                totalUniqueFiles, outOfSyncCount, syncedFiles);

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
     * Helper method to check if a file needs transfer (fast sample-based comparison)
     */
    private boolean needsTransfer(Path sourceFile, Path targetFile) {
        try {
            if (!Files.exists(targetFile)) {
                logger.debug("File {} needs transfer: target doesn't exist", sourceFile.getFileName());
                return true;
            }

            // Use fast sample-based comparison
            boolean different = FileSystemUtil.areFilesDifferent(sourceFile, targetFile);
            
            if (different) {
                logger.debug("File {} needs transfer: content different", sourceFile.getFileName());
            } else {
                logger.debug("File {} is identical", sourceFile.getFileName());
            }
            
            return different;

        } catch (Exception e) {
            logger.warn("Error comparing files {} and {}: {}", sourceFile, targetFile, e.getMessage());
            return true; // Assume transfer needed if we can't compare
        }
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
