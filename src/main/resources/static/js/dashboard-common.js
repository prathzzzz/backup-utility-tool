// ==================== COMMON UTILITIES ==================== //

// Application State Management
const AppState = {
  isTransferInProgress: false,
  autoRefreshEnabled: true,
  wsConnection: null,
  lastUpdated: new Date()
};

// UI Helper Functions
const UIHelpers = {
  showAlert(message, type = "info", duration = 5000) {
    const alertSection = $("#alertSection");
    const alertId = `alert-${Date.now()}`;

    const alertHtml = `
      <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show" role="alert">
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
      </div>
    `;

    alertSection.append(alertHtml);

    if (duration > 0) {
      setTimeout(() => $(`#${alertId}`).alert('close'), duration);
    }
  },

  showError(message, duration = 8000) {
    this.showAlert(`<i class="bi bi-exclamation-triangle"></i> ${message}`, "danger", duration);
  },

  showSuccess(message, duration = 5000) {
    this.showAlert(`<i class="bi bi-check-circle"></i> ${message}`, "success", duration);
  },

  showLoading(message = "Loading...") {
    const progressSection = $("#progressSection");
    const progressStatus = $("#progressStatus");

    progressStatus.html(`<div class="spinner me-2"></div>${message}`);
    progressSection.show();
  },

  hideLoading(finalMessage = null) {
    const progressSection = $("#progressSection");

    if (finalMessage) {
      this.showSuccess(finalMessage);
    }

    setTimeout(() => progressSection.hide(), 1000);
  },

  updateProgress(percentage, message = "") {
    const progressBar = $("#progressBar");
    const progressText = $("#progressText");
    const progressStatus = $("#progressStatus");

    progressBar.css("width", `${percentage}%`);
    progressText.text(`${percentage}% Complete`);

    if (message) {
      progressStatus.text(message);
    }
  },

  updateMetrics(metrics) {
    const container = $("#progressMetrics");
    container.empty();

    Object.entries(metrics).forEach(([key, value]) => {
      const metricHtml = `
        <div class="metric-card">
          <div class="metric-value">${value}</div>
          <div class="metric-label">${key}</div>
        </div>
      `;
      container.append(metricHtml);
    });
  },

  getElements() {
    return {
      checkBtn: $("#checkSyncBtn"),
      autoRefresh: $("#autoRefresh"),
      lastUpdated: $("#lastUpdated"),
      connectionStatus: $("#connectionStatus")
    };
  },

  updateLastUpdated() {
    const { lastUpdated } = this.getElements();
    AppState.lastUpdated = new Date();
    lastUpdated.text(AppState.lastUpdated.toLocaleTimeString());
  },

  updateConnectionStatus(connected) {
    const { connectionStatus } = this.getElements();

    if (connected) {
      connectionStatus.html('<i class="bi bi-wifi"></i> <span>Connected</span>');
      connectionStatus.removeClass('text-danger').addClass('text-success');
    } else {
      connectionStatus.html('<i class="bi bi-wifi-off"></i> <span>Disconnected</span>');
      connectionStatus.removeClass('text-success').addClass('text-danger');
    }
  }
};

// Auto-refresh Management
const AutoRefresh = {
  intervalId: null,

  start() {
    if (this.intervalId) return;

    this.intervalId = setInterval(() => {
      if (AppState.autoRefreshEnabled && !AppState.isTransferInProgress) {
        this.refreshData();
      }
    }, 10000); // Refresh every 10 seconds
  },

  stop() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  },

  refreshData() {
    // Refresh sync status silently
    $.get("/api/incremental/progress")
      .done((data) => {
        if (data.inProgress !== AppState.isTransferInProgress) {
          AppState.isTransferInProgress = data.inProgress;
          window.location.reload();
        }
      })
      .fail(() => {
        UIHelpers.updateConnectionStatus(false);
      });
  }
};

// Initialize on document ready
$(document).ready(function() {
  // Initialize Bootstrap dropdowns
  var dropdownElementList = [].slice.call(document.querySelectorAll('.dropdown-toggle'));
  var dropdownList = dropdownElementList.map(function (dropdownToggleEl) {
    return new bootstrap.Dropdown(dropdownToggleEl);
  });

  // Initialize auto-refresh
  AutoRefresh.start();

  // Handle auto-refresh toggle
  $("#autoRefresh").on("change", function() {
    AppState.autoRefreshEnabled = $(this).is(":checked");
    if (AppState.autoRefreshEnabled) {
      AutoRefresh.start();
    } else {
      AutoRefresh.stop();
    }
  });

  // Initialize connection status
  UIHelpers.updateConnectionStatus(true);
  UIHelpers.updateLastUpdated();

  // Update last updated time every minute
  setInterval(() => UIHelpers.updateLastUpdated(), 60000);
});

// Handle page visibility change
document.addEventListener("visibilitychange", function() {
  if (document.hidden) {
    AutoRefresh.stop();
  } else {
    if (AppState.autoRefreshEnabled) {
      AutoRefresh.start();
    }
  }
});
