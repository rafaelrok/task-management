// Task List Page JS
(function () {
    'use strict';
    
    const THEME_KEY = 'theme';

    function applyTheme() {
        const theme = localStorage.getItem(THEME_KEY) || 'light';
        document.documentElement.setAttribute('data-theme', theme);
    }

    // --- Toast Helper ---
    function showToast(cls, msg, delay = 4000) {
        const container = document.querySelector('.toast-container') || (function () {
            const c = document.createElement('div');
            c.className = 'toast-container position-fixed top-0 end-0 p-3';
            c.style.zIndex = '1300';
            document.body.appendChild(c);
            return c;
        })();
        const t = document.createElement('div');
        t.className = 'toast ' + cls + ' border-0';
        t.innerHTML = '<div class="d-flex"><div class="toast-body">' + msg + '</div><button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div>';
        container.appendChild(t);
        new bootstrap.Toast(t, {delay}).show();
    }

    // --- Format Helper ---
    function formatHMS(totalSec) {
        totalSec = Math.max(0, Math.floor(totalSec));
        const h = String(Math.floor(totalSec / 3600)).padStart(2, '0');
        const m = String(Math.floor((totalSec % 3600) / 60)).padStart(2, '0');
        const s = String(totalSec % 60).padStart(2, '0');
        return `${h}:${m}:${s}`;
    }

    function formatMS(totalSec) {
        totalSec = Math.max(0, Math.floor(totalSec));
        const m = String(Math.floor(totalSec / 60)).padStart(2, '0');
        const s = String(totalSec % 60).padStart(2, '0');
        return `${m}:${s}`;
    }

    // --- MAIN TIMER UPDATE FUNCTION (called every second) ---
    function updateAllTimers() {
        const rows = document.querySelectorAll('#taskTableInner .task-list-row');
        
        rows.forEach(row => {
            const status = row.dataset.status;
            const id = row.dataset.taskId;
            if (!id) return;

            // Skip finished tasks - just show static values
            if (status === 'DONE' || status === 'CANCELLED') {
                const mainTimerEl = row.querySelector('[data-role="main-timer"]');
                const mainElapsed = parseInt(row.dataset.mainElapsed) || 0;
                if (mainTimerEl) {
                    mainTimerEl.textContent = formatHMS(mainElapsed);
                }
                return;
            }

            // === MAIN TIMER (for IN_PROGRESS) ===
            const mainTimerEl = row.querySelector('[data-role="main-timer"]');
            const mainProgressEl = row.querySelector('[data-role="main-progress"]');
            
            if (status === 'IN_PROGRESS') {
                const mainStart = row.dataset.mainStart;
                const mainElapsed = parseInt(row.dataset.mainElapsed) || 0;
                const execMin = parseInt(row.dataset.exec) || 0;
                const extraMin = parseInt(row.dataset.extraMin) || 0;
                
                let elapsed = mainElapsed;
                if (mainStart) {
                    const now = new Date();
                    const start = new Date(mainStart);
                    elapsed = Math.floor((now - start) / 1000) + mainElapsed;
                }
                
                if (mainTimerEl) {
                    mainTimerEl.textContent = formatHMS(elapsed);
                }
                
                if (mainProgressEl && execMin > 0) {
                    const targetSec = (execMin + extraMin) * 60;
                    const pct = Math.min(100, (elapsed / targetSec) * 100);
                    mainProgressEl.style.width = pct.toFixed(1) + '%';
                }
            } else if (status === 'IN_PAUSE') {
                // For paused tasks, show the frozen elapsed time
                const mainElapsed = parseInt(row.dataset.mainElapsed) || 0;
                if (mainTimerEl) {
                    mainTimerEl.textContent = formatHMS(mainElapsed);
                }
            }

            // === POMODORO TIMER (for IN_PROGRESS and IN_PAUSE) ===
            const pomoTimerEl = row.querySelector('[data-role="pomo-timer"]');
            if (pomoTimerEl) {
                const pomoUntil = row.dataset.pomoUntil;
                
                if (pomoUntil && (status === 'IN_PROGRESS' || status === 'IN_PAUSE')) {
                    const until = new Date(pomoUntil);
                    const now = new Date();
                    const remaining = Math.floor((until - now) / 1000);
                    
                    if (remaining > 0) {
                        pomoTimerEl.textContent = formatMS(remaining);
                    } else {
                        // Timer expired
                        if (status === 'IN_PROGRESS') {
                            pomoTimerEl.textContent = 'Pausa!';
                        } else {
                            pomoTimerEl.textContent = 'Retomar!';
                        }
                    }
                } else {
                    pomoTimerEl.textContent = '—';
                }
            }

            // === UPDATE STATUS ICON ===
            updateStatusIcon(row);
        });
    }

    function updateStatusIcon(row) {
        const status = row.dataset.status;
        const overdue = row.dataset.overdue === 'true';
        const execMin = parseInt(row.dataset.exec) || 0;
        const mainStart = row.dataset.mainStart;
        const mainElapsed = parseInt(row.dataset.mainElapsed) || 0;
        const extraMin = parseInt(row.dataset.extraMin) || 0;
        
        const icon = row.querySelector('[data-role="status-icon"]');
        if (!icon) return;
        
        icon.className = 'task-row-icon bi';
        
        if (status === 'IN_PROGRESS' && mainStart && !overdue) {
            const targetSec = (execMin + extraMin) * 60;
            const now = new Date();
            const start = new Date(mainStart);
            const elapsed = Math.floor((now - start) / 1000) + mainElapsed;
            if (elapsed >= targetSec) {
                icon.classList.add('bi-person-fill');
                icon.title = 'Tempo concluído';
            } else {
                icon.classList.add('bi-hourglass-split');
                icon.title = 'Em progresso';
            }
        } else if (status === 'IN_PROGRESS' && (!mainStart || overdue)) {
            icon.classList.add('bi-exclamation-triangle-fill');
            icon.title = 'Atrasado - não iniciado';
        } else if (status === 'IN_PAUSE') {
            icon.classList.add('bi-pause-circle-fill');
            icon.title = 'Em pausa';
        } else if (status === 'PENDING') {
            icon.classList.add('bi-clock-history');
            icon.title = 'Aguardando Finalizar';
        } else if (status === 'OVERDUE' || overdue) {
            icon.classList.add('bi-clock-fill');
            icon.title = 'Atrasado';
        } else if (status === 'DONE') {
            icon.classList.add('bi-check-circle-fill');
            icon.title = 'Concluído';
        } else if (status === 'CANCELLED') {
            icon.classList.add('bi-x-circle-fill');
            icon.title = 'Cancelado';
        } else {
            icon.classList.add('bi-circle');
            icon.title = 'Pendente';
        }
    }

    // --- Timer Control ---
    let mainTimerInterval = null;

    function startTimerInterval() {
        // Clear any existing interval
        if (mainTimerInterval) {
            clearInterval(mainTimerInterval);
        }
        
        // Run immediately once
        updateAllTimers();
        
        // Then run every second
        mainTimerInterval = setInterval(updateAllTimers, 1000);
        
        console.log('✅ Timer interval started');
    }

    function stopTimerInterval() {
        if (mainTimerInterval) {
            clearInterval(mainTimerInterval);
            mainTimerInterval = null;
        }
    }

    // Initialize task list events
    function initTaskList() {
        bindStatusForms();
        bindAjaxPagination();
        syncActionButtons();
    }

    // Reload the task table fragment
    function reloadTasksFragment() {
        const url = window.location.href;
        loadFragment(url);
    }

    // Sync buttons
    function syncActionButtons() {
        document.querySelectorAll('.task-list-row').forEach(row => {
            const status = row.dataset.status;
            if (!status) return;
            
            const editBtn = row.querySelector('.btn-edit');
            if (editBtn) editBtn.disabled = status === 'DONE' || status === 'CANCELLED';
            
            const startBtn = row.querySelector('.btn-start');
            // PENDING e OVERDUE não podem iniciar
            if (startBtn) startBtn.disabled = ['IN_PROGRESS', 'DONE', 'CANCELLED', 'OVERDUE', 'PENDING'].includes(status) || row.dataset.overdue === 'true';
            
            const pauseBtn = row.querySelector('.btn-pause');
            if (pauseBtn) pauseBtn.disabled = status !== 'IN_PROGRESS';
            
            const deleteBtn = row.querySelector('.btn-delete');
            if (deleteBtn) deleteBtn.disabled = status !== 'TODO';
            
            // Botão de estender - visível para PENDING e OVERDUE
            const extendBtn = row.querySelector('.btn-extend');
            if (extendBtn) {
                extendBtn.style.display = (status === 'PENDING' || status === 'OVERDUE') ? 'inline-flex' : 'none';
            }
        });
        
        document.querySelectorAll('.btn-edit').forEach(btn => {
            btn.addEventListener('click', function () {
                if (this.disabled) return;
                const url = this.dataset.editUrl;
                if (url) window.location.href = url;
            });
        });
    }

    // Extend modal open
    async function openExtendModalFromList(taskId) {
        try {
            const response = await fetch(`/api/tasks/${taskId}`);
            if (!response.ok) throw new Error('Failed to fetch task details');
            const task = await response.json();
            document.getElementById('extendTaskId').value = task.id;
            document.getElementById('extendTaskTitle').textContent = task.title;
            document.getElementById('extendExtraTime').value = '';
            document.getElementById('extendJustification').value = '';
            if (task.scheduledStartAt) {
                const d = new Date(task.scheduledStartAt);
                document.getElementById('extendScheduledStart').value = d.toISOString().slice(0, 16);
            }
            if (task.dueDate) {
                const dd = new Date(task.dueDate);
                document.getElementById('extendDueDate').value = dd.toISOString().slice(0, 16);
            }
            const modalElement = document.getElementById('extendTaskModal');
            const modal = new bootstrap.Modal(modalElement);
            modal.show();
        } catch (e) {
            console.error(e);
            showToast('text-bg-danger', '❌ Erro ao carregar dados da tarefa');
        }
    }

    // Submit extend
    async function submitExtendTask() {
        const taskId = document.getElementById('extendTaskId').value;
        const extraTime = document.getElementById('extendExtraTime').value;
        const scheduledStart = document.getElementById('extendScheduledStart').value;
        const dueDate = document.getElementById('extendDueDate').value;
        const justification = document.getElementById('extendJustification').value;
        if (!extraTime || parseInt(extraTime) < 1) {
            showToast('text-bg-warning', '⚠️ Informe o tempo extra (mínimo 1 minuto)');
            return;
        }
        const submitBtn = document.getElementById('extendSubmitBtn');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Processando...';
        try {
            const payload = {extraTimeMinutes: parseInt(extraTime), extensionJustification: justification || null};
            if (scheduledStart) payload.scheduledStartAt = new Date(scheduledStart).toISOString();
            if (dueDate) payload.dueDate = new Date(dueDate).toISOString();
            const response = await fetch(`/api/tasks/${taskId}/extend`, {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                const errData = await response.json().catch(() => ({}));
                throw new Error(errData.message || 'Erro ao estender tarefa');
            }
            await response.json();
            const modalElement = document.getElementById('extendTaskModal');
            bootstrap.Modal.getInstance(modalElement).hide();
            showToast('text-bg-success', '✅ Tarefa estendida com sucesso!');
            setTimeout(() => window.location.reload(), 1500);
        } catch (e) {
            showToast('text-bg-danger', '❌ ' + e.message);
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="bi bi-clock-history"></i> Estender Tarefa';
        }
    }

    // Status form binding
    function bindStatusForms() {
        document.querySelectorAll('#taskTableInner form[action*="/status"]').forEach(f => {
            f.addEventListener('submit', e => {
                const input = f.querySelector('input[name="status"]');
                if (!input || input.value !== 'IN_PROGRESS') return;
                e.preventDefault();
                const url = f.getAttribute('action');
                const formData = new FormData(f);
                fetch(url, {method: 'POST', body: formData, redirect: 'follow'})
                    .then(r => {
                        return r.text().then(() => handleStatusRedirect(url));
                    })
                    .catch(err => {
                        console.error('Erro ao iniciar tarefa:', err);
                        showToast('text-bg-danger', 'Erro ao iniciar tarefa: ' + err.message);
                    });
            });
        });
    }

    function handleStatusRedirect(url) {
        const idMatch = url.match(/\/tasks\/(\d+)\/status/);
        const taskId = idMatch ? idMatch[1] : null;
        if (!taskId) {
            reloadTasksFragment();
            return;
        }
        fetch('/api/tasks/' + taskId)
            .then(r => r.json())
            .then(task => {
                if (task.status === 'IN_PROGRESS') {
                    showToast('text-bg-success', '✅ Tarefa iniciada! Contador em execução.');
                    setTimeout(() => reloadTasksFragment(), 1100);
                } else if (task.status === 'TODO') {
                    showToast('text-bg-warning', '⚠️ Preencha Tempo de Execução e Duração do Pomodoro antes de iniciar.');
                    setTimeout(() => window.location.href = '/tasks/' + taskId, 1500);
                } else {
                    reloadTasksFragment();
                }
            })
            .catch(() => reloadTasksFragment());
    }

    // Pagination
    function bindAjaxPagination() {
        document.querySelectorAll('#taskTableInner .page-link-fade').forEach(a => {
            a.addEventListener('click', function (e) {
                if (this.classList.contains('disabled')) return;
                e.preventDefault();
                const url = this.getAttribute('href');
                loadFragment(url);
            });
        });
    }

    // Load fragment via AJAX
    function loadFragment(url) {
        const container = document.getElementById('taskTableInner');
        if (container) container.classList.add('loading');
        fetch(url + (url.includes('?') ? '&' : '?') + 'ajax=true', {headers: {'X-Requested-With': 'XMLHttpRequest'}})
            .then(r => r.text())
            .then(html => {
                const c = document.getElementById('taskTableInner');
                if (!c) return window.location.href = url;
                c.innerHTML = html;
                c.classList.remove('loading');
                initTaskList();
                // Restart timer after fragment load
                startTimerInterval();
                history.pushState({}, '', url);
            })
            .catch(() => {
                if (container) container.classList.remove('loading');
                window.location.href = url;
            });
    }

    // --- Initialization ---
    applyTheme();
    
    document.addEventListener('DOMContentLoaded', function () {
        console.log('📋 Task List JS loaded');
        initTaskList();
        startTimerInterval();
    });

    // Expose functions globally for onclick handlers in HTML and WebSocket updates
    window.openExtendModalFromList = openExtendModalFromList;
    window.submitExtendTask = submitExtendTask;
    window.updateAllTimers = updateAllTimers;
})();

