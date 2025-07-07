# DC-DR Backup Utility - Complete Technical Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Core Algorithms](#core-algorithms)
4. [Service Layer Design](#service-layer-design)
5. [Data Models](#data-models)
6. [File System Operations](#file-system-operations)
7. [Web Interface](#web-interface)
8. [API Endpoints](#api-endpoints)
9. [Testing Strategy](#testing-strategy)
10. [Performance Considerations](#performance-considerations)
11. [Security & Error Handling](#security--error-handling)
12. [Deployment & Configuration](#deployment--configuration)

---

## Project Overview

### Purpose
The DC-DR Backup Utility is a sophisticated Spring Boot application designed for high-performance incremental file synchronization between Data Center (DC) and Disaster Recovery (DR) environments. It implements block-level differential backup with real-time web monitoring.

### Key Features
- **Block-Level Incremental Sync**: Only transfers changed blocks, not entire files
- **Bidirectional Transfer**: Supports both DC→DR and DR→DC synchronization
- **Real-time Web Dashboard**: Modern UI with progress tracking
- **Snapshot-Based Detection**: Uses SHA-256 hashing for change detection
- **Full & Incremental Modes**: Flexible backup strategies
- **Timestamp Preservation**: Maintains file modification times
- **Empty File Handling**: Correctly handles file truncation scenarios

---

## Architecture

### Overall System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Web Layer                               │
├─────────────────────────────────────────────────────────────────┤
│  Controllers: TransferController, IncrementalTransferController │
│  Templates: Thymeleaf Layout + Fragments                       │
│  Static Assets: CSS, JavaScript                                │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service Layer                            │
├─────────────────────────────────────────────────────────────────┤
│  TransferService (Orchestrator)                                │
│  ├── SnapshotService (File State Management)                   │
│  ├── FileDetectionService (Change Detection)                   │
│  ├── DeltaCalculationService (Block Diff Algorithm)            │
│  ├── DeltaApplicationService (File Reconstruction)             │
│  └── ProgressTrackingService (Transfer Monitoring)             │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Utility Layer                             │
├─────────────────────────────────────────────────────────────────┤
│  HashUtil: SHA-256 hashing for block identification            │
│  FileSystemUtil: File operations and directory management      │
│  SnapshotPersistence: Snapshot serialization/deserialization  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Data Layer                               │
├─────────────────────────────────────────────────────────────────┤
│  Models: FileSnapshot, FileDelta, BlockDelta, TransferOperation│
│  Repository: TransferLogRepository (JPA)                       │
│  Database: MySQL for transfer logs                             │
│  File System: DC and DR directories                            │
└─────────────────────────────────────────────────────────────────┘
```

### Design Principles Applied

#### 1. **SOLID Principles**
- **Single Responsibility**: Each service has one clear purpose
- **Open/Closed**: Services are open for extension, closed for modification
- **Liskov Substitution**: All services implement well-defined contracts
- **Interface Segregation**: Clean separation between different concerns
- **Dependency Inversion**: Services depend on abstractions, not concretions

#### 2. **Clean Architecture**
- **Domain Models**: Pure business objects (FileSnapshot, FileDelta)
- **Service Layer**: Business logic without infrastructure concerns
- **Controllers**: Thin layer handling HTTP requests/responses
- **Utilities**: Infrastructure and cross-cutting concerns

---

## Core Algorithms

### 1. Block-Level Delta Algorithm

#### Algorithm Overview
The system implements a rolling hash-based block differential algorithm similar to rsync but optimized for our use case.

```java
Algorithm: CalculateFileDelta(sourceFile, targetSnapshot)
Input: sourceFile (Path), targetSnapshot (FileSnapshot)
Output: FileDelta containing changed blocks

1. Initialize:
   - blockSize = 4096 bytes (configurable)
   - delta = new FileDelta()
   - blockIndex = 0

2. For each block in sourceFile:
   a. Read blockSize bytes into buffer
   b. Calculate SHA-256 hash of block
   c. Compare with targetSnapshot.blockHashes[blockIndex]
   d. If different:
      - Create BlockDelta(blockIndex, blockData, hash)
      - Add to delta.blockDeltas
   e. Increment blockIndex

3. Set delta metadata:
   - totalBlocks = blockIndex
   - changedBlocks = delta.blockDeltas.size()
   - sourceFileSize = file size
   - sourceLastModified = file timestamp

4. Return delta
```

#### Block Delta Structure
```java
public class BlockDelta {
    private long blockIndex;     // Position in file (block number)
    private byte[] data;         // Actual block content
    private String hash;         // SHA-256 hash for verification
}
```

### 2. File Detection Algorithm

#### Change Detection Strategy
```java
Algorithm: DetectChangedFiles(sourceBase, targetBase)
Input: sourceBase (Path), targetBase (Path)
Output: List<Path> of files needing transfer

1. Get all files in sourceBase recursively
2. For each sourceFile:
   a. Calculate relativePath = sourceBase.relativize(sourceFile)
   b. targetFile = targetBase.resolve(relativePath)
   c. If needsTransfer(sourceFile, targetFile):
      - Add sourceFile to changedFiles list

3. Return changedFiles

Function: needsTransfer(sourceFile, targetFile)
1. If targetFile doesn't exist: return true
2. If file sizes differ: return true
3. If modification times differ: return true
4. Return false
```

### 3. Snapshot Management Algorithm

#### Snapshot Creation
```java
Algorithm: CreateSnapshot(basePath)
Input: basePath (Path to directory)
Output: Map<String, FileSnapshot> of all files

1. Initialize snapshots = new HashMap()
2. Get all files in basePath recursively
3. For each file:
   a. relativePath = basePath.relativize(file)
   b. Calculate file hash and block hashes
   c. Create FileSnapshot with metadata
   d. Store in snapshots[relativePath]

4. Return snapshots

Function: CalculateFileSnapshot(file)
1. Initialize blockHashes = new ArrayList()
2. Read file in blocks of blockSize
3. For each block:
   - Calculate SHA-256 hash
   - Add to blockHashes
4. Calculate overall file hash
5. Return FileSnapshot(path, size, lastModified, fileHash, blockHashes)
```

### 4. Delta Application Algorithm

#### File Reconstruction
```java
Algorithm: ApplyDelta(targetFile, delta)
Input: targetFile (Path), delta (FileDelta)
Output: Updated file with applied changes

1. Ensure target directory exists
2. If targetFile doesn't exist:
   - Create new file from delta blocks
3. Else:
   - Open targetFile in read-write mode
   - For each blockDelta in delta.blockDeltas:
     a. Seek to position = blockDelta.blockIndex * blockSize
     b. Write blockDelta.data to file
   - Truncate file to delta.sourceFileSize

4. Set file modification time = delta.sourceLastModified
```

---

## Service Layer Design

### TransferService (Orchestrator)
**Purpose**: Coordinates the entire transfer process

```java
Key Methods:
- performTransfer(direction, mode): Main entry point
- getDetailedSyncStatus(): Status analysis
- getSyncStatus(): Quick status check
- isTransferInProgress(): Transfer state

Orchestration Flow:
1. Validate parameters and check if transfer in progress
2. Determine source and target directories
3. Create/load snapshots
4. Detect changed files
5. Calculate deltas for changed files
6. Apply deltas to target files
7. Update snapshots
8. Clean up deleted files (if applicable)
```

### SnapshotService
**Purpose**: Manages file state snapshots for change detection

```java
Key Responsibilities:
- createSnapshot(basePath): Generate complete directory snapshot
- loadSnapshot(snapshotPath): Load existing snapshot from disk
- saveSnapshot(snapshot, path): Persist snapshot to disk
- compareSnapshots(old, new): Find differences between snapshots

Snapshot Format:
{
  "filePath": {
    "filePath": "relative/path/to/file",
    "fileSize": 1024,
    "lastModified": 1672531200000,
    "fileHash": "sha256hash",
    "blockHashes": ["block1hash", "block2hash", ...]
  }
}
```

### FileDetectionService
**Purpose**: Identifies files that need to be transferred

```java
Detection Logic:
1. detectAllFiles(sourceBase): For full transfers
2. detectChangedFiles(source, target): For incremental transfers
3. detectDeletedFiles(source, target): Files to remove
4. needsTransfer(sourceFile, targetFile): Individual file check

Performance Optimizations:
- Quick size and timestamp checks before detailed analysis
- Parallel processing for large directory trees
- Efficient file tree traversal
```

### DeltaCalculationService
**Purpose**: Calculates block-level differences between files

```java
Algorithm Details:
- Block size: 4KB (configurable)
- Hash algorithm: SHA-256 for integrity
- Memory efficient: Streams large files
- Handles edge cases: Empty files, partial blocks

Key Features:
- Only changed blocks are included in delta
- Preserves source file metadata
- Calculates transfer efficiency metrics
```

### DeltaApplicationService
**Purpose**: Applies deltas to reconstruct target files

```java
Application Logic:
1. New files: Create from delta blocks
2. Existing files: Update changed blocks only
3. File truncation: Handle size reduction correctly
4. Timestamp preservation: Maintain sync consistency

Error Handling:
- Atomic operations where possible
- Rollback on failure
- Validation of applied changes
```

### ProgressTrackingService
**Purpose**: Monitors transfer progress and provides real-time updates

```java
Tracking Metrics:
- Files processed / total files
- Bytes transferred / total bytes
- Transfer speed (MB/s)
- Estimated time remaining
- Success/failure counts

Real-time Updates:
- WebSocket notifications (planned)
- Progress percentages
- Current file being processed
```

---

## Data Models

### FileSnapshot
**Purpose**: Represents the state of a file at a point in time

```java
public class FileSnapshot {
    private String filePath;           // Relative path from base
    private long fileSize;             // File size in bytes
    private long lastModified;         // Timestamp in milliseconds
    private String fileHash;           // SHA-256 of entire file
    private List<String> blockHashes;  // SHA-256 of each block

    // Methods for comparison and serialization
}

Usage:
- Change detection: Compare timestamps and hashes
- Block identification: Match block hashes for delta calculation
- Integrity verification: Validate file transfers
```

### FileDelta
**Purpose**: Represents changes needed to transform one file to another

```java
public class FileDelta {
    private String filePath;              // Target file path
    private List<BlockDelta> blockDeltas; // Changed blocks
    private long totalBlocks;             // Total blocks in file
    private long changedBlocks;           // Number of changed blocks
    private long sourceFileSize;          // Final file size
    private long sourceLastModified;      // Source timestamp

    // Efficiency calculation methods
}

Efficiency Calculation:
efficiency = (totalBlocks - changedBlocks) / totalBlocks * 100
- 100% = No blocks transferred (file unchanged)
- 0% = All blocks transferred (new or completely changed file)
```

### BlockDelta
**Purpose**: Represents a single changed block within a file

```java
public class BlockDelta {
    private long blockIndex;    // Block position (0-based)
    private byte[] data;        // Block content
    private String hash;        // Block hash for verification
}

Block Layout:
File: [Block0][Block1][Block2][Block3]...
Index:   0      1      2      3    ...
Size:   4KB    4KB    4KB    ≤4KB  (last block may be smaller)
```

### TransferOperation
**Purpose**: Represents a single file transfer operation

```java
public class TransferOperation {
    private String filePath;
    private String operation;     // CREATE, UPDATE, DELETE
    private long bytesTransferred;
    private double efficiency;
    private String status;        // SUCCESS, FAILED, SKIPPED
    private String message;       // Status message
}
```

---

## File System Operations

### Directory Structure
```
Project Root/
├── dc-data/          # Data Center directory
│   ├── folder_1/
│   ├── folder_2/
│   └── ...
├── dr-data/          # Disaster Recovery directory
│   ├── folder_1/
│   ├── folder_2/
│   └── ...
└── snapshots/        # Snapshot storage
    ├── dc_snapshot.json
    └── dr_snapshot.json
```

### FileSystemUtil Operations

#### Directory Management
```java
Key Methods:
- ensureDirectoryExists(path): Create directory if not exists
- getAllFiles(basePath): Recursive file listing
- isDirectory(path): Check if path is directory
- deleteFileIfExists(path): Safe file deletion

Safety Features:
- Path validation to prevent directory traversal
- Atomic operations where possible
- Error handling with detailed logging
```

#### File Operations
```java
File Handling:
- Copy with metadata preservation
- Move operations
- Size and timestamp queries
- Permission handling

Performance Optimizations:
- NIO.2 for efficient file operations
- Parallel processing for large directories
- Memory-mapped files for large file operations
```

### Hash Calculations

#### HashUtil Implementation
```java
SHA-256 Algorithm Usage:
- Block hashing: Hash individual 4KB blocks
- File hashing: Hash of all block hashes combined
- Integrity verification: Compare hashes after transfer

Performance Considerations:
- Streaming hash calculation for large files
- Reusable MessageDigest instances
- Hex encoding for storage efficiency
```

---

## Web Interface

### Frontend Architecture

#### Thymeleaf Layout System
```
Templates Structure:
layout/
├── base.html                 # Main layout template
fragments/
├── header.html              # Page header
├── footer.html              # Page footer
├── controls.html            # Transfer controls
├── progress.html            # Progress tracking
└── syncTable.html           # Sync status table

Static Assets:
css/
└── dashboard-styles.css     # Centralized styling
js/
├── dashboard-common.js      # Common utilities
├── transfer-manager.js      # Transfer operations
└── sync-manager.js          # Sync management
```

#### JavaScript Architecture
```javascript
// Application State Management
const AppState = {
    isTransferInProgress: false,
    autoRefreshEnabled: true,
    wsConnection: null,
    lastUpdated: new Date()
};

// Transfer Manager
const transferManager = {
    start(direction, mode): Initiate transfer
    handleSuccess(response): Process successful transfer
    handleError(xhr): Handle transfer errors
    displayResults(results): Show transfer results
};

// Sync Manager
const syncManager = {
    checkStatus(): Check sync status
    updateSyncTable(files): Update status table
    getStatusClass(status): Get CSS class for status
};
```

#### UI Features
- **Responsive Design**: Mobile-friendly layout
- **Real-time Updates**: Auto-refresh capabilities
- **Progress Tracking**: Visual progress indicators
- **Error Handling**: User-friendly error messages
- **Bootstrap Integration**: Modern UI components

---

## API Endpoints

### Transfer Operations
```http
POST /api/incremental/transfer
Parameters:
- direction: DC_TO_DR | DR_TO_DC
- mode: incremental | full
Response: List<String> of operation results

GET /api/incremental/status
Response: {
    mode: "Current transfer mode",
    synced_files: number,
    out_of_sync_files: number
}

GET /api/incremental/detailed-status
Response: {
    mode: "Transfer mode description",
    files: { "file/path": "STATUS" },
    total_snapshots: number,
    synced_files: number,
    out_of_sync_files: number
}

GET /api/incremental/progress
Response: {
    inProgress: boolean
}
```

### Dashboard Endpoint
```http
GET /
Response: Dashboard HTML page with:
- Transfer logs from database
- Current sync status
- Transfer progress indicators
```

### Error Responses
```json
Standard Error Format:
{
    "error": "Error description",
    "timestamp": "ISO timestamp",
    "path": "/api/endpoint"
}

HTTP Status Codes:
- 200: Success
- 400: Bad Request (invalid parameters)
- 500: Internal Server Error
```

---

## Testing Strategy

### Test Categories

#### 1. Unit Tests
```java
Service Layer Tests:
- DeltaCalculationServiceTest: Block difference algorithm
- DeltaApplicationServiceTest: File reconstruction
- FileDetectionServiceTest: Change detection logic
- SnapshotServiceTest: Snapshot management

Utility Tests:
- HashUtilTest: Hash calculation accuracy
- FileSystemUtilTest: File operation safety
```

#### 2. Integration Tests
```java
Controller Tests:
- TransferControllerTest: Web interface integration
- IncrementalTransferControllerTest: API endpoint testing

Database Tests:
- TransferLogRepositoryTest: Data persistence
```

#### 3. Feature Tests
```java
End-to-End Scenarios:
- BackupTransferFeatureTest: Complete transfer workflows
- FileSystemIntegrationTest: Real file operations
- ConcurrencyTest: Multiple simultaneous transfers
```

#### 4. Performance Tests
```java
Load Testing:
- Large file transfers (>1GB)
- Many small files (>10,000 files)
- Deep directory structures
- Concurrent transfer scenarios
```

### Test Data Management
```java
Test Utilities:
- TestFileGenerator: Creates test files with known content
- TestDirectorySetup: Sets up test DC/DR directories
- SnapshotTestUtil: Creates test snapshots
- AssertionHelper: Custom assertions for file comparisons
```

---

## Performance Considerations

### Memory Management
```java
Strategies:
- Streaming file processing: Process files in chunks
- Block-based operations: 4KB blocks to balance memory and I/O
- Lazy loading: Load snapshots only when needed
- Garbage collection: Explicit cleanup of large objects

Memory Usage Patterns:
- Snapshot loading: O(number of files)
- Delta calculation: O(file size / block size)
- File transfer: O(block size) - constant memory
```

### I/O Optimization
```java
Techniques:
- NIO.2 file operations: Efficient file handling
- Parallel processing: Multiple files simultaneously
- Sequential access: Minimize random I/O
- Buffered operations: Reduce system calls

File System Considerations:
- Block alignment: 4KB blocks match typical filesystem blocks
- Metadata caching: Reduce filesystem metadata queries
- Atomic operations: Ensure consistency
```

### Network Optimization (Future)
```java
Planned Improvements:
- Compression: Compress delta blocks before transfer
- Deduplication: Share identical blocks across files
- Bandwidth throttling: Control transfer speed
- Resume capability: Resume interrupted transfers
```

### Algorithm Complexity
```java
Time Complexity:
- File detection: O(n) where n = number of files
- Delta calculation: O(f/b) where f = file size, b = block size
- Delta application: O(c) where c = number of changed blocks
- Overall transfer: O(n × f/b) for changed files only

Space Complexity:
- Snapshots: O(n × f/b) for storing block hashes
- Deltas: O(c × b) for changed block data
- Working memory: O(b) constant per operation
```

---

## Security & Error Handling

### Security Measures

#### Path Validation
```java
Security Checks:
- Path traversal prevention: Validate all file paths
- Directory boundaries: Ensure operations stay within DC/DR directories
- File permissions: Respect filesystem permissions
- Input sanitization: Validate all user inputs

Example:
private void validatePath(Path basePath, Path targetPath) {
    if (!targetPath.normalize().startsWith(basePath.normalize())) {
        throw new SecurityException("Path traversal attempt detected");
    }
}
```

#### Data Integrity
```java
Integrity Measures:
- Hash verification: SHA-256 hashes for all operations
- Atomic operations: Prevent partial file corruption
- Backup validation: Verify transfers after completion
- Rollback capability: Undo failed operations where possible
```

### Error Handling Strategy

#### Exception Hierarchy
```java
Custom Exceptions:
- TransferException: Base for all transfer-related errors
- FileDetectionException: File system access errors
- DeltaCalculationException: Block processing errors
- SnapshotException: Snapshot management errors

Error Recovery:
- Graceful degradation: Continue with remaining files on individual failures
- Retry mechanisms: Automatic retry for transient failures
- Logging: Comprehensive error logging for debugging
- User notification: Clear error messages in UI
```

#### Logging Strategy
```java
Log Levels:
- ERROR: Critical failures requiring attention
- WARN: Recoverable issues and important events
- INFO: Transfer progress and major operations
- DEBUG: Detailed operation traces for troubleshooting

Log Format:
[TIMESTAMP] [LEVEL] [THREAD] [CLASS] - Message
Context information included for all operations
```

---

## Deployment & Configuration

### Spring Boot Configuration

#### Application Properties
```properties
# Server Configuration
server.port=8081
server.servlet.context-path=/

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/backup_utility
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:password}
spring.jpa.hibernate.ddl-auto=update

# Application Configuration
app.dc-path=${DC_PATH:./dc-data}
app.dr-path=${DR_PATH:./dr-data}
app.snapshot-path=${SNAPSHOT_PATH:./snapshots}
app.block-size=${BLOCK_SIZE:4096}

# Logging Configuration
logging.level.com.pratham.backuputility=INFO
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

#### Environment Variables
```bash
Environment Configuration:
- DC_PATH: Data Center directory path
- DR_PATH: Disaster Recovery directory path
- SNAPSHOT_PATH: Snapshot storage directory
- DB_USERNAME: Database username
- DB_PASSWORD: Database password
- BLOCK_SIZE: Block size for delta operations (default: 4096)
```

### Database Schema
```sql
CREATE TABLE transfer_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_path VARCHAR(1000) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    bytes_transferred BIGINT DEFAULT 0,
    efficiency DOUBLE DEFAULT 0.0,
    message TEXT,
    transferred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_transferred_at (transferred_at),
    INDEX idx_status (status),
    INDEX idx_operation (operation)
);
```

### Deployment Options

#### Docker Deployment
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/backup-utility-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]

Environment Variables:
- DC_PATH=/data/dc
- DR_PATH=/data/dr
- SNAPSHOT_PATH=/data/snapshots
- DB_URL=jdbc:mysql://mysql:3306/backup_utility
```

#### Systemd Service
```ini
[Unit]
Description=DC-DR Backup Utility
After=network.target

[Service]
Type=simple
User=backup
WorkingDirectory=/opt/backup-utility
ExecStart=/usr/bin/java -jar backup-utility.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### Monitoring & Maintenance

#### Health Checks
```java
Built-in Monitoring:
- Application health endpoint: /actuator/health
- Database connectivity check
- File system access validation
- Transfer status monitoring

Custom Health Indicators:
- DC directory accessibility
- DR directory accessibility
- Snapshot directory writability
- Transfer service status
```

#### Backup Strategy
```bash
Backup Components:
1. Database: Regular MySQL dumps
2. Snapshots: Snapshot files backup
3. Configuration: Application properties backup
4. Logs: Log file retention and rotation

Automated Backup Script:
#!/bin/bash
mysqldump backup_utility > backup_$(date +%Y%m%d).sql
tar -czf snapshots_$(date +%Y%m%d).tar.gz snapshots/
find logs/ -name "*.log" -mtime +30 -delete
```

---

## Conclusion

This DC-DR Backup Utility represents a modern, efficient approach to incremental file synchronization. The architecture follows SOLID principles and clean code practices, making it maintainable and extensible. The block-level delta algorithm ensures optimal transfer efficiency, while the web interface provides real-time monitoring and control.

### Key Strengths
1. **Efficient Algorithm**: Block-level deltas minimize transfer overhead
2. **Clean Architecture**: Well-separated concerns enable easy maintenance
3. **Comprehensive Testing**: Unit, integration, and feature tests ensure reliability
4. **Modern UI**: Responsive web interface with real-time updates
5. **Robust Error Handling**: Graceful failure handling and recovery
6. **Flexible Configuration**: Environment-based configuration for different deployments

### Future Enhancements
1. **Network Support**: Remote DC/DR synchronization over network
2. **Compression**: Block compression for reduced bandwidth usage
3. **Encryption**: Secure transfer with encryption support
4. **Scheduling**: Automated transfer scheduling
5. **Monitoring**: Advanced monitoring and alerting capabilities
6. **Multi-tenant**: Support for multiple DC/DR pairs

This documentation provides a complete technical reference for understanding, maintaining, and extending the DC-DR Backup Utility.
