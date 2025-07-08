#!/bin/bash

# Base directory where to create the structure
BASE_DIR="dc-data"

# Number of top-level folders
TOP_LEVEL=5

# Number of subfolders per folder
SUBFOLDERS=5

# Number of files per folder
FILES_PER_FOLDER=3

# Size of each file in MB
FILE_SIZE_MB=10

mkdir -p "$BASE_DIR"

for i in $(seq 1 $TOP_LEVEL); do
  FOLDER="$BASE_DIR/folder_$i"
  mkdir -p "$FOLDER"

  # Create files in top-level folder
  for f in $(seq 1 $FILES_PER_FOLDER); do
    dd if=/dev/urandom of="$FOLDER/file_$f.bin" bs=1M count=$FILE_SIZE_MB status=none
  done

  # Create subfolders with files
  for j in $(seq 1 $SUBFOLDERS); do
    SUBFOLDER="$FOLDER/subfolder_$j"
    mkdir -p "$SUBFOLDER"

    for k in $(seq 1 $FILES_PER_FOLDER); do
      dd if=/dev/urandom of="$SUBFOLDER/file_$k.bin" bs=1M count=$FILE_SIZE_MB status=none
    done
  done
done
