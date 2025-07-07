// ==================== TRANSFER MANAGER ==================== //
const transferManager = {
  start(direction, mode = "incremental") {
    if (AppState.isTransferInProgress) {
      UIHelpers.showError("Transfer already in progress");
      return;
    }

    // Convert direction to backend format
    const backendDirection = direction === 'DC' ? 'DC_TO_DR' : 'DR_TO_DC';

    const modeText = mode === "full" ? "full" : "incremental";
    UIHelpers.showLoading(`Starting ${modeText} transfer...`);
    AppState.isTransferInProgress = true;

    $.post("/api/incremental/transfer", {
      direction: backendDirection,
      mode: mode,
    })
      .done((response) => this.handleSuccess(response))
      .fail((xhr) => this.handleError(xhr))
      .always(() => {
        AppState.isTransferInProgress = false;
      });
  },

  handleSuccess(response) {
    if (response && response.length > 0) {
      // Count actual file results (exclude summary messages)
      const fileResults = response.filter(
        (msg) =>
          msg.startsWith("✓") ||
          msg.startsWith("○") ||
          msg.startsWith("✗")
      );
      const fileCount = fileResults.length;

      UIHelpers.hideLoading(
        `Transfer completed: ${fileCount} file${
          fileCount !== 1 ? "s" : ""
        } processed`
      );
      this.displayResults(response);
    } else {
      UIHelpers.hideLoading("Transfer completed: No changes detected");
    }

    setTimeout(() => window.location.reload(), 3000);
  },

  handleError(xhr) {
    UIHelpers.showError(`Failed to start transfer: ${xhr.responseText}`);
  },

  displayResults(results) {
    let content =
      '<h6>Transfer Results:</h6><ul class="mb-0">';
    results.forEach((result) => {
      content += `<li><small>${result}</small></li>`;
    });
    content += "</ul>";

    UIHelpers.showAlert(content, "info", 10000);
  },
};
