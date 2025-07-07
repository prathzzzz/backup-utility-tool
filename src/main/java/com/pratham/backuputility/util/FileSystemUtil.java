package com.pratham.backuputility.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for file system operations
 */
public final class FileSystemUtil {

    private FileSystemUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Get all files in a directory recursively
     */
    public static List<Path> getAllFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();

        if (!Files.exists(directory)) {
            return files;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                files.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Log but continue processing
                System.err.println("Failed to visit file: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

    /**
     * Calculate total size of files in bytes
     */
    public static long calculateTotalBytes(List<Path> files) {
        return files.stream()
            .mapToLong(file -> {
                try {
                    return Files.exists(file) ? Files.size(file) : 0;
                } catch (IOException e) {
                    return 0;
                }
            })
            .sum();
    }

    /**
     * Count unique files across two directory trees
     */
    public static int countUniqueFiles(Path dir1, Path dir2) throws IOException {
        List<Path> files1 = getAllFiles(dir1);
        List<Path> files2 = getAllFiles(dir2);

        // Use relative paths for comparison
        List<String> relativePaths = new ArrayList<>();

        // Add files from dir1
        for (Path file : files1) {
            String relativePath = dir1.relativize(file).toString();
            if (!relativePaths.contains(relativePath)) {
                relativePaths.add(relativePath);
            }
        }

        // Add files from dir2
        for (Path file : files2) {
            String relativePath = dir2.relativize(file).toString();
            if (!relativePaths.contains(relativePath)) {
                relativePaths.add(relativePath);
            }
        }

        return relativePaths.size();
    }

    /**
     * Ensure directory exists, creating it if necessary
     */
    public static void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    /**
     * Check if a file exists and is a regular file
     */
    public static boolean isRegularFile(Path file) {
        return Files.exists(file) && Files.isRegularFile(file);
    }

    /**
     * Check if a path exists and is a directory
     */
    public static boolean isDirectory(Path path) {
        return Files.exists(path) && Files.isDirectory(path);
    }
}
