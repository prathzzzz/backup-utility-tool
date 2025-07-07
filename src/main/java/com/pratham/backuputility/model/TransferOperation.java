package com.pratham.backuputility.model;

/**
 * Represents transfer operation parameters
 */
public class TransferOperation {
    public enum Direction {
        DC_TO_DR, DR_TO_DC
    }

    public enum Mode {
        INCREMENTAL, FULL
    }

    private final Direction direction;
    private final Mode mode;
    private final String sourcePathStr;
    private final String targetPathStr;

    public TransferOperation(Direction direction, Mode mode, String sourcePathStr, String targetPathStr) {
        this.direction = direction;
        this.mode = mode;
        this.sourcePathStr = sourcePathStr;
        this.targetPathStr = targetPathStr;
    }

    public Direction getDirection() { return direction; }
    public Mode getMode() { return mode; }
    public String getSourcePathStr() { return sourcePathStr; }
    public String getTargetPathStr() { return targetPathStr; }

    public boolean isDcToDr() { return direction == Direction.DC_TO_DR; }
    public boolean isFullMode() { return mode == Mode.FULL; }

    public String getOperationDescription() {
        String modeStr = (mode == Mode.FULL) ? "Full" : "Incremental";
        String directionStr = isDcToDr() ? "backup (DC → DR)" : "recovery (DR → DC)";
        return String.format("%s %s", modeStr, directionStr);
    }

    public static Direction parseDirection(String directionStr) {
        if (directionStr == null) {
            throw new IllegalArgumentException("Direction cannot be null");
        }

        switch (directionStr.toUpperCase()) {
            case "DC_TO_DR":
            case "DC-TO-DR":
            case "BACKUP":
                return Direction.DC_TO_DR;
            case "DR_TO_DC":
            case "DR-TO-DC":
            case "RECOVERY":
            case "RESTORE":
                return Direction.DR_TO_DC;
            default:
                throw new IllegalArgumentException("Invalid direction: " + directionStr);
        }
    }

    public static Mode parseMode(String modeStr) {
        if (modeStr == null) {
            return Mode.INCREMENTAL; // Default
        }

        switch (modeStr.toLowerCase()) {
            case "full":
                return Mode.FULL;
            case "incremental":
            case "inc":
                return Mode.INCREMENTAL;
            default:
                throw new IllegalArgumentException("Invalid mode: " + modeStr);
        }
    }
}
