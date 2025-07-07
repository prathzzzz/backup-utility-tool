package com.pratham.backuputility.controller;

import com.pratham.backuputility.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/incremental")
public class IncrementalTransferController {

    @Autowired
    private TransferService transferService;

    /**
     * Perform incremental transfer
     */
    @PostMapping("/transfer")
    @ResponseBody
    public ResponseEntity<?> performIncrementalTransfer(
            @RequestParam String direction,
            @RequestParam(required = false) String mode) {
        try {
            if (transferService.isTransferInProgress()) {
                return ResponseEntity.badRequest().body("Transfer already in progress");
            }

            List<String> results = transferService.performTransfer(direction, mode);
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Transfer failed: " + e.getMessage());
        }
    }

    /**
     * Get incremental sync status
     */
    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getIncrementalSyncStatus() {
        try {
            Map<String, String> status = transferService.getSyncStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if incremental transfer is in progress
     */
    @GetMapping("/progress")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkProgress() {
        return ResponseEntity.ok(Map.of("inProgress", transferService.isTransferInProgress()));
    }

    /**
     * Get detailed file sync status for UI table
     */
    @GetMapping("/detailed-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDetailedSyncStatus() {
        try {
            Map<String, Object> detailedStatus = transferService.getDetailedSyncStatus();
            return ResponseEntity.ok(detailedStatus);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
