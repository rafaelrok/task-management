const NotificationSystem = {
  eventSource: null,

  init: function () {
    // Don't initialize if notification elements are not present (e.g., on login page)
    if (!document.getElementById("notificationBadge")) {
      return;
    }
    // Connection is now handled by websocket-client.js
    this.loadUnreadCount();
    this.setupBadge();
    // Carregar notificações sticky ao iniciar
    this.loadStickyNotifications();
  },

  /**
   * Carrega e exibe notificações sticky (persistentes) não lidas.
   * Estas notificações aparecem automaticamente e não desaparecem até serem marcadas como lidas.
   */
  loadStickyNotifications: function () {
    fetch("/api/notifications/sticky")
      .then((response) => {
        if (!response.ok) {
          if (response.status === 401) {
            return []; // Não autenticado, retorna lista vazia
          }
          throw new Error(
            "Failed to load sticky notifications: " + response.status
          );
        }
        return response.json();
      })
      .then((notifications) => {
        if (notifications && notifications.length > 0) {
          console.log(
            `📌 Carregando ${notifications.length} notificação(ões) sticky`
          );
          // Mostrar cada notificação sticky com delay para não sobrecarregar
          notifications.forEach((notification, index) => {
            setTimeout(() => {
              this.showToast(notification);
            }, index * 500); // 500ms entre cada toast
          });
        }
      })
      .catch((error) => {
        console.warn("Erro ao carregar notificações sticky:", error.message);
      });
  },

  // SSE connection removed to avoid duplication with WebSocket

  loadUnreadCount: function () {
    fetch("/api/notifications/unread-count")
      .then((response) => {
        if (!response.ok) {
          if (response.status === 401) {
            this.updateBadgeCount(0, false);
            return Promise.reject(new Error("Não autenticado"));
          }
          throw new Error("Failed to load count: " + response.status);
        }
        return response.json();
      })
      .then((count) => {
        this.updateBadgeCount(count, false);
      })
      .catch((error) => {
        console.warn("Badge count erro:", error.message);
      });
  },

  updateBadgeCount: function (value, isIncrement) {
    const badge = document.getElementById("notificationBadge");
    const countSpan = document.getElementById("notificationCount");

    if (!badge || !countSpan) return;

    let currentCount = parseInt(countSpan.innerText || "0");
    if (isNaN(currentCount)) currentCount = 0;

    let newCount = isIncrement ? currentCount + value : value;
    if (newCount < 0) newCount = 0; // Prevent negative count

    countSpan.innerText = newCount;

    if (newCount > 0) {
      badge.classList.remove("d-none");
      countSpan.classList.remove("d-none");
    } else {
      badge.classList.add("d-none");
      countSpan.classList.add("d-none");
    }
  },

  showToast: function (notification) {
    const container = document.getElementById("toastContainer");
    if (!container) return;

    const toastId = "toast-" + notification.id;
    const typeConfig = this.getTypeConfig(notification.type);
    const isDark =
      document.body.classList.contains("dark-theme") ||
      document.documentElement.getAttribute("data-theme") === "dark";

    const toastClass = isDark
      ? "bg-dark text-white border-secondary"
      : "bg-white text-dark";

    // Determina se é uma notificação sticky (da API ou do tipo)
    const isSticky =
      notification.sticky || typeConfig.isSticky || !typeConfig.autohide;
    const stickyClass = isSticky ? "notification-toast-sticky" : "";
    const isPendingType = notification.type === "TASK_PENDING";
    const pendingClass = isPendingType ? "pending" : "";

    // Parse rich message if available (format: Message | Status: X | Priority: Y)
    let message = notification.message;
    let details = "";

    if (message.includes("|")) {
      const parts = message.split("|");
      message = parts[0].trim();
      details = parts
        .slice(1)
        .map(
          (p) =>
            `<span class="badge bg-secondary me-1" style="font-size: 0.7rem;">${p.trim()}</span>`
        )
        .join("");
    }

    // Adiciona indicador sticky se necessário
    const stickyIndicator = isSticky
      ? `
            <div class="notification-requires-action ${pendingClass}">
                <i class="bi bi-pin-angle-fill"></i>
                <span>Esta notificação requer sua ação e não desaparecerá automaticamente</span>
            </div>
        `
      : "";

    // Botões de ação para TASK_TIME_UP
    const isTimeUp = notification.type === "TASK_TIME_UP";
    const actionButtons = isTimeUp
      ? `
            <div class="d-flex gap-2 mt-2">
                <button onclick="NotificationSystem.finishTaskFromNotification(${notification.taskId}, ${notification.id})" 
                        class="btn btn-sm btn-success flex-fill">
                    <i class="bi bi-check-circle"></i> Finalizar
                </button>
                <button onclick="NotificationSystem.cancelTaskFromNotification(${notification.taskId}, ${notification.id})" 
                        class="btn btn-sm btn-danger flex-fill">
                    <i class="bi bi-x-circle"></i> Cancelar
                </button>
            </div>
        `
      : "";

    const toastHtml = `
            <div id="${toastId}" class="toast ${toastClass} ${stickyClass} ${pendingClass}" role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="${
      typeConfig.duration
    }" data-sticky="${isSticky}">
                <div class="toast-header ${typeConfig.headerClass} text-white">
                    <i class="${typeConfig.icon} me-2"></i>
                    <strong class="me-auto">${notification.title}</strong>
                    ${
                      isSticky
                        ? '<span class="notification-sticky-badge ' +
                          pendingClass +
                          '"><i class="bi bi-pin-fill notification-sticky-icon"></i> Fixada</span>'
                        : ""
                    }
                    <small class="${
                      isDark ? "text-light" : "text-muted"
                    } ms-2">${this.formatTime(notification.createdAt)}</small>
                    <button type="button" class="btn-close btn-close-white ms-2" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">
                    <p class="mb-1">${message}</p>
                    ${details ? `<div class="mb-2">${details}</div>` : ""}
                    ${stickyIndicator}
                    ${actionButtons}
                    <div class="mt-2 pt-2 border-top ${
                      isDark ? "border-secondary" : ""
                    } d-flex justify-content-between align-items-center">
                        <a href="/tasks/${
                          notification.taskId
                        }" onclick="NotificationSystem.markAsRead(${
      notification.id
    }, this); return true;" class="btn btn-sm ${
      isDark ? "btn-outline-light" : "btn-light"
    } notification-btn">Ver Tarefa</a>
                        <button onclick="NotificationSystem.markAsRead(${
                          notification.id
                        }, this)" class="btn btn-sm btn-link text-decoration-none ${
      isDark ? "text-light" : "text-muted"
    } notification-mark-read" title="Marcar como lida">
                            <i class="bi bi-check2-all"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;

    const toastWrapper = document.createElement("div");
    toastWrapper.innerHTML = toastHtml;
    const toastElement = toastWrapper.firstElementChild;

    container.appendChild(toastElement);

    const toast = new bootstrap.Toast(toastElement, {
      autohide: !isSticky && typeConfig.autohide,
      delay: typeConfig.duration,
    });

    toast.show();

    // Remove from DOM after hidden
    toastElement.addEventListener("hidden.bs.toast", () => {
      toastElement.remove();
    });
  },

  initBackgroundRefresh: function () {
    // Refresh every 10 seconds
    setInterval(() => {
      // Only refresh if we are on the dashboard to update KPIs
      if (window.location.pathname === "/" || window.location.pathname === "") {
        this.refreshDashboardKPIs();
      }
      // Always refresh badge count
      this.loadUnreadCount();
    }, 10000);
  },

  refreshDashboardKPIs: function () {
    // We can fetch the dashboard HTML and parse out the KPI values
    // This is a simple way to refresh without a dedicated API
    fetch(window.location.href)
      .then((response) => response.text())
      .then((html) => {
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, "text/html");

        // Update KPIs
        const kpiSelectors = [
          ".kpi-todo .kpi-value",
          ".kpi-progress .kpi-value",
          ".kpi-done .kpi-value",
          ".kpi-cancelled .kpi-value",
        ];
        kpiSelectors.forEach((selector) => {
          const newVal = doc.querySelector(selector);
          const currVal = document.querySelector(selector);
          if (newVal && currVal && newVal.innerText !== currVal.innerText) {
            currVal.innerText = newVal.innerText;
            // Add a subtle flash effect
            currVal.style.color = "var(--primary)";
            setTimeout(() => (currVal.style.color = ""), 500);
          }
        });

        // Update Priority Cards
        const prioritySelectors = [
          ".priority-low .priority-value",
          ".priority-medium .priority-value",
          ".priority-high .priority-value",
          ".priority-urgent .priority-value",
        ];
        prioritySelectors.forEach((selector) => {
          const newVal = doc.querySelector(selector);
          const currVal = document.querySelector(selector);
          if (newVal && currVal && newVal.innerText !== currVal.innerText) {
            currVal.innerText = newVal.innerText;
          }
        });
      })
      .catch((err) => console.error("Background refresh failed", err));
  },

  refreshTaskRelatedSections: function (notification) {
    // Apenas tenta atualizar se estamos na dashboard
    if (window.location.pathname !== "/" && window.location.pathname !== "")
      return;
    // Atualizar seção de tarefas ativas se função global disponível
    if (typeof window.refreshActiveTasks === "function") {
      window.refreshActiveTasks();
    }
    // Recarregar KPIs e tabelas específicas via fetch parcial
    fetch(window.location.href, { cache: "no-store" })
      .then((r) => r.text())
      .then((html) => {
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, "text/html");
        const kpiMap = {
          ".kpi-todo .kpi-value": null,
          ".kpi-progress .kpi-value": null,
          ".kpi-done .kpi-value": null,
          ".kpi-cancelled .kpi-value": null,
          ".priority-low .priority-value": null,
          ".priority-medium .priority-value": null,
          ".priority-high .priority-value": null,
          ".priority-urgent .priority-value": null,
        };
        Object.keys(kpiMap).forEach((sel) => {
          const newEl = doc.querySelector(sel);
          const curEl = document.querySelector(sel);
          if (newEl && curEl && newEl.innerText !== curEl.innerText) {
            curEl.innerText = newEl.innerText;
            curEl.style.transition = "color .3s";
            curEl.style.color = "var(--primary)";
            setTimeout(() => (curEl.style.color = ""), 600);
          }
        });
        // Atualizar tabela Overdue
        const newOverdueBody = doc.querySelector("#overdueTableBody");
        const curOverdueBody = document.querySelector("#overdueTableBody");
        if (newOverdueBody && curOverdueBody) {
          curOverdueBody.innerHTML = newOverdueBody.innerHTML;
        }
        // Atualizar tabela Due Today
        const newDueTodayBody = doc.querySelector("#dueTodayTableBody");
        const curDueTodayBody = document.querySelector("#dueTodayTableBody");
        if (newDueTodayBody && curDueTodayBody) {
          curDueTodayBody.innerHTML = newDueTodayBody.innerHTML;
        }
      })
      .catch((err) => console.warn("Falha ao atualizar KPIs/tabelas", err));
  },

  getTypeConfig: function (type) {
    // Default config
    let config = {
      icon: "bi bi-info-circle",
      headerClass: "bg-primary",
      duration: 10000,
      autohide: true,
    };

    switch (type) {
      case "TASK_STARTED":
      case "TASK_RESUMED":
      case "TASK_CREATED":
        config.icon = "bi bi-play-circle";
        config.headerClass = "bg-info";
        break;
      case "TASK_UPDATED":
        config.icon = "bi bi-pencil-square";
        config.headerClass = "bg-primary";
        break;
      case "TASK_PAUSED":
        config.icon = "bi bi-pause-circle";
        config.headerClass = "bg-warning";
        break;
      case "TASK_FINISHED":
        config.icon = "bi bi-check-circle";
        config.headerClass = "bg-success";
        break;
      case "TASK_OVERDUE":
      case "TASK_ERROR":
        config.icon = "bi bi-exclamation-triangle";
        config.headerClass = "bg-danger";
        config.autohide = false; // Sticky
        break;
      case "TASK_PENDING":
        config.icon = "bi bi-clock-history";
        config.headerClass = "bg-primary";
        config.autohide = false; // Sticky - requer ação do usuário
        config.isSticky = true;
        break;
      case "TASK_STARTING_SOON":
        config.icon = "bi bi-alarm";
        config.headerClass = "bg-info";
        config.duration = 30000; // 30 segundos para alerta de início próximo
        break;
      case "TASK_DUE_SOON":
        config.icon = "bi bi-hourglass-split";
        config.headerClass = "bg-warning";
        config.duration = 20000; // 20 segundos para alerta de prazo
        break;
      case "TASK_TODO":
        config.icon = "bi bi-list-task";
        config.headerClass = "bg-secondary";
        break;
    }

    if (type === "TASK_TIME_UP") {
      config.autohide = false;
      config.headerClass = "bg-danger";
      config.icon = "bi bi-alarm";
    }

    return config;
  },

  formatTime: function (dateString) {
    const date = new Date(dateString);
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  },

  setupBadge: function () {
    const dropdown = document.getElementById("notificationDropdown");
    if (dropdown) {
      dropdown.addEventListener("show.bs.dropdown", () => {
        this.loadRecentNotifications();
      });
    }
  },

  addNotificationToDropdown: function (notification) {
    const list = document.getElementById("notificationList");
    if (!list) return;

    // Remove "No notifications" or "Loading" item if present
    const emptyItem = list.querySelector(".empty-notification");
    if (emptyItem) emptyItem.remove();

    const isDark =
      document.body.classList.contains("dark-theme") ||
      document.documentElement.getAttribute("data-theme") === "dark";
    const itemClass = isDark ? "text-light hover-dark" : "text-dark";

    const item = document.createElement("li");
    item.innerHTML = `
            <div class="dropdown-item d-flex align-items-center gap-2 py-2 ${itemClass}">
                <div class="flex-shrink-0">
                    <i class="${
                      this.getTypeConfig(notification.type).icon
                    } text-${this.getTypeConfig(
      notification.type
    ).headerClass.replace("bg-", "")}"></i>
                </div>
                <div class="flex-grow-1" style="min-width: 0;">
                    <a href="/tasks/${
                      notification.taskId
                    }" class="text-decoration-none ${itemClass} notification-btn">
                        <h6 class="mb-0 small text-truncate">${
                          notification.title
                        }</h6>
                        <small class="${
                          isDark ? "text-secondary" : "text-muted"
                        } text-truncate d-block" style="font-size: 0.75rem;">${
      notification.message
    }</small>
                    </a>
                </div>
                <div class="flex-shrink-0 ms-2">
                    <button onclick="NotificationSystem.markAsRead(${
                      notification.id
                    }, this)" class="btn btn-sm btn-link text-success p-0 notification-mark-read" title="Marcar como lida">
                        <i class="bi bi-check2"></i>
                    </button>
                </div>
            </div>
        `;

    // Insert at top
    if (list.firstChild) {
      list.insertBefore(item, list.firstChild);
    } else {
      list.appendChild(item);
    }

    // Limit to 5 items
    const items = list.querySelectorAll("li");
    if (items.length > 5) {
      // Remove the last one (oldest)
      list.removeChild(list.lastElementChild);
    }
  },

  loadRecentNotifications: function () {
    const list = document.getElementById("notificationList");
    if (!list) return;

    if (list.children.length === 0) {
      list.innerHTML =
        '<li class="dropdown-item text-center text-muted small empty-notification">Carregando...</li>';
    }

    fetch("/api/notifications?page=0&size=5")
      .then((response) => {
        if (!response.ok) {
          if (response.status === 401) {
            list.innerHTML =
              '<li class="dropdown-item text-center text-muted small empty-notification">Faça login para ver notificações</li>';
            return Promise.reject(new Error("Não autenticado"));
          }
          if (response.status === 403) {
            list.innerHTML =
              '<li class="dropdown-item text-center text-muted small empty-notification">Acesso não permitido</li>';
            return Promise.reject(new Error("Forbidden"));
          }
          list.innerHTML =
            '<li class="dropdown-item text-center text-danger small empty-notification">Erro ao carregar (' +
            response.status +
            ")</li>";
          return Promise.reject(new Error("Erro HTTP " + response.status));
        }
        return response.json();
      })
      .then((data) => {
        list.innerHTML = "";
        const notifications = data.content;
        if (notifications.length === 0) {
          list.innerHTML =
            '<li class="dropdown-item text-center text-muted small empty-notification">Nenhuma notificação</li>';
          return;
        }
        notifications.forEach((notification) => {
          this.addNotificationToDropdown(notification);
        });
      })
      .catch((err) => {
        // já tratado acima, só logar
        console.warn("Falha ao carregar notificações:", err.message);
      });
  },

  markAsRead: function (id, btnElement) {
    // Prevent click propagation if inside a link
    if (event) event.stopPropagation();
    if (event) event.preventDefault();

    fetch(`/api/notifications/${id}/read`, {
      method: "POST",
    })
      .then((response) => {
        if (response.ok) {
          // Update badge count globally (decrement 1)
          this.updateBadgeCount(-1, true);

          // Fechar o toast associado se existir
          const toastElement = document.getElementById(`toast-${id}`);
          if (toastElement) {
            const bsToast = bootstrap.Toast.getInstance(toastElement);
            if (bsToast) {
              bsToast.hide();
            } else {
              toastElement.remove();
            }
          }

          if (btnElement) {
            // Detect context: dropdown item vs full list item
            const dropdownItem = btnElement.closest(".dropdown-item");
            const fullListItem = btnElement.closest(".list-group-item");

            if (dropdownItem) {
              // Fade then remove from dropdown
              dropdownItem.style.opacity = "0.5";
              setTimeout(() => {
                dropdownItem.remove();
                const list = document.getElementById("notificationList");
                if (list && list.children.length === 0) {
                  list.innerHTML =
                    '<li class="dropdown-item text-center text-muted small empty-notification">Nenhuma notificação</li>';
                }
              }, 400);
            } else if (fullListItem) {
              // Remove badge "Nova" e ajustar estilos sem remover item da lista completa
              const newBadge = fullListItem.querySelector(".badge.bg-primary");
              if (newBadge) newBadge.remove();
              fullListItem.classList.remove("bg-light");
              // Visual feedback rápido no botão
              btnElement.classList.add("disabled");
              btnElement.setAttribute("aria-disabled", "true");
              btnElement.innerHTML = '<i class="bi bi-check2"></i>'; // icone confirmado
              // Opcional: desabilitar clique para evitar chamada repetida
              btnElement.onclick = null;
            }
          }
        } else {
          console.error("Failed to mark as read:", response.status);
        }
      })
      .catch((err) => console.error("Error marking as read:", err));
  },

  markAllAsRead: function () {
    const btn = document.getElementById("markAllReadBtn");
    if (!btn) return;

    // Disable button during request
    btn.disabled = true;
    const originalHtml = btn.innerHTML;
    btn.innerHTML =
      '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Processando...';

    fetch("/api/notifications/read-all", {
      method: "POST",
    })
      .then((response) => {
        if (response.ok) {
          // Update badge to 0
          this.updateBadgeCount(0, false);

          // Remove all "Nova" badges and bg-light from list items
          const fullList = document.getElementById("fullNotificationList");
          if (fullList) {
            const items = fullList.querySelectorAll(".list-group-item");
            items.forEach((item) => {
              const badge = item.querySelector(".badge.bg-primary");
              if (badge) badge.remove();
              item.classList.remove("bg-light");

              // Hide the mark as read button for each item
              const markReadBtn = item.querySelector(".btn-light.text-primary");
              if (markReadBtn) {
                markReadBtn.style.display = "none";
              }
            });
          }

          // Show success feedback
          btn.innerHTML = '<i class="bi bi-check-all me-1"></i>Todas marcadas!';
          btn.classList.remove("btn-outline-primary");
          btn.classList.add("btn-success");

          // Reset button after 2 seconds
          setTimeout(() => {
            btn.innerHTML = originalHtml;
            btn.classList.remove("btn-success");
            btn.classList.add("btn-outline-primary");
            btn.disabled = false;
          }, 2000);
        } else if (response.status === 401) {
          btn.innerHTML = originalHtml;
          btn.disabled = false;
          alert("Sessão expirada. Por favor, faça login novamente.");
        } else {
          btn.innerHTML = originalHtml;
          btn.disabled = false;
          console.error("Failed to mark all as read:", response.status);
          alert("Erro ao marcar notificações como lidas. Tente novamente.");
        }
      })
      .catch((err) => {
        btn.innerHTML = originalHtml;
        btn.disabled = false;
        console.error("Error marking all as read:", err);
        alert("Erro ao marcar notificações como lidas. Tente novamente.");
      });
  },

  setupMarkAllButton: function () {
    const btn = document.getElementById("markAllReadBtn");
    if (btn) {
      btn.addEventListener("click", () => this.markAllAsRead());
    }
  },

  /**
   * Finaliza uma tarefa diretamente da notificação
   */
  finishTaskFromNotification: async function (taskId, notificationId) {
    try {
      const response = await fetch(`/api/tasks/${taskId}/status?status=DONE`, {
        method: "PATCH",
      });

      if (response.ok) {
        // Mark notification as read
        this.markAsRead(notificationId);

        // Close the toast
        const toastElement = document.getElementById(`toast-${notificationId}`);
        if (toastElement) {
          const bsToast = bootstrap.Toast.getInstance(toastElement);
          if (bsToast) bsToast.hide();
        }

        // Show success message
        alert("✅ Tarefa finalizada com sucesso!");

        // Refresh dashboard if available
        if (typeof window.refreshDashboard === "function") {
          window.refreshDashboard();
        }
      } else {
        alert("❌ Erro ao finalizar tarefa. Tente novamente.");
      }
    } catch (error) {
      console.error("Error finishing task:", error);
      alert("❌ Erro ao finalizar tarefa. Tente novamente.");
    }
  },

  /**
   * Cancela uma tarefa com confirmação
   */
  cancelTaskFromNotification: async function (taskId, notificationId) {
    // Show confirmation modal
    const confirmed = confirm(
      "⚠️ Atenção!\n\n" +
        "Você tem certeza que deseja CANCELAR esta tarefa?\n\n" +
        "Esta ação NÃO PODE ser revertida e a tarefa será marcada como CANCELADA permanentemente.\n\n" +
        "Deseja continuar?"
    );

    if (!confirmed) {
      return;
    }

    try {
      const response = await fetch(
        `/api/tasks/${taskId}/status?status=CANCELLED`,
        {
          method: "PATCH",
        }
      );

      if (response.ok) {
        // Mark notification as read
        this.markAsRead(notificationId);

        // Close the toast
        const toastElement = document.getElementById(`toast-${notificationId}`);
        if (toastElement) {
          const bsToast = bootstrap.Toast.getInstance(toastElement);
          if (bsToast) bsToast.hide();
        }

        // Show success message
        alert("✅ Tarefa cancelada com sucesso!");

        // Refresh dashboard if available
        if (typeof window.refreshDashboard === "function") {
          window.refreshDashboard();
        }
      } else {
        alert("❌ Erro ao cancelar tarefa. Tente novamente.");
      }
    } catch (error) {
      console.error("Error cancelling task:", error);
      alert("❌ Erro ao cancelar tarefa. Tente novamente.");
    }
  },
};

document.addEventListener("DOMContentLoaded", () => {
  NotificationSystem.init();
  NotificationSystem.setupMarkAllButton();
});

// Expose handler for WebSocket notifications
window.handleWebSocketNotification = function (data) {
  console.log("[NotificationSystem] Recebida via WebSocket:", data);
  if (
    NotificationSystem &&
    typeof NotificationSystem.showToast === "function"
  ) {
    // Show toast notification
    NotificationSystem.showToast(data);

    // Update badge count (increment by 1)
    NotificationSystem.updateBadgeCount(1, true);

    // Add to dropdown
    NotificationSystem.addNotificationToDropdown(data);

    // Refresh task-related sections
    if (typeof NotificationSystem.refreshTaskRelatedSections === "function") {
      NotificationSystem.refreshTaskRelatedSections(data);
    }

    console.log("[NotificationSystem] ✅ Notificação processada com sucesso");
  } else {
    console.error(
      "[NotificationSystem] Sistema de notificações não disponível"
    );
  }
};
