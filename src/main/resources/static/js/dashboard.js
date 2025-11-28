// Dashboard JS: Theme/Sidebar + Active Tasks + Task Modal
(function () {
  "use strict";
  const root = document.documentElement;
  const sidebar = document.getElementById("sidebar");
  const toggle = document.getElementById("sidebarToggle");
  const themeSwitch = document.getElementById("themeSwitch");

  if (toggle && sidebar) {
    const collapsed = localStorage.getItem("sidebarCollapsed") === "true";
    if (collapsed) sidebar.classList.add("collapsed");
    toggle.addEventListener("click", () => {
      sidebar.classList.toggle("collapsed");
      localStorage.setItem(
        "sidebarCollapsed",
        sidebar.classList.contains("collapsed")
      );
    });
  }

  if (themeSwitch) {
    const saved = localStorage.getItem("theme") || "light";
    applyTheme(saved);

    themeSwitch.addEventListener("change", (e) => {
      const next = e.target.checked ? "dark" : "light";
      applyTheme(next);
      localStorage.setItem("theme", next);
    });

    function applyTheme(t) {
      root.setAttribute("data-theme", t);
      themeSwitch.checked = t === "dark";
    }
  }

  if (window.bootstrap) {
    const triggers = [].slice.call(
      document.querySelectorAll('[data-bs-toggle="tooltip"]')
    );
    triggers.map((el) => new bootstrap.Tooltip(el));
  }
})();

// Toast notification utility
function showToast(cls, msg, delay = 4000) {
  const container =
    document.querySelector(".toast-container") ||
    (function () {
      const c = document.createElement("div");
      c.className = "toast-container position-fixed top-0 end-0 p-3";
      c.style.zIndex = "1300";
      document.body.appendChild(c);
      return c;
    })();
  const t = document.createElement("div");
  t.className = "toast " + cls + " border-0";
  t.innerHTML =
    '<div class="d-flex"><div class="toast-body">' +
    msg +
    '</div><button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div>';
  container.appendChild(t);
  new bootstrap.Toast(t, { delay }).show();
}

// Active Tasks live behavior
(function () {
  const grid = document.getElementById("activeGrid");
  if (!grid) return;

  const sticky = new Set(); // keep cards with finished time

  const parseDT = (s) => {
    if (!s) return null;
    const d = new Date(s);
    return isNaN(d.getTime()) ? null : d;
  };
  const fmt = (sec) => {
    sec = Math.max(0, Math.floor(sec));
    const h = String(Math.floor(sec / 3600)).padStart(2, "0");
    const m = String(Math.floor((sec % 3600) / 60)).padStart(2, "0");
    const s = String(sec % 60).padStart(2, "0");
    return `${h}:${m}:${s}`;
  };

  function updateCard(card) {
    const status = card.dataset.status;
    const exec = parseInt(card.dataset.exec || "0", 10);
    const mainStart = parseDT(card.dataset.mainStart);
    const baseElapsed = parseInt(card.dataset.mainElapsed || "0", 10);
    const pomoUntil = parseDT(card.dataset.pomoUntil);
    const pomoMin = parseInt(card.dataset.pomoMin || "0", 10);
    const breakMin = parseInt(card.dataset.breakMin || "0", 10);
    const scheduledStart = parseDT(card.dataset.scheduledStart);
    const now = new Date();

    // Elapsed calc (supports continuation after extension because baseElapsed retained)
    let elapsed = baseElapsed;
    if (status === "IN_PROGRESS" && mainStart) {
      elapsed += (Date.now() - mainStart.getTime()) / 1000;
    }
    const progress = card.querySelector('[data-role="main-progress"]');
    if (progress && exec > 0) {
      const pct = Math.min(100, (elapsed / (exec * 60)) * 100);
      progress.style.width = pct.toFixed(1) + "%";
    }
    const mainTimer = card.querySelector('[data-role="main-timer"]');
    if (mainTimer) mainTimer.textContent = fmt(elapsed);

    // Pomodoro vs Break display
    const pomoRow = card.querySelector('[data-role="pomo-row"]');
    const breakRow = card.querySelector('[data-role="break-row"]');
    const pomoTimer = card.querySelector('[data-role="pomo-timer"]');
    const breakTimer = card.querySelector('[data-role="break-timer"]');

    // Compute a local fallback for until if API didn't provide it
    let until = pomoUntil;
    if (status === "IN_PROGRESS") {
      if (!until && mainStart && pomoMin > 0) {
        until = new Date(mainStart.getTime() + pomoMin * 60000);
        card.dataset.pomoUntil = until.toISOString();
      }
    } else if (status === "IN_PAUSE") {
      if (!until && breakMin > 0) {
        until = new Date(Date.now() + breakMin * 60000);
        card.dataset.pomoUntil = until.toISOString();
      }
    }

    if (status === "IN_PAUSE") {
      // Show break timer
      if (pomoRow) pomoRow.style.display = "none";
      if (breakRow) breakRow.style.display = "";
      if (breakTimer)
        breakTimer.textContent = until
          ? fmt((until.getTime() - Date.now()) / 1000)
          : "--:--";
    } else {
      // Show pomodoro timer
      if (pomoRow) pomoRow.style.display = "";
      if (breakRow) breakRow.style.display = "none";
      if (pomoTimer)
        pomoTimer.textContent = until
          ? fmt((until.getTime() - Date.now()) / 1000)
          : "--:--";
    }

    // Determine state flags
    const isOverdue = status === "OVERDUE";
    const isPending = status === "PENDING"; // Novo status PENDING
    const pendingStart =
      status === "IN_PROGRESS" &&
      !mainStart &&
      scheduledStart &&
      scheduledStart < now;
    const scheduledPending =
      status === "IN_PROGRESS" && !mainStart && !scheduledStart;
    const timeFinished =
      status === "IN_PROGRESS" && exec > 0 && elapsed >= exec * 60;
    const isPaused = status === "IN_PAUSE";

    // Reset visual indicators
    card.classList.remove(
      "pending-start",
      "time-finished",
      "in-pause",
      "overdue-state",
      "pending-state"
    );
    const title = card.querySelector(".active-title-text");
    if (title)
      title.querySelectorAll(".status-indicator").forEach((i) => i.remove());
    card
      .querySelectorAll(
        ".badge-status.pending-flag, .badge-status.overdue-flag, .badge-status.pending-state-flag"
      )
      .forEach((b) => b.remove());

    // Icon + background selection (priority order)
    if (isOverdue) {
      // OVERDUE state: red background, clock icon, "Pendente finalização" badge
      card.classList.add("overdue-state");
      sticky.add(card.dataset.taskId);
      if (title) {
        const i = document.createElement("i");
        i.className = "bi bi-alarm status-indicator clock";
        i.title = "Prazo vencido - pendente finalização";
        title.insertBefore(i, title.firstChild);
      }
      // Badge flag
      const header = card.querySelector(".active-card-header");
      if (header && !header.querySelector(".overdue-flag")) {
        const flag = document.createElement("span");
        flag.className = "badge-status overdue-flag";
        flag.textContent = "PENDENTE FINALIZAÇÃO";
        header.appendChild(flag);
      }
    } else if (isPending) {
      // PENDING state: blue background, clock icon, "Aguardando ação" badge
      card.classList.add("pending-state");
      sticky.add(card.dataset.taskId);
      if (title) {
        const i = document.createElement("i");
        i.className = "bi bi-clock-history status-indicator pending";
        i.title = "Tempo finalizado - aguardando ação";
        title.insertBefore(i, title.firstChild);
      }
      // Badge flag
      const header = card.querySelector(".active-card-header");
      if (header && !header.querySelector(".pending-state-flag")) {
        const flag = document.createElement("span");
        flag.className = "badge-status pending-state-flag";
        flag.textContent = "AGUARDANDO AÇÃO";
        flag.style.background = "#dbeafe";
        flag.style.color = "#1e40af";
        header.appendChild(flag);
      }
    } else if (isPaused) {
      card.classList.add("in-pause");
      if (title) {
        const i = document.createElement("i");
        i.className = "bi bi-pause-circle status-indicator pause";
        i.title = "Em pausa";
        title.insertBefore(i, title.firstChild);
      }
    } else if (timeFinished) {
      card.classList.add("time-finished");
      sticky.add(card.dataset.taskId);
      if (title) {
        const i = document.createElement("i");
        i.className = "bi bi-person-circle status-indicator user";
        i.title = "Tempo concluído - aguarde ação";
        title.insertBefore(i, title.firstChild);
      }
    } else if (status === "IN_PROGRESS" && mainStart) {
      // Running
      if (title) {
        const i = document.createElement("i");
        i.className = "bi bi-hourglass-split status-indicator timer";
        i.title = "Contando tempo";
        title.insertBefore(i, title.firstChild);
      }
    } else if (pendingStart || scheduledPending) {
      card.classList.add("pending-start");
      if (title) {
        const i = document.createElement("i");
        i.className =
          "bi bi-exclamation-triangle-fill status-indicator warning";
        i.title = "Atrasado - não iniciado";
        title.insertBefore(i, title.firstChild);
      }
      // Badge flag
      const header = card.querySelector(".active-card-header");
      if (header && !header.querySelector(".pending-flag")) {
        const flag = document.createElement("span");
        flag.className = "badge-status pending-flag";
        flag.textContent = "PENDENTE INICIAR";
        header.appendChild(flag);
      }
    }

    // Button logic
    const taskId = card.dataset.taskId;
    card.querySelectorAll("[data-action]").forEach((btn) => {
      const action = btn.dataset.action;
      let disabled = false;

      if (isOverdue) {
        // OVERDUE: enable finish, cancel, extend; disable start and pause
        if (action === "finish" || action === "cancel" || action === "extend") {
          disabled = false;
        } else {
          disabled = true;
        }
      } else if (isPending) {
        // PENDING: enable finish, cancel, extend; disable start and pause
        // Similar a OVERDUE mas com visual azul
        if (action === "finish" || action === "cancel" || action === "extend") {
          disabled = false;
        } else {
          disabled = true;
        }
      } else if (timeFinished) {
        // Only finish & extend enabled
        if (action === "finish" || action === "extend") disabled = false;
        else disabled = true;
      } else if (pendingStart || scheduledPending) {
        // Only start + cancel enabled
        if (action === "start" || action === "cancel") disabled = false;
        else disabled = true;
      } else if (isPaused) {
        // Pause state: allow resume (start), cancel & finish
        if (action === "start" || action === "cancel" || action === "finish")
          disabled = false;
        else disabled = true;
      } else if (status === "IN_PROGRESS") {
        // Running: disable cancel & finish; enable pause
        if (action === "pause") disabled = false;
        else if (action === "cancel" || action === "finish") disabled = true;
        else if (action === "start") disabled = true;
        else disabled = true;
      } else {
        // Fallback
        if (action === "start") disabled = status !== "TODO";
      }

      const wasDisabled = btn.disabled;
      btn.disabled = disabled;

      if (action === "start" && wasDisabled !== disabled) {
        console.log(
          `🔧 Task #${taskId} START button state changed: ${wasDisabled} → ${disabled} (status: ${status}, pendingStart: ${pendingStart}, scheduledPending: ${scheduledPending})`
        );
      }
    });

    // Extend button visibility
    const extendBtn = card.querySelector('[data-action="extend"]');
    if (extendBtn) {
      extendBtn.style.display =
        timeFinished || isOverdue || isPending ? "inline-flex" : "none";
    }
  }

  function esc(s) {
    return String(s ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  }

  // cardTemplate removed as we use server-side fragments
  // function cardTemplate(t) { ... }

  function wireActions(scope) {
    scope.querySelectorAll(".active-card [data-action]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.dataset.id;
        const action = btn.dataset.action;
        console.log(`🔘 Button clicked - Action: ${action}, Task ID: ${id}`);

        if (action === "extend") {
          openExtendModal(id);
          return;
        }

        let status;
        if (action === "start") status = "IN_PROGRESS";
        else if (action === "pause") status = "IN_PAUSE";
        else if (action === "cancel") status = "CANCELLED";
        else if (action === "finish") status = "DONE";
        else {
          console.warn("Unknown action:", action);
          return;
        }

        console.log(
          `📤 Sending request: PATCH /api/tasks/${id}/status?status=${status}`
        );
        btn.disabled = true;
        try {
          const response = await fetch(
            `/api/tasks/${id}/status?status=${status}`,
            { method: "PATCH" }
          );
          console.log(
            `📥 Response status: ${response.status} ${response.statusText}`
          );

          if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            const errorMsg =
              errorData.message || "Erro ao atualizar status da tarefa";
            console.error("❌ Error response:", errorData);
            showToast("text-bg-warning", "⚠️ " + errorMsg);
            if (action === "start" && errorMsg.includes("Tempo de Execução")) {
              setTimeout(() => (window.location.href = `/tasks/${id}`), 1500);
            }
            btn.disabled = false;
            return;
          }

          const result = await response.json();
          console.log("✅ Task updated successfully:", result);

          if (status === "DONE" || status === "CANCELLED") sticky.delete(id);
          showToast(
            "text-bg-success",
            action === "start" ? "✅ Tarefa iniciada!" : "✅ Status atualizado!"
          );
          await refreshDashboard();
        } catch (e) {
          console.error("💥 Exception during request:", e);
          showToast("text-bg-danger", "❌ Erro: " + e.message);
        } finally {
          btn.disabled = false;
        }
      });
    });
  }

  // Replaced getActive with server-side fragment fetching
  // async function getActive() { ... }

  async function autoStartTasks(tasks) {
    const now = new Date();
    for (const task of tasks) {
      // Auto-start if: TODO, scheduledStartAt passed, has execution and pomodoro config, not already started
      if (
        task.status === "TODO" &&
        task.scheduledStartAt &&
        new Date(task.scheduledStartAt) <= now &&
        task.executionTimeMinutes &&
        task.pomodoroMinutes &&
        !task.mainStartedAt
      ) {
        try {
          console.log(`Auto-starting task #${task.id}: ${task.title}`);
          await fetch(`/api/tasks/${task.id}/status?status=IN_PROGRESS`, {
            method: "PATCH",
          });
        } catch (e) {
          console.error(`Failed to auto-start task #${task.id}:`, e);
        }
      }
    }
  }

  async function refreshDashboard() {
    console.log("[Dashboard] 🔄 Executando refresh completo do dashboard...");
    try {
      // 1. Refresh Active Tasks
      try {
        const responseActive = await fetch("/dashboard/active-tasks");
        if (responseActive.ok) {
          const html = await responseActive.text();
          const container = document.getElementById("active-tasks-container");
          if (container) {
            container.innerHTML = html;
            const newGrid = document.getElementById("activeGrid");
            if (newGrid) {
              wireActions(newGrid);
              newGrid.querySelectorAll(".active-card").forEach(updateCard);
            }
          }
        }
      } catch (e) {
        console.error("[Dashboard] ❌ Erro ao atualizar active tasks:", e);
      }

      // 2. Refresh Scheduled Tasks
      try {
        const responseScheduled = await fetch("/dashboard/scheduled-tasks");
        if (responseScheduled.ok) {
          const html = await responseScheduled.text();
          const container = document.getElementById(
            "scheduled-tasks-container"
          );
          if (container) {
            container.innerHTML = html;
          }
        }
      } catch (e) {
        console.error("[Dashboard] ❌ Erro ao atualizar scheduled tasks:", e);
      }

      // 3. Refresh Overdue Tasks
      try {
        const responseOverdue = await fetch("/dashboard/overdue-tasks");
        if (responseOverdue.ok) {
          const html = await responseOverdue.text();
          const tbody = document.getElementById("overdueTableBody");
          if (tbody) {
            tbody.outerHTML = html;
          }
        }
      } catch (e) {
        console.error("[Dashboard] ❌ Erro ao atualizar overdue tasks:", e);
      }

      // 4. Refresh Due Today Tasks
      try {
        const responseDue = await fetch("/dashboard/due-today-tasks");
        if (responseDue.ok) {
          const html = await responseDue.text();
          const tbody = document.getElementById("dueTodayTableBody");
          if (tbody) {
            tbody.outerHTML = html;
          }
        }
      } catch (e) {
        console.error("[Dashboard] ❌ Erro ao atualizar due today tasks:", e);
      }

      console.log("[Dashboard] ✅ Refresh completo finalizado");
    } catch (e) {
      console.error("[Dashboard] ❌ Erro geral no refresh:", e);
    }
  }

    setInterval(() => {
        const currentGrid = document.getElementById('activeGrid');
        if (currentGrid) {
            currentGrid.querySelectorAll('.active-card').forEach(updateCard);
        }
    }, 1000);

    // Polling a cada 5s como fallback (apenas se estiver no dashboard)
    if (document.getElementById('active-tasks-container')) {
        setInterval(refreshDashboard, 5000);
        setTimeout(refreshDashboard, 500);
    }

    // Expor a função de refresh para outros scripts (ex: notifications.js, websocket-client.js)
    if (typeof window !== 'undefined') {
        window.refreshDashboard = refreshDashboard;

        // Mantendo compatibilidade com chamadas antigas, se houver
        window.refreshActiveTasks = refreshDashboard;
        window.refreshScheduledTasks = refreshDashboard;
        window.refreshOverdueTasks = refreshDashboard;
        window.refreshDueTodayTasks = refreshDashboard;
        window.refreshAllTables = refreshDashboard;
    }
})();

// Task Modal
async function openTaskModal(taskId) {
  try {
    if (!taskId) return;
    const modalEl = document.getElementById("taskViewModal");
    if (!modalEl) {
      console.error("#taskViewModal não encontrado");
      return;
    }
    const modal =
      window.bootstrap && bootstrap.Modal ? new bootstrap.Modal(modalEl) : null;
    modal?.show();
    const loading = modalEl.querySelector(".task-detail-loading");
    const content = modalEl.querySelector(".task-detail-content");
    if (loading) loading.style.display = "flex";
    if (content) content.style.display = "none";
    modalEl.querySelectorAll(".value").forEach((v) => (v.textContent = "—"));
    const title = document.getElementById("modalTaskTitle");
    if (title) title.textContent = "Carregando...";

    const resp = await fetch(`/api/tasks/${taskId}`, {
      headers: { Accept: "application/json" },
    });
    if (!resp.ok) throw new Error("Falha ao carregar tarefa");
    const t = await resp.json();

    if (title) title.textContent = t.title || `Tarefa #${taskId}`;
    const statusBadge = document.getElementById("modalTaskStatus");
    if (statusBadge) {
      statusBadge.textContent = t.status;
      statusBadge.className = "badge-status " + String(t.status).toLowerCase();
    }
    const priorityBadge = document.getElementById("modalTaskPriority");
    if (priorityBadge) {
      priorityBadge.textContent = t.priority;
      priorityBadge.className =
        "badge-priority " + String(t.priority).toLowerCase();
    }

    const set = (id, val) => {
      const el = document.getElementById(id);
      if (el) el.textContent = val ?? "—";
    };
    set("modalTaskId", `#${t.id}`);
    set("modalTaskStatusText", t.status);
    set("modalTaskPriorityText", t.priority);
    set("modalTaskCategory", t.categoryName);
    set("modalTaskAssigned", t.assignedUserName);
    set("modalTaskDueDate", formatDateTime(t.dueDate));
    set("modalTaskScheduled", formatDateTime(t.scheduledStartAt));
    set("modalTaskCreated", formatDateTime(t.createdAt));
    set("modalTaskUpdated", formatDateTime(t.updatedAt));
    set("modalTaskExecTime", t.executionTimeMinutes);
    set("modalTaskPomodoro", t.pomodoroMinutes);
    set("modalTaskBreak", t.pomodoroBreakMinutes);
    set("modalTaskMainStarted", formatDateTime(t.mainStartedAt));
    set("modalTaskMainElapsed", t.mainElapsedSeconds);
    set("modalTaskPomoUntil", formatDateTime(t.pomodoroUntil));

    const editBtn = document.getElementById("modalTaskEditBtn");
    if (editBtn) {
      editBtn.href = `/tasks/${t.id}`;
      editBtn.onclick = (e) => {
        e.preventDefault();
        window.location.href = editBtn.href;
      };
    }

    if (loading) loading.style.display = "none";
    if (content) content.style.display = "block";
  } catch (e) {
    console.error("Erro ao abrir modal da tarefa:", e);
    const modalEl = document.getElementById("taskViewModal");
    if (!modalEl) return;
    const loading = modalEl.querySelector(".task-detail-loading");
    const content = modalEl.querySelector(".task-detail-content");
    if (content) content.style.display = "none";
    if (loading) {
      loading.style.display = "flex";
      loading.innerHTML = `<div class="alert alert-danger w-100 d-flex flex-column gap-2">\n  <div><i class='bi bi-exclamation-triangle'></i> Erro ao carregar tarefa.</div>\n  <button type='button' class='btn btn-sm btn-primary align-self-start' onclick='openTaskModal(${JSON.stringify(
        taskId
      )})'>Tentar novamente</button>\n</div>`;
    }
  }
}

if (typeof window !== "undefined") window.openTaskModal = openTaskModal;

function formatDateTime(dt) {
  if (!dt) return "—";
  try {
    const d = new Date(dt);
    if (isNaN(d.getTime())) return "—";
    return d.toLocaleString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return "—";
  }
}

// Extend Task Modal
async function openExtendModal(taskId) {
  const modalEl = document.getElementById("extendTaskModal");
  if (!modalEl) {
    console.error("#extendTaskModal not found");
    return;
  }

  const modal =
    window.bootstrap && bootstrap.Modal ? new bootstrap.Modal(modalEl) : null;
  if (!modal) {
    console.error("Bootstrap Modal not available");
    return;
  }

  // Load task data
  try {
    const resp = await fetch(`/api/tasks/${taskId}`, {
      headers: { Accept: "application/json" },
    });
    if (!resp.ok) throw new Error("Failed to load task");
    const task = await resp.json();

    // Populate modal
    document.getElementById("extendTaskId").value = taskId;
    document.getElementById("extendTaskTitle").textContent =
      task.title || `Task #${taskId}`;
    document.getElementById("extendExtraTime").value = "";
    document.getElementById("extendJustification").value = "";

    // Set current dates as defaults
    const now = new Date();
    const formatForInput = (date) => {
      if (!date) return "";
      const d = new Date(date);
      return d.toISOString().slice(0, 16);
    };

    document.getElementById("extendScheduledStart").value = formatForInput(
      task.scheduledStartAt || now
    );
    document.getElementById("extendDueDate").value = formatForInput(
      task.dueDate || now
    );

    modal.show();
  } catch (e) {
    console.error("Error opening extend modal:", e);
    alert("Erro ao carregar dados da tarefa");
  }
}

async function submitExtendTask() {
  const taskId = document.getElementById("extendTaskId").value;
  const extraTime = document.getElementById("extendExtraTime").value;
  const justification = document.getElementById("extendJustification").value;
  const scheduledStart = document.getElementById("extendScheduledStart").value;
  const dueDate = document.getElementById("extendDueDate").value;

  if (!extraTime || extraTime <= 0) {
    alert("Por favor, informe o tempo extra");
    return;
  }

  const submitBtn = document.getElementById("extendSubmitBtn");
  if (submitBtn) submitBtn.disabled = true;

  try {
    const payload = {
      extraTimeMinutes: parseInt(extraTime),
      justification: justification || null,
      scheduledStartAt: scheduledStart
        ? new Date(scheduledStart).toISOString()
        : null,
      dueDate: dueDate ? new Date(dueDate).toISOString() : null,
    };

    const resp = await fetch(`/api/tasks/${taskId}/extend`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (!resp.ok) {
      const error = await resp.text();
      throw new Error(error || "Failed to extend task");
    }

    // Close modal
    const modalEl = document.getElementById("extendTaskModal");
    const modal = bootstrap.Modal.getInstance(modalEl);
    if (modal) modal.hide();

    // Refresh tasks
    const grid = document.getElementById("activeGrid");
    if (grid) {
      // Force refresh
      setTimeout(() => window.location.reload(), 500);
    }

    alert("Tarefa estendida com sucesso!");
  } catch (e) {
    console.error("Error extending task:", e);
    alert("Erro ao estender tarefa: " + e.message);
  } finally {
    if (submitBtn) submitBtn.disabled = false;
  }
}

if (typeof window !== "undefined") {
  window.openExtendModal = openExtendModal;
  window.submitExtendTask = submitExtendTask;

  window.startScheduledTask = async function(btn) {
      const taskId = btn.getAttribute('data-task-id');
      if (!taskId) return;

      // Disable button to prevent double clicks
      btn.disabled = true;
      const originalHtml = btn.innerHTML;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>';

      try {
          const response = await fetch(`/api/tasks/${taskId}/status?status=IN_PROGRESS`, {
              method: 'PATCH'
          });

          if (response.ok) {
              showToast("text-bg-success", "✅ Tarefa agendada iniciada!");
              if (typeof window.refreshDashboard === 'function') {
                  await window.refreshDashboard();
              } else {
                  window.location.reload();
              }
          } else {
              const errorData = await response.json().catch(() => ({}));
              const msg = errorData.message || "Erro ao iniciar tarefa";
              showToast("text-bg-warning", "⚠️ " + msg);
              btn.disabled = false;
              btn.innerHTML = originalHtml;
          }
      } catch (e) {
          console.error("Error starting scheduled task:", e);
          showToast("text-bg-danger", "❌ Erro de conexão");
          btn.disabled = false;
          btn.innerHTML = originalHtml;
      }
  };

  window.cancelScheduledTask = async function(btn) {
      const taskId = btn.getAttribute('data-task-id');
      if (!taskId) return;

      if (!confirm('Tem certeza que deseja cancelar esta tarefa agendada?')) {
          return;
      }

      // Disable button
      btn.disabled = true;
      const originalHtml = btn.innerHTML;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>';

      try {
          const response = await fetch(`/api/tasks/${taskId}/status?status=CANCELLED`, {
              method: 'PATCH'
          });

          if (response.ok) {
              showToast("text-bg-success", "✅ Tarefa agendada cancelada!");
              if (typeof window.refreshDashboard === 'function') {
                  await window.refreshDashboard();
              } else {
                  window.location.reload();
              }
          } else {
              const errorData = await response.json().catch(() => ({}));
              const msg = errorData.message || "Erro ao cancelar tarefa";
              showToast("text-bg-warning", "⚠️ " + msg);
              btn.disabled = false;
              btn.innerHTML = originalHtml;
          }
      } catch (e) {
          console.error("Error cancelling scheduled task:", e);
          showToast("text-bg-danger", "❌ Erro de conexão");
          btn.disabled = false;
          btn.innerHTML = originalHtml;
      }
  };
}
