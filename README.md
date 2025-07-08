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

## Web Dashboard

Access the web dashboard at `http://localhost:8081/` for:

- Real-time sync status
- Transfer initiation
- File synchronization monitoring
- Transfer history

### How It Works

1. **Snapshot Creation**: Creates SHA-256 hashes for each file block
2. **Change Detection**: Compares current files with stored snapshots
3. **Delta Calculation**: Identifies only changed blocks
4. **Block Transfer**: Transfers only modified blocks
5. **Snapshot Update**: Updates snapshots for future comparisons





