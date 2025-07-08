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
            System.err.println("Directory does not exist: " + directory);
            return files;
        }

        System.out.println("Scanning directory: " + directory.toAbsolutePath());

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                files.add(file);
                if (files.size() % 10 == 0) { // Log every 10 files
                    System.out.println("Found " + files.size() + " files so far...");
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Log but continue processing
                System.err.println("Failed to visit file: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                System.out.println("Entering directory: " + dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                if (exc != null) {
                    System.err.println("Error after visiting directory: " + dir + " - " + exc.getMessage());
                }
                System.out.println("Finished directory: " + dir);
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("Total files found in " + directory + ": " + files.size());
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

    /**
     * Simple but effective file comparison
     */
    public static boolean areFilesDifferent(Path file1, Path file2) {
        try {
            // If either file doesn't exist, they're different
            if (!Files.exists(file1) || !Files.exists(file2)) {
                return true;
            }

            // Size check first (fastest)
            long size1 = Files.size(file1);
            long size2 = Files.size(file2);
            
            if (size1 != size2) {
                return true; // Different sizes = definitely different
            }

            // If both are empty, they're the same
            if (size1 == 0) {
                return false;
            }

            // For any file, just do a simple hash of first 1KB + last 1KB
            return !quickHashEquals(file1, file2, size1);

        } catch (IOException e) {
            System.err.println("Error comparing files: " + e.getMessage());
            return true; // Assume different if we can't compare
        }
    }

    /**
     * Quick hash comparison using first and last 1KB
     */
    private static boolean quickHashEquals(Path file1, Path file2, long fileSize) throws IOException {
        int hashSize = (int) Math.min(1024, fileSize);
        
        // Read first 1KB
        byte[] start1 = readBytesFromPosition(file1, 0, hashSize);
        byte[] start2 = readBytesFromPosition(file2, 0, hashSize);
        
        if (!java.util.Arrays.equals(start1, start2)) {
            return false;
        }

        // If file is larger than 1KB, also check last 1KB
        if (fileSize > 1024) {
            long lastPos = Math.max(0, fileSize - 1024);
            byte[] end1 = readBytesFromPosition(file1, lastPos, 1024);
            byte[] end2 = readBytesFromPosition(file2, lastPos, 1024);
            
            if (!java.util.Arrays.equals(end1, end2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Read bytes from a specific position in a file
     */
    private static byte[] readBytesFromPosition(Path file, long position, int length) throws IOException {
        try (java.nio.channels.SeekableByteChannel channel = Files.newByteChannel(file)) {
            channel.position(position);
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(length);
            int bytesRead = channel.read(buffer);
            
            if (bytesRead == -1) {
                return new byte[0];
            }
            
            byte[] result = new byte[bytesRead];
            buffer.flip();
            buffer.get(result);
            return result;
        }
    }
}
