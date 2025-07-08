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

    // Show progress section immediately
    $("#progressSection").show();
    $("#progressStatus").text(`Starting ${modeText} transfer...`);
    $("#progressBar").css("width", "0%");
    $("#progressText").text("0% Complete");

    $.post("/api/incremental/transfer", {
      direction: backendDirection,
      mode: mode,
    })
      .done((response) => this.handleSuccess(response, modeText))
      .fail((xhr) => this.handleError(xhr))
      .always(() => {
        // Don't set to false immediately, let WebSocket handle it
        // AppState.isTransferInProgress = false;
      });
  },

  handleSuccess(response, modeText) {
    if (response && response.length > 0) {
      // Count actual file results (exclude summary messages)
      const fileResults = response.filter(
        (msg) =>
          msg.startsWith("✓") ||
          msg.startsWith("○") ||
          msg.startsWith("✗")
      );
      const fileCount = fileResults.length;

      console.log(`${modeText} transfer completed: ${fileCount} file${fileCount !== 1 ? "s" : ""} processed`);
      this.displayResults(response);
    } else {
      console.log(`${modeText} transfer completed: No changes detected`);
    }
  },

  handleError(xhr) {
    AppState.isTransferInProgress = false;
    $("#progressSection").hide();
    UIHelpers.showError(`Failed to start transfer: ${xhr.responseText}`);
  },

  displayResults(results) {
    let content = '<h6>Transfer Results:</h6><ul class="mb-0">';
    results.forEach((result) => {
      content += `<li><small>${result}</small></li>`;
    });
    content += "</ul>";

    UIHelpers.showAlert(content, "info", 10000);
  },
};
