package com.pratham.backuputility.entity;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_logs", indexes = {
    @Index(name = "idx_transferred_at", columnList = "transferredAt"),
    @Index(name = "idx_file_name", columnList = "fileName"),
    @Index(name = "idx_status", columnList = "status")
})
public class TransferLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fileName", length = 500)
    private String fileName;

    private long fileSize;
    private LocalDateTime lastModified;

    @Column(name = "status", length = 20)
    private String status; // SUCCESS, FAILED

    @Column(name = "errorMessage", length = 1000)
    private String errorMessage;

    @Column(name = "transferredAt")
    private LocalDateTime transferredAt;

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getTransferredAt() { return transferredAt; }
    public void setTransferredAt(LocalDateTime transferredAt) { this.transferredAt = transferredAt; }
}
