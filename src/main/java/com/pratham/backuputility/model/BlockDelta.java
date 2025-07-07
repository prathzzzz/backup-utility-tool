package com.pratham.backuputility.model;

/**
 * Represents a single block change in a file for incremental transfers.
 */
public class BlockDelta {
    private long blockIndex;
    private byte[] data;
    private String hash;

    public BlockDelta() {}

    public BlockDelta(long blockIndex, byte[] data, String hash) {
        this.blockIndex = blockIndex;
        this.data = data;
        this.hash = hash;
    }

    // Getters and setters
    public long getBlockIndex() { return blockIndex; }
    public void setBlockIndex(long blockIndex) { this.blockIndex = blockIndex; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
