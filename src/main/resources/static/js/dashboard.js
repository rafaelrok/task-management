// Dashboard JS: Theme/Sidebar + Active Tasks + Task Modal
(function () {
    'use strict';
    const root = document.documentElement;
    const sidebar = document.getElementById('sidebar');
    const toggle = document.getElementById('sidebarToggle');
    const themeToggle = document.getElementById('themeToggle');

    if (toggle && sidebar) {
        const collapsed = localStorage.getItem('sidebarCollapsed') === 'true';
        if (collapsed) sidebar.classList.add('collapsed');
        toggle.addEventListener('click', () => {
            sidebar.classList.toggle('collapsed');
            localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed'));
        });
    }

    if (themeToggle) {
        const icon = themeToggle.querySelector('i');
        const label = themeToggle.querySelector('.menu-text');
        const saved = localStorage.getItem('theme') || 'light';
        applyTheme(saved);
        themeToggle.addEventListener('click', (e) => {
            e.preventDefault();
            const cur = root.getAttribute('data-theme') || 'light';
            const next = cur === 'light' ? 'dark' : 'light';
            applyTheme(next);
            localStorage.setItem('theme', next);
        });

        function applyTheme(t) {
            root.setAttribute('data-theme', t);
            if (t === 'dark') {
                icon?.classList.remove('bi-moon-stars');
                icon?.classList.add('bi-sun');
                if (label) label.textContent = 'Tema Claro';
            } else {
                icon?.classList.remove('bi-sun');
                icon?.classList.add('bi-moon-stars');
                if (label) label.textContent = 'Tema Escuro';
            }
        }
    }

    if (window.bootstrap) {
        const triggers = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        triggers.map(el => new bootstrap.Tooltip(el));
    }
})();

// Active Tasks live behavior
(function () {
    const grid = document.getElementById('activeGrid');
    if (!grid) return;

    const sticky = new Set(); // keep cards with finished time

    const parseDT = (s) => {
        if (!s) return null;
        const d = new Date(s);
        return isNaN(d.getTime()) ? null : d;
    };
    const fmt = (sec) => {
        sec = Math.max(0, Math.floor(sec));
        const h = String(Math.floor(sec / 3600)).padStart(2, '0');
        const m = String(Math.floor((sec % 3600) / 60)).padStart(2, '0');
        const s = String(sec % 60).padStart(2, '0');
        return `${h}:${m}:${s}`;
    };

    function updateCard(card) {
        const status = card.dataset.status;
        const exec = parseInt(card.dataset.exec || '0', 10);
        const mainStart = parseDT(card.dataset.mainStart);
        const baseElapsed = parseInt(card.dataset.mainElapsed || '0', 10);
        const pomoUntil = parseDT(card.dataset.pomoUntil);
        const scheduledStart = parseDT(card.dataset.scheduledStart);

        // Main timer
        let elapsed = baseElapsed;
        if (status === 'IN_PROGRESS' && mainStart) elapsed += (Date.now() - mainStart.getTime()) / 1000;
        const mainTimer = card.querySelector('[data-role="main-timer"]');
        if (mainTimer) mainTimer.textContent = fmt(elapsed);
        const progress = card.querySelector('[data-role="main-progress"]');
        if (progress && exec > 0) {
            const pct = Math.min(100, (elapsed / (exec * 60)) * 100);
            progress.style.width = pct.toFixed(1) + '%';
        }

        // Pomo vs Break
        const pomoRow = card.querySelector('[data-role="pomo-row"]');
        const breakRow = card.querySelector('[data-role="break-row"]');
        const pomoTimer = card.querySelector('[data-role="pomo-timer"]');
        const breakTimer = card.querySelector('[data-role="break-timer"]');
        if (status === 'IN_PAUSE') {
            if (pomoRow) pomoRow.style.display = 'none';
            if (breakRow) breakRow.style.display = '';
            if (breakTimer) breakTimer.textContent = pomoUntil ? fmt((pomoUntil.getTime() - Date.now()) / 1000) : '--:--';
            card.classList.add('on-break');
            const title = card.querySelector('.active-title-text');
            if (title && !title.querySelector('.status-indicator.pause')) {
                const i = document.createElement('i');
                i.className = 'bi bi-pause-circle status-indicator pause';
                i.title = 'Em pausa (intervalo)';
                title.insertBefore(i, title.firstChild);
            }
        } else {
            if (pomoRow) pomoRow.style.display = '';
            if (breakRow) breakRow.style.display = 'none';
            if (pomoTimer) pomoTimer.textContent = pomoUntil ? fmt((pomoUntil.getTime() - Date.now()) / 1000) : '--:--';
            card.classList.remove('on-break');
            const pi = card.querySelector('.status-indicator.pause');
            if (pi) pi.remove();
        }

        // Time finished sticky
        const timeDone = status === 'IN_PROGRESS' && exec > 0 && elapsed >= (exec * 60);
        if (timeDone) {
            card.classList.add('time-finished');
            sticky.add(card.dataset.taskId);
            const title = card.querySelector('.active-title-text');
            if (title && !title.querySelector('.status-indicator.info')) {
                const i = document.createElement('i');
                i.className = 'bi bi-info-circle status-indicator info';
                i.title = 'Tempo finalizado - aguardando conclusão';
                title.insertBefore(i, title.firstChild);
            }
            card.querySelectorAll('[data-action]').forEach(btn => {
                if (btn.dataset.action !== 'finish') btn.disabled = true;
            });
        } else {
            card.classList.remove('time-finished');
        }

        // TODO overdue
        const overdue = status === 'TODO' && scheduledStart && scheduledStart < new Date();
        if (overdue) card.classList.add('todo-overdue'); else card.classList.remove('todo-overdue');
    }

    function esc(s) {
        return String(s ?? '').replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll('\'', '&#39;');
    }

    function cardTemplate(t) {
        const data = [
            `data-task-id="${t.id}"`,
            `data-status="${t.status}"`,
            `data-exec="${t.executionTimeMinutes ?? ''}"`,
            `data-main-start="${t.mainStartedAt ?? ''}"`,
            `data-main-elapsed="${t.mainElapsedSeconds ?? 0}"`,
            `data-pomo-until="${t.pomodoroUntil ?? ''}"`,
            `data-pomo-min="${t.pomodoroMinutes ?? ''}"`,
            `data-break-min="${t.pomodoroBreakMinutes ?? ''}"`,
            `data-scheduled-start="${t.scheduledStartAt ?? ''}"`
        ].join(' ');
        const disabledStart = t.status === 'IN_PROGRESS' ? 'disabled' : '';
        const disabledPause = t.status === 'IN_PAUSE' || t.status === 'TODO' ? 'disabled' : '';
        const statusClass = String(t.status).toLowerCase();
        let icon = '';
        if (t.status === 'TODO') icon = '<i class="bi bi-exclamation-triangle status-indicator warning" title="Tarefa em atraso"></i>';
        if (t.status === 'IN_PAUSE') icon = '<i class="bi bi-pause-circle status-indicator pause" title="Em pausa"></i>';
        return `
      <div class="active-card" ${data}>
        <div class="active-card-header">
          <div class="active-title-text">${icon}${esc(t.title)}</div>
          <span class="badge-status ${statusClass}">${t.status}</span>
        </div>
        <div class="active-timers">
          <div class="timer-row"><span class="timer-label">Principal</span><span class="timer-value" data-role="main-timer">00:00:00</span></div>
          <div class="progress progress-compact"><div class="progress-bar" role="progressbar" style="width: 0%" aria-valuemin="0" aria-valuemax="100" data-role="main-progress"></div></div>
          <div class="timer-row" data-role="pomo-row"><span class="timer-label">Pomodoro</span><span class="timer-value" data-role="pomo-timer">--:--</span></div>
          <div class="timer-row" data-role="break-row" style="display:none;"><span class="timer-label">Intervalo</span><span class="timer-value" data-role="break-timer">--:--</span></div>
        </div>
        <div class="active-actions">
          <a href="/tasks/${t.id}" class="btn-icon-sm btn-outline" title="Abrir tarefa"><i class="bi bi-box-arrow-up-right"></i></a>
          <button class="btn-icon-sm btn-outline" data-action="start" data-id="${t.id}" title="Iniciar" ${disabledStart}><i class="bi bi-play-fill"></i></button>
          <button class="btn-icon-sm btn-outline" data-action="pause" data-id="${t.id}" title="Pausar" ${disabledPause}><i class="bi bi-pause-fill"></i></button>
          <button class="btn-icon-sm btn-outline text-danger" data-action="cancel" data-id="${t.id}" title="Cancelar"><i class="bi bi-x-lg"></i></button>
          <button class="btn-icon-sm btn-outline text-success" data-action="finish" data-id="${t.id}" title="Finalizar"><i class="bi bi-check-lg"></i></button>
        </div>
      </div>`;
    }

    function wireActions(scope) {
        scope.querySelectorAll('.active-card [data-action]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                const action = btn.dataset.action;
                let status;
                if (action === 'start') status = 'IN_PROGRESS'; else if (action === 'pause') status = 'IN_PAUSE'; else if (action === 'cancel') status = 'CANCELLED'; else if (action === 'finish') status = 'DONE'; else return;
                btn.disabled = true;
                try {
                    await fetch(`/api/tasks/${id}/status?status=${status}`, {method: 'PATCH'});
                    if (status === 'DONE' || status === 'CANCELLED') sticky.delete(id);
                    await refresh();
                } catch (e) {
                    console.error(e);
                } finally {
                    btn.disabled = false;
                }
            });
        });
    }

    async function getActive() {
        const settled = await Promise.allSettled([
            fetch('/api/tasks/status/IN_PROGRESS').then(r => r.json()),
            fetch('/api/tasks/status/IN_PAUSE').then(r => r.json()),
            fetch('/api/tasks/status/TODO').then(r => r.json())
        ]);
        const [p1, p2, p3] = settled.map(s => s.status === 'fulfilled' ? s.value : []);
        const now = new Date();
        const list = [...p1, ...p2];
        (p3 || []).forEach(t => {
            if (t.scheduledStartAt && new Date(t.scheduledStartAt) <= now && t.executionTimeMinutes && t.pomodoroMinutes) list.push(t);
        });
        // Keep stickies if API stops returning them
        sticky.forEach(id => {
            if (!list.find(x => String(x.id) === String(id))) {
                const dom = grid.querySelector(`.active-card[data-task-id="${id}"]`);
                if (dom) {
                    const t = {
                        id,
                        status: dom.dataset.status || 'IN_PROGRESS',
                        title: dom.querySelector('.active-title-text')?.textContent?.trim() || `#${id}`,
                        executionTimeMinutes: parseInt(dom.dataset.exec || '0', 10) || undefined,
                        mainStartedAt: dom.dataset.mainStart || undefined,
                        mainElapsedSeconds: parseInt(dom.dataset.mainElapsed || '0', 10) || 0,
                        pomodoroUntil: dom.dataset.pomoUntil || undefined,
                        pomodoroMinutes: parseInt(dom.dataset.pomoMin || '0', 10) || undefined,
                        pomodoroBreakMinutes: parseInt(dom.dataset.breakMin || '0', 10) || undefined,
                        scheduledStartAt: dom.dataset.scheduledStart || undefined
                    };
                    list.push(t);
                }
            }
        });
        return list;
    }

    async function refresh() {
        try {
            const tasks = await getActive();
            const section = document.querySelector('.active-section');
            if (!tasks || tasks.length === 0) {
                if (section) section.style.display = 'none';
                grid.innerHTML = '';
                return;
            }
            if (section) section.style.display = '';
            grid.innerHTML = tasks.map(cardTemplate).join('');
            wireActions(grid);
            grid.querySelectorAll('.active-card').forEach(updateCard);
        } catch (e) {
            console.error('Failed to refresh active tasks', e);
        }
    }

    setInterval(() => {
        grid.querySelectorAll('.active-card').forEach(updateCard);
    }, 1000);
    setInterval(refresh, 8000);
    setTimeout(refresh, 500);
})();

// Task Modal
async function openTaskModal(taskId) {
    try {
        if (!taskId) return;
        const modalEl = document.getElementById('taskViewModal');
        if (!modalEl) {
            console.error('#taskViewModal não encontrado');
            return;
        }
        const modal = window.bootstrap && bootstrap.Modal ? new bootstrap.Modal(modalEl) : null;
        modal?.show();
        const loading = modalEl.querySelector('.task-detail-loading');
        const content = modalEl.querySelector('.task-detail-content');
        if (loading) loading.style.display = 'flex';
        if (content) content.style.display = 'none';
        modalEl.querySelectorAll('.value').forEach(v => v.textContent = '—');
        const title = document.getElementById('modalTaskTitle');
        if (title) title.textContent = 'Carregando...';

        const resp = await fetch(`/api/tasks/${taskId}`, {headers: {'Accept': 'application/json'}});
        if (!resp.ok) throw new Error('Falha ao carregar tarefa');
        const t = await resp.json();

        if (title) title.textContent = t.title || `Tarefa #${taskId}`;
        const statusBadge = document.getElementById('modalTaskStatus');
        if (statusBadge) {
            statusBadge.textContent = t.status;
            statusBadge.className = 'badge-status ' + String(t.status).toLowerCase();
        }
        const priorityBadge = document.getElementById('modalTaskPriority');
        if (priorityBadge) {
            priorityBadge.textContent = t.priority;
            priorityBadge.className = 'badge-priority ' + String(t.priority).toLowerCase();
        }

        const set = (id, val) => {
            const el = document.getElementById(id);
            if (el) el.textContent = val ?? '—';
        };
        set('modalTaskId', `#${t.id}`);
        set('modalTaskStatusText', t.status);
        set('modalTaskPriorityText', t.priority);
        set('modalTaskCategory', t.categoryName);
        set('modalTaskAssigned', t.assignedUserName);
        set('modalTaskDueDate', formatDateTime(t.dueDate));
        set('modalTaskScheduled', formatDateTime(t.scheduledStartAt));
        set('modalTaskCreated', formatDateTime(t.createdAt));
        set('modalTaskUpdated', formatDateTime(t.updatedAt));
        set('modalTaskExecTime', t.executionTimeMinutes);
        set('modalTaskPomodoro', t.pomodoroMinutes);
        set('modalTaskBreak', t.pomodoroBreakMinutes);
        set('modalTaskMainStarted', formatDateTime(t.mainStartedAt));
        set('modalTaskMainElapsed', t.mainElapsedSeconds);
        set('modalTaskPomoUntil', formatDateTime(t.pomodoroUntil));

        const editBtn = document.getElementById('modalTaskEditBtn');
        if (editBtn) {
            editBtn.href = `/tasks/${t.id}`;
            editBtn.onclick = (e) => {
                e.preventDefault();
                window.location.href = editBtn.href;
            };
        }

        if (loading) loading.style.display = 'none';
        if (content) content.style.display = 'block';
    } catch (e) {
        console.error('Erro ao abrir modal da tarefa:', e);
        const modalEl = document.getElementById('taskViewModal');
        if (!modalEl) return;
        const loading = modalEl.querySelector('.task-detail-loading');
        const content = modalEl.querySelector('.task-detail-content');
        if (content) content.style.display = 'none';
        if (loading) {
            loading.style.display = 'flex';
            loading.innerHTML = `<div class="alert alert-danger w-100 d-flex flex-column gap-2">\n  <div><i class='bi bi-exclamation-triangle'></i> Erro ao carregar tarefa.</div>\n  <button type='button' class='btn btn-sm btn-primary align-self-start' onclick='openTaskModal(${JSON.stringify(taskId)})'>Tentar novamente</button>\n</div>`;
        }
    }
}

if (typeof window !== 'undefined') window.openTaskModal = openTaskModal;

function formatDateTime(dt) {
    if (!dt) return '—';
    try {
        const d = new Date(dt);
        if (isNaN(d.getTime())) return '—';
        return d.toLocaleString('pt-BR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch {
        return '—';
    }
}
