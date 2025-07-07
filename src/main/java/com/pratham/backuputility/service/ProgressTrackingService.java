package com.pratham.backuputility.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProgressTrackingService {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    private final AtomicInteger totalFiles = new AtomicInteger(0);
    private final AtomicInteger processedFiles = new AtomicInteger(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong processedBytes = new AtomicLong(0);
    private volatile String currentFile = "";
    private volatile String currentOperation = "Idle";
    private volatile LocalDateTime startTime;
    private volatile boolean isActive = false;

    public void startProgress(int totalFileCount, long totalByteCount, String operation) {
        this.totalFiles.set(totalFileCount);
        this.processedFiles.set(0);
        this.totalBytes.set(totalByteCount);
        this.processedBytes.set(0);
        this.currentOperation = operation;
        this.startTime = LocalDateTime.now();
        this.isActive = true;
        sendProgressUpdate();
    }

    public void updateFileProgress(String fileName, String status, long fileSize) {
        this.currentFile = fileName;
        this.processedFiles.incrementAndGet();
        this.processedBytes.addAndGet(fileSize);

        ProgressUpdate update = new ProgressUpdate();
        update.setCurrentFile(fileName);
        update.setStatus(status);
        update.setProcessedFiles(processedFiles.get());
        update.setTotalFiles(totalFiles.get());
        update.setProcessedBytes(processedBytes.get());
        update.setTotalBytes(totalBytes.get());
        update.setOperation(currentOperation);
        update.setActive(isActive);
        update.setStartTime(startTime);
        update.setPercentage(calculatePercentage());
        update.setEstimatedTimeRemaining(calculateETA());

        messagingTemplate.convertAndSend("/topic/progress", update);
    }

    public void finishProgress(String finalMessage) {
        this.isActive = false;
        this.currentOperation = finalMessage;
        this.currentFile = "";
        sendProgressUpdate();
    }

    public void sendError(String errorMessage) {
        this.isActive = false;
        this.currentOperation = "Error: " + errorMessage;
        sendProgressUpdate();
    }

    private void sendProgressUpdate() {
        ProgressUpdate update = new ProgressUpdate();
        update.setCurrentFile(currentFile);
        update.setProcessedFiles(processedFiles.get());
        update.setTotalFiles(totalFiles.get());
        update.setProcessedBytes(processedBytes.get());
        update.setTotalBytes(totalBytes.get());
        update.setOperation(currentOperation);
        update.setActive(isActive);
        update.setStartTime(startTime);
        update.setPercentage(calculatePercentage());
        update.setEstimatedTimeRemaining(calculateETA());

        messagingTemplate.convertAndSend("/topic/progress", update);
    }

    private double calculatePercentage() {
        if (totalFiles.get() == 0) return 0.0;
        return (double) processedFiles.get() / totalFiles.get() * 100.0;
    }

    private String calculateETA() {
        if (!isActive || startTime == null || processedFiles.get() == 0) {
            return "Calculating...";
        }

        long elapsedSeconds = java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
        double filesPerSecond = (double) processedFiles.get() / elapsedSeconds;

        if (filesPerSecond <= 0) return "Calculating...";

        int remainingFiles = totalFiles.get() - processedFiles.get();
        long etaSeconds = (long) (remainingFiles / filesPerSecond);

        if (etaSeconds < 60) {
            return etaSeconds + "s";
        } else if (etaSeconds < 3600) {
            return (etaSeconds / 60) + "m " + (etaSeconds % 60) + "s";
        } else {
            return (etaSeconds / 3600) + "h " + ((etaSeconds % 3600) / 60) + "m";
        }
    }

    public static class ProgressUpdate {
        private String currentFile;
        private String status;
        private int processedFiles;
        private int totalFiles;
        private long processedBytes;
        private long totalBytes;
        private String operation;
        private boolean active;
        private LocalDateTime startTime;
        private double percentage;
        private String estimatedTimeRemaining;

        // Getters and setters
        public String getCurrentFile() { return currentFile; }
        public void setCurrentFile(String currentFile) { this.currentFile = currentFile; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getProcessedFiles() { return processedFiles; }
        public void setProcessedFiles(int processedFiles) { this.processedFiles = processedFiles; }
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
        public long getProcessedBytes() { return processedBytes; }
        public void setProcessedBytes(long processedBytes) { this.processedBytes = processedBytes; }
        public long getTotalBytes() { return totalBytes; }
        public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public String getEstimatedTimeRemaining() { return estimatedTimeRemaining; }
        public void setEstimatedTimeRemaining(String estimatedTimeRemaining) { this.estimatedTimeRemaining = estimatedTimeRemaining; }
    }
}
