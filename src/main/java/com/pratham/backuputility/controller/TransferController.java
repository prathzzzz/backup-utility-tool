package com.pratham.backuputility.controller;

import com.pratham.backuputility.entity.TransferLog;
import com.pratham.backuputility.repository.TransferLogRepository;
import com.pratham.backuputility.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class TransferController {

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransferLogRepository transferLogRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        // Get only last 10 logs for better performance
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("transferredAt").descending());
        List<TransferLog> logs = transferLogRepository.findAll(pageable).getContent();

        model.addAttribute("logs", logs);
        // Use incremental status instead of old sync status
        model.addAttribute("syncStatus", createSimpleSyncStatus());
        model.addAttribute("isTransferRunning", transferService.isTransferInProgress());

        return "dashboard";
    }

    /**
     * Create a simple sync status for UI display using incremental logic
     */
    private java.util.Map<String, String> createSimpleSyncStatus() {
        java.util.Map<String, String> status = new java.util.LinkedHashMap<>();
        try {
            java.util.Map<String, String> incrementalStatus = transferService.getSyncStatus();

            if (incrementalStatus.containsKey("error")) {
                status.put("System Status", incrementalStatus.get("error"));
                return status;
            }

            String syncedFiles = incrementalStatus.getOrDefault("synced_files", "0");
            String outOfSyncFiles = incrementalStatus.getOrDefault("out_of_sync_files", "0");
            String totalSnapshots = incrementalStatus.getOrDefault("total_snapshots", "0");

            // Provide clear status information
            int outOfSyncCount = Integer.parseInt(outOfSyncFiles);
            if (outOfSyncCount > 0) {
                status.put("Files needing sync", outOfSyncFiles + " file" + (outOfSyncCount > 1 ? "s" : "") + " need synchronization");
            } else {
                status.put("Sync Status", "All files are synchronized");
            }

            status.put("Synchronized files", syncedFiles + " file" + (Integer.parseInt(syncedFiles) > 1 ? "s" : "") + " in sync");
            status.put("Transfer Mode", "Incremental (Block-level)");
            status.put("Tracking", totalSnapshots + " snapshots maintained");

        } catch (Exception e) {
            status.put("Error", "Failed to get sync status: " + e.getMessage());
        }

        return status;
    }




}
