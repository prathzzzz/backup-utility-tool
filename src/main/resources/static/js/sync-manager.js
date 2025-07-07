// ==================== SYNC MANAGER ==================== //
const syncManager = {
  checkStatus() {
    const { checkBtn } = UIHelpers.getElements();

    checkBtn.prop("disabled", true).html('<div class="spinner me-2"></div>Checking...');

    $.get("/api/incremental/detailed-status")
      .done((status) => {
        checkBtn.prop("disabled", false).text("Check Sync Status");

        if (status.error) {
          UIHelpers.showError(`Sync check failed: ${status.error}`);
          return;
        }

        // Display comprehensive sync status
        const content = `
          <div class="row">
            <h6>Sync Status Summary:</h6>
            <div class="row mb-3">
              <div class="col-md-3"><strong>Total Files:</strong> ${status.total_snapshots || 0}</div>
              <div class="col-md-3"><strong>Synced Files:</strong> ${status.synced_files || 0}</div>
              <div class="col-md-3"><strong>Out of Sync:</strong> ${status.out_of_sync_files || 0}</div>
              <div class="col-md-3"><strong>Mode:</strong> ${status.mode || 'N/A'}</div>
            </div>
          </div>
        `;

        const alertType = (status.out_of_sync_files || 0) > 0 ? "warning" : "success";
        UIHelpers.showAlert(content, alertType, 8000);

        // Update the sync status table
        this.updateSyncTable(status.files || {});
      })
      .fail((xhr) => {
        checkBtn.prop("disabled", false).text("Check Sync Status");
        UIHelpers.showError(`Failed to check sync status: ${xhr.responseText}`);
      });
  },

  updateSyncTable(files) {
    const tbody = $("#syncTableBody");
    tbody.empty();

    if (Object.keys(files).length === 0) {
      tbody.append(`
        <tr>
          <td colspan="3" class="text-center text-muted">
            <em><i class="bi bi-check-circle text-success"></i> All files are in sync</em>
          </td>
        </tr>
      `);
      return;
    }

    // Display files with status
    Object.entries(files).forEach(([filePath, status]) => {
      const statusClass = this.getStatusClass(status);
      const priority = status === 'SYNCED' ? 'Normal' : 'High';
      const priorityClass = status === 'SYNCED' ? '' : 'priority-high';

      tbody.append(`
        <tr class="${priorityClass}">
          <td><code>${filePath}</code></td>
          <td><span class="status-badge ${statusClass}">${status}</span></td>
          <td><small class="text-muted">${priority}</small></td>
        </tr>
      `);
    });
  },

  getStatusClass(status) {
    switch (status) {
      case 'SYNCED':
        return 'badge-matched';
      case 'MISSING_IN_DR':
        return 'badge-missing';
      case 'MISSING_IN_DC':
        return 'badge-extra';
      case 'MISMATCH':
        return 'badge-mismatch';
      default:
        return 'badge-mismatch';
    }
  }
};
