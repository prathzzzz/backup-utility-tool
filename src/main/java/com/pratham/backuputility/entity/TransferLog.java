package com.pratham.backuputility.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_logs", indexes = {
    @Index(name = "idx_transferred_at", columnList = "transferredAt"),
    @Index(name = "idx_file_name", columnList = "fileName"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_operation_type", columnList = "operationType")
})
public class TransferLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fileName", length = 500, nullable = false)
    private String fileName;

    @Column(name = "fileSize", nullable = false)
    private long fileSize;
    
    @Column(name = "lastModified")
    private LocalDateTime lastModified;

    @Column(name = "status", length = 20, nullable = false)
    private String status; // SUCCESS, FAILED, SKIPPED

    @Column(name = "operationType", length = 30, nullable = false)
    private String operationType; // INCREMENTAL, FULL, DELTA_SYNC

    @Column(name = "errorMessage", length = 1000)
    private String errorMessage;

    @Column(name = "transferredAt", nullable = false)
    private LocalDateTime transferredAt;
    
    @Column(name = "transferDurationMs")
    private Long transferDurationMs;
    
    @Column(name = "bytesTransferred")
    private Long bytesTransferred;
    
    @Column(name = "compressionRatio")
    private Double compressionRatio;
    
    @Column(name = "checksum", length = 64)
    private String checksum;

    // Constructors
    public TransferLog() {}
    
    public TransferLog(String fileName, long fileSize, String status, String operationType) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status;
        this.operationType = operationType;
        this.transferredAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getTransferredAt() { return transferredAt; }
    public void setTransferredAt(LocalDateTime transferredAt) { this.transferredAt = transferredAt; }
    
    public Long getTransferDurationMs() { return transferDurationMs; }
    public void setTransferDurationMs(Long transferDurationMs) { this.transferDurationMs = transferDurationMs; }
    
    public Long getBytesTransferred() { return bytesTransferred; }
    public void setBytesTransferred(Long bytesTransferred) { this.bytesTransferred = bytesTransferred; }
    
    public Double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(Double compressionRatio) { this.compressionRatio = compressionRatio; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
}
