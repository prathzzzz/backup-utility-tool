package com.pratham.backuputility.service;

import com.pratham.backuputility.util.FileSystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for detecting file changes and deletions
 * Follows Single Responsibility Principle
 */
@Service
public class FileDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FileDetectionService.class);

    /**
     * Detect all files in a directory (for full transfers)
     */
    public List<Path> detectAllFiles(Path sourceBase) {
        try {
            List<Path> allFiles = FileSystemUtil.getAllFiles(sourceBase);
            logger.debug("Detected {} files for full transfer from {}", allFiles.size(), sourceBase);
            return allFiles;
        } catch (IOException e) {
            logger.error("Failed to detect all files in: {}", sourceBase, e);
            return new ArrayList<>();
        }
    }

    /**
     * Detect files that need to be transferred (new or modified)
     */
    public List<Path> detectChangedFiles(Path sourceBase, Path targetBase) {
        List<Path> changedFiles = new ArrayList<>();

        try {
            List<Path> sourceFiles = FileSystemUtil.getAllFiles(sourceBase);
            logger.info("Checking {} source files for changes from {} to {}", 
                sourceFiles.size(), sourceBase, targetBase);

            for (Path sourceFile : sourceFiles) {
                String relativePath = sourceBase.relativize(sourceFile).toString();
                Path targetFile = targetBase.resolve(relativePath);

                if (needsTransfer(sourceFile, targetFile)) {
                    changedFiles.add(sourceFile);
                    logger.info("File {} needs transfer", relativePath);
                }
            }

            logger.info("Detected {} changed files from {} to {}",
                changedFiles.size(), sourceBase, targetBase);

        } catch (IOException e) {
            logger.error("Failed to detect changed files from {} to {}", sourceBase, targetBase, e);
        }

        return changedFiles;
    }

    /**
     * Detect files that have been deleted from source and should be removed from target
     */
    public List<Path> detectDeletedFiles(Path sourceBase, Path targetBase) {
        List<Path> deletedFiles = new ArrayList<>();

        try {
            List<Path> sourceFiles = FileSystemUtil.getAllFiles(sourceBase);
            List<Path> targetFiles = FileSystemUtil.getAllFiles(targetBase);

            // Get relative paths of source files
            Set<String> sourceRelativePaths = new HashSet<>();
            for (Path sourceFile : sourceFiles) {
                sourceRelativePaths.add(sourceBase.relativize(sourceFile).toString());
            }

            // Find target files that don't exist in source
            for (Path targetFile : targetFiles) {
                String targetRelativePath = targetBase.relativize(targetFile).toString();
                if (!sourceRelativePaths.contains(targetRelativePath)) {
                    deletedFiles.add(targetFile);
                }
            }

            logger.debug("Detected {} deleted files in {} (not present in {})",
                deletedFiles.size(), targetBase, sourceBase);

        } catch (IOException e) {
            logger.error("Failed to detect deleted files from {} to {}", sourceBase, targetBase, e);
        }

        return deletedFiles;
    }

    /**
     * Check if a file needs to be transferred based on fast content comparison
     */
    private boolean needsTransfer(Path sourceFile, Path targetFile) {
        try {
            if (!Files.exists(targetFile)) {
                logger.info("File {} needs transfer: target doesn't exist", sourceFile.getFileName());
                return true;
            }

            // Use fast sample-based comparison
            boolean different = com.pratham.backuputility.util.FileSystemUtil.areFilesDifferent(sourceFile, targetFile);
            
            if (different) {
                logger.info("File {} needs transfer: files are different", sourceFile.getFileName());
            } else {
                logger.debug("File {} is identical, no transfer needed", sourceFile.getFileName());
            }
            
            return different;

        } catch (Exception e) {
            logger.warn("Error comparing files {} and {}: {}", sourceFile, targetFile, e.getMessage());
            return true; // Assume transfer needed if we can't compare
        }
    }
}
