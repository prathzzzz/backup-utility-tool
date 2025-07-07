# DC-DR Backup Utility

A modern, high-performance Spring Boot application for incremental file synchronization between Data Center (DC) and Disaster Recovery (DR) environments.

## Features

- **Block-Level Incremental Transfer**: Only transfers changed blocks, not entire files
- **Bidirectional Sync**: Supports both DC→DR and DR→DC synchronization
- **True Incremental Detection**: Efficiently detects only actually changed files
- **Snapshot-Based Tracking**: Uses SHA-256 hashes for accurate change detection
- **Modern Web Dashboard**: Real-time status monitoring and transfer initiation
- **RESTful API**: Complete programmatic control

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- MySQL 8.0+

### Setup

1. Clone the repository
2. Configure database connection in `application.properties`
3. Set DC and DR paths in `application.properties`
4. Run `mvn spring-boot:run`

### Configuration

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/dcdr_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# Paths
app.dc-path=/path/to/dc-data
app.dr-path=/path/to/dr-data
app.block-size=4096
```

## API Endpoints

### Incremental Transfer

```bash
# DC to DR
curl -X POST "http://localhost:8081/api/incremental/transfer?direction=DC_TO_DR"

# DR to DC
curl -X POST "http://localhost:8081/api/incremental/transfer?direction=DR_TO_DC"
```

### Status Check

```bash
curl "http://localhost:8081/api/incremental/status"
```

### Health Check

```bash
curl "http://localhost:8081/api/health"
```

## Web Dashboard

Access the web dashboard at `http://localhost:8081/` for:

- Real-time sync status
- Transfer initiation
- File synchronization monitoring
- Transfer history

## Architecture

### Core Components

1. **IncrementalTransferService**: Main service handling block-level transfers
2. **SnapshotPersistence**: Manages file snapshots and metadata
3. **TransferController**: Web dashboard controller
4. **IncrementalTransferController**: REST API controller
5. **HealthController**: System health monitoring

### How It Works

1. **Snapshot Creation**: Creates SHA-256 hashes for each file block
2. **Change Detection**: Compares current files with stored snapshots
3. **Delta Calculation**: Identifies only changed blocks
4. **Block Transfer**: Transfers only modified blocks
5. **Snapshot Update**: Updates snapshots for future comparisons

### Performance Optimizations

- **True Incremental**: Only scans files that actually exist in source directory
- **Efficient Change Detection**: Uses file size and timestamp for quick filtering
- **Block-Level Transfer**: Minimizes data transfer by sending only changed blocks
- **Smart Caching**: Maintains snapshot cache for fast lookups
- **Parallel Processing**: Uses concurrent processing for status checks

## File Structure

```
src/main/java/com/pratham/backuputility/
├── BackupUtilityApplication.java          # Main application
├── controller/
│   ├── TransferController.java           # Web dashboard
│   ├── IncrementalTransferController.java # REST API
│   └── HealthController.java             # Health monitoring
├── service/
│   └── IncrementalTransferService.java   # Core transfer logic
├── entity/
│   └── TransferLog.java                  # Transfer history
├── repository/
│   └── TransferLogRepository.java        # Data access
└── util/
    └── SnapshotPersistence.java          # Snapshot management
```

## Testing

### Manual Testing

```bash
# Create test file
echo "test data" > /path/to/dc-data/test.txt

# Sync DC to DR
curl -X POST "http://localhost:8081/api/incremental/transfer?direction=DC_TO_DR"

# Verify file exists
ls -la /path/to/dr-data/test.txt
```

### Automated Testing

```bash
mvn test
```

## Monitoring

- **Logs**: Check application logs for transfer details
- **Database**: View transfer history in `transfer_log` table
- **Snapshots**: Stored in `~/.backup-utility/snapshots/`

## Troubleshooting

### Common Issues

1. **No files detected**: Check file paths in `application.properties`
2. **Database connection**: Verify MySQL is running and credentials are correct
3. **Permission errors**: Ensure application has read/write access to DC and DR directories
4. **Port conflicts**: Default port is 8081, change if needed

### Debug Mode

Enable debug logging in `application.properties`:

```properties
logging.level.com.pratham.backuputility=DEBUG
```

## Performance

- **Block Size**: Default 4KB blocks for optimal performance
- **Memory Usage**: Efficient streaming for large files
- **CPU Usage**: Minimal overhead with smart change detection
- **Network**: Only transfers changed blocks, reducing bandwidth

## Security

- **Data Integrity**: SHA-256 hashing ensures data accuracy
- **Error Handling**: Comprehensive error recovery
- **Atomic Operations**: Ensures data consistency during transfers

## License

This project is licensed under the MIT License.
