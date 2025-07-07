#!/bin/bash

echo "=== DC Directory Files ==="
find /home/pratham/Documents/backup-utility/dc-data -type f | while read file; do
    rel_path=${file#/home/pratham/Documents/backup-utility/dc-data/}
    echo "DC: $rel_path"
done

echo ""
echo "=== DR Directory Files ==="
find /home/pratham/Documents/backup-utility/dr-data -type f | while read file; do
    rel_path=${file#/home/pratham/Documents/backup-utility/dr-data/}
    echo "DR: $rel_path"
done

echo ""
echo "=== Summary ==="
dc_count=$(find /home/pratham/Documents/backup-utility/dc-data -type f | wc -l)
dr_count=$(find /home/pratham/Documents/backup-utility/dr-data -type f | wc -l)
echo "DC files: $dc_count"
echo "DR files: $dr_count"
echo "Missing in DR: $((dc_count - dr_count))"
