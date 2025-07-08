# PowerShell script to generate test data for backup utility

# Base directory where to create the structure
$BASE_DIR = "dc-data"

# Number of top-level folders
$TOP_LEVEL = 5

# Number of subfolders per folder
$SUBFOLDERS = 5

# Number of files per folder
$FILES_PER_FOLDER = 3

# Size of each file in KB
$FILE_SIZE_KB = 1000

# Remove existing data
if (Test-Path $BASE_DIR) {
    Write-Host "Removing existing $BASE_DIR..."
    Remove-Item -Path $BASE_DIR -Recurse -Force
}

# Create base directory
New-Item -ItemType Directory -Path $BASE_DIR -Force | Out-Null

Write-Host "Creating test data structure in $BASE_DIR..."

for ($i = 1; $i -le $TOP_LEVEL; $i++) {
    $FOLDER = "$BASE_DIR/folder_$i"
    
    # Create top-level folder
    New-Item -ItemType Directory -Path $FOLDER -Force | Out-Null
    
    Write-Host "Creating files in $FOLDER..."
    
    # Create files in top-level folder
    for ($f = 1; $f -le $FILES_PER_FOLDER; $f++) {
        $filePath = "$FOLDER/file_$f.bin"
        
        # Create a file with random content using fsutil (faster method)
        try {
            & fsutil file createnew $filePath ($FILE_SIZE_KB * 1024) 2>$null | Out-Null
            Write-Host "  Created $filePath ($FILE_SIZE_KB KB)"
        } catch {
            # Fallback method if fsutil fails
            $content = "Sample data for folder $i file $f " * ($FILE_SIZE_KB * 100)
            [System.IO.File]::WriteAllText($filePath, $content)
            Write-Host "  Created $filePath (fallback method)"
        }
    }
    
    # Create subfolders with files
    for ($j = 1; $j -le $SUBFOLDERS; $j++) {
        $SUBFOLDER = "$FOLDER/subfolder_$j"
        
        # Create subfolder
        New-Item -ItemType Directory -Path $SUBFOLDER -Force | Out-Null
        
        Write-Host "Creating files in $SUBFOLDER..."
        
        # Create files in subfolder
        for ($k = 1; $k -le $FILES_PER_FOLDER; $k++) {
            $filePath = "$SUBFOLDER/file_$k.bin"
            
            # Create a file with random content
            try {
                & fsutil file createnew $filePath ($FILE_SIZE_KB * 1024) 2>$null | Out-Null
                Write-Host "    Created $filePath ($FILE_SIZE_KB KB)"
            } catch {
                # Fallback method if fsutil fails
                $content = "Sample data for folder $i subfolder $j file $k " * ($FILE_SIZE_KB * 100)
                [System.IO.File]::WriteAllText($filePath, $content)
                Write-Host "    Created $filePath (fallback method)"
            }
        }
    }
}

Write-Host ""
Write-Host "Test data generation completed!"
Write-Host "Created structure:"
Write-Host "- $TOP_LEVEL top-level folders (folder_1 to folder_$TOP_LEVEL)"
Write-Host "- $SUBFOLDERS subfolders per folder (subfolder_1 to subfolder_$SUBFOLDERS)"
Write-Host "- $FILES_PER_FOLDER files per folder/subfolder"
Write-Host "- Each file is $FILE_SIZE_KB KB"

# Count total files
$totalFiles = $TOP_LEVEL * ($FILES_PER_FOLDER + ($SUBFOLDERS * $FILES_PER_FOLDER))
Write-Host "Total files that should be created: $totalFiles"

# Verify actual file count
$actualFiles = (Get-ChildItem -Path $BASE_DIR -Recurse -File | Measure-Object).Count
Write-Host "Actual files created: $actualFiles"

if ($actualFiles -eq $totalFiles) {
    Write-Host "✅ Success! All files created correctly." -ForegroundColor Green
} else {
    Write-Host "⚠️  Warning: Expected $totalFiles files but created $actualFiles files." -ForegroundColor Yellow
}
