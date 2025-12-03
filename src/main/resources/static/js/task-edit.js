// Task Edit Page JS - Real-time timer updates and Rich Editor initialization
(function () {
    'use strict';

    // Initialize Rich Editor for edit page
    function initializeRichEditor() {
        console.log('Initializing rich editor for edit page...');
        if (typeof RichEditor === 'function') {
            RichEditor('taskDescEdit', 'description');
        } else {
            console.error('RichEditor function not found!');
        }

        // Sync rich editor content on form submit
        const form = document.querySelector('form');
        if (form) {
            form.addEventListener('submit', function (e) {
                const hiddenField = document.getElementById('taskDescEdit_hidden');
                const editorArea = document.querySelector('#taskDescEdit .editor-area');
                if (hiddenField && editorArea) {
                    hiddenField.value = editorArea.innerHTML;
                    console.log('Synced description on submit:', hiddenField.value.substring(0, 100));
                }
            });
        }
    }

    // Initialize Squad Member Selection
    function initializeSquadMemberSelect() {
        const squadSelect = document.getElementById('squadSelect');
        const assignedUserContainer = document.getElementById('assignedUserContainer');
        const assignedUserSelect = document.getElementById('assignedUserSelect');
        
        if (!squadSelect || !assignedUserSelect) {
            return;
        }

        // Get the current assigned user ID (if any)
        const currentAssignedUserId = assignedUserSelect.value;

        // Function to load squad members
        async function loadSquadMembers(squadId) {
            if (!squadId) {
                assignedUserContainer.style.display = 'none';
                assignedUserSelect.innerHTML = '<option value="">👤 Nenhum - Tarefa sem responsável</option>';
                return;
            }

            try {
                // Show loading state
                assignedUserSelect.innerHTML = '<option value="">Carregando membros...</option>';
                assignedUserContainer.style.display = 'block';
                
                const response = await fetch(`/api/squads/${squadId}/members`);
                if (!response.ok) {
                    throw new Error('Failed to fetch squad members');
                }

                const members = await response.json();
                
                // Clear and rebuild options with "Nenhum" as first option
                assignedUserSelect.innerHTML = '<option value="">👤 Nenhum - Tarefa sem responsável</option>';
                
                if (members.length === 0) {
                    const emptyOption = document.createElement('option');
                    emptyOption.disabled = true;
                    emptyOption.textContent = '(Nenhum membro encontrado)';
                    assignedUserSelect.appendChild(emptyOption);
                } else {
                    members.forEach(member => {
                        const option = document.createElement('option');
                        option.value = member.id;
                        const roleIcon = member.role === 'LEAD' ? '👑' : '👤';
                        option.textContent = `${roleIcon} ${member.name} (@${member.username})`;
                        assignedUserSelect.appendChild(option);
                    });
                }

                // Restore selected value if it exists in the new options
                if (currentAssignedUserId) {
                    const optionExists = Array.from(assignedUserSelect.options)
                        .some(opt => opt.value === currentAssignedUserId);
                    if (optionExists) {
                        assignedUserSelect.value = currentAssignedUserId;
                    }
                }

                // Show the container
                assignedUserContainer.style.display = 'block';
            } catch (error) {
                console.error('Error loading squad members:', error);
                assignedUserSelect.innerHTML = '<option value="">👤 Nenhum - Tarefa sem responsável</option>';
                assignedUserContainer.style.display = 'block';
            }
        }

        // Listen for squad changes
        squadSelect.addEventListener('change', function() {
            loadSquadMembers(this.value);
        });

        // Initial load if squad is already selected
        if (squadSelect.value) {
            loadSquadMembers(squadSelect.value);
        }
    }

    // Format seconds to HH:MM:SS
    function formatHMS(totalSec) {
        totalSec = Math.max(0, Math.floor(totalSec));
        const h = String(Math.floor(totalSec / 3600)).padStart(2, '0');
        const m = String(Math.floor((totalSec % 3600) / 60)).padStart(2, '0');
        const s = String(totalSec % 60).padStart(2, '0');
        return `${h}:${m}:${s}`;
    }

    // Format minutes to readable format
    function formatMinutes(min) {
        if (!min || min <= 0) return '—';
        const hours = Math.floor(min / 60);
        const mins = min % 60;
        if (hours > 0) {
            return `${hours}h ${mins}min`;
        }
        return `${mins}min`;
    }

    // Parse datetime string to Date object
    function parseDT(s) {
        if (!s) return null;
        const d = new Date(s);
        return isNaN(d.getTime()) ? null : d;
    }

    // Update execution panel timers
    function updateExecPanel() {
        const panel = document.getElementById('execPanel');
        if (!panel) return;

        const status = panel.dataset.status;
        const targetMin = parseInt(panel.dataset.targetMin) || 0;
        const extraMin = parseInt(panel.dataset.extraMin) || 0;
        const pomoMin = parseInt(panel.dataset.pomoMin) || 0;
        const breakMin = parseInt(panel.dataset.breakMin) || 0;
        const mainStarted = parseDT(panel.dataset.mainStarted);
        const mainElapsed = parseInt(panel.dataset.mainElapsed) || 0;
        const pomoUntil = parseDT(panel.dataset.pomoUntil);

        const mainElapsedLabel = document.getElementById('mainElapsedLabel');
        const pomoRemainingLabel = document.getElementById('pomoRemainingLabel');
        const pomoLabelText = document.getElementById('pomoLabelText');
        const execProgress = document.getElementById('execProgress');
        const targetLabel = document.getElementById('targetLabel');

        // Calculate total elapsed time
        let elapsed = mainElapsed;
        if (status === 'IN_PROGRESS' && mainStarted) {
            const now = new Date();
            const delta = Math.floor((now - mainStarted) / 1000);
            elapsed = mainElapsed + delta;
        }

        // Update main elapsed timer
        if (mainElapsedLabel) {
            mainElapsedLabel.textContent = formatHMS(elapsed);
        }

        // Update progress bar
        const totalTargetMin = targetMin + extraMin;
        if (execProgress && totalTargetMin > 0) {
            const targetSec = totalTargetMin * 60;
            const pct = Math.min(100, (elapsed / targetSec) * 100);
            execProgress.style.width = pct.toFixed(1) + '%';
            
            // Change color based on progress
            if (pct >= 100) {
                execProgress.style.backgroundColor = 'var(--danger, #dc3545)';
            } else if (pct >= 80) {
                execProgress.style.backgroundColor = 'var(--warning, #ffc107)';
            } else {
                execProgress.style.backgroundColor = 'var(--primary, #0d6efd)';
            }
        }

        // Update target label
        if (targetLabel) {
            if (totalTargetMin > 0) {
                targetLabel.textContent = `Alvo: ${formatMinutes(totalTargetMin)}`;
            } else {
                targetLabel.textContent = '—';
            }
        }

        // Update pomodoro/break timer
        if (pomoRemainingLabel && pomoLabelText) {
            if (status === 'IN_PAUSE') {
                // Break mode
                pomoLabelText.textContent = '☕ Pausa';
                if (pomoUntil) {
                    const now = new Date();
                    const remaining = Math.max(0, Math.floor((pomoUntil - now) / 1000));
                    if (remaining > 0) {
                        const mins = Math.floor(remaining / 60);
                        const secs = remaining % 60;
                        pomoRemainingLabel.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
                    } else {
                        pomoRemainingLabel.textContent = 'Retomar!';
                    }
                } else if (breakMin > 0) {
                    pomoRemainingLabel.textContent = `${breakMin}:00`;
                } else {
                    pomoRemainingLabel.textContent = '—';
                }
            } else if (status === 'IN_PROGRESS') {
                // Pomodoro mode
                pomoLabelText.textContent = '🍅 Pomodoro';
                if (pomoUntil) {
                    const now = new Date();
                    const remaining = Math.max(0, Math.floor((pomoUntil - now) / 1000));
                    if (remaining > 0) {
                        const mins = Math.floor(remaining / 60);
                        const secs = remaining % 60;
                        pomoRemainingLabel.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
                    } else {
                        pomoRemainingLabel.textContent = 'Intervalo!';
                    }
                } else if (pomoMin > 0) {
                    pomoRemainingLabel.textContent = `${pomoMin}:00`;
                } else {
                    pomoRemainingLabel.textContent = '—';
                }
            } else {
                // Not running
                pomoLabelText.textContent = '🍅 Pomodoro';
                pomoRemainingLabel.textContent = pomoMin > 0 ? `${pomoMin}:00` : '—';
            }
        }
    }

    // Fetch updated task data from server
    async function syncTaskData() {
        const panel = document.getElementById('execPanel');
        if (!panel) return;

        // Get task ID from the form action URL
        const form = document.querySelector('form[action*="/tasks/"]');
        if (!form) return;
        
        const actionUrl = form.getAttribute('action');
        const match = actionUrl.match(/\/tasks\/(\d+)/);
        if (!match) return;
        
        const taskId = match[1];

        try {
            const resp = await fetch(`/api/tasks/${taskId}/elapsed`);
            if (resp.ok) {
                const data = await resp.json();
                // Update panel data attributes with fresh server data
                if (data.seconds !== undefined) {
                    panel.dataset.mainElapsed = data.seconds;
                }
            }
        } catch (e) {
            console.warn('Failed to sync task elapsed time:', e);
        }
    }

    let timerIntervalId = null;
    let syncIntervalId = null;

    function startTimers() {
        if (timerIntervalId) return;
        
        // Update display every second
        timerIntervalId = setInterval(updateExecPanel, 1000);
        
        // Sync with server every 30 seconds
        syncIntervalId = setInterval(syncTaskData, 30000);
        
        // Initial update
        updateExecPanel();
        
        // Initial sync after short delay
        setTimeout(syncTaskData, 500);
    }

    function stopTimers() {
        if (timerIntervalId) {
            clearInterval(timerIntervalId);
            timerIntervalId = null;
        }
        if (syncIntervalId) {
            clearInterval(syncIntervalId);
            syncIntervalId = null;
        }
    }

    // Initialize on DOM ready
    document.addEventListener('DOMContentLoaded', function() {
        // Initialize Rich Editor
        initializeRichEditor();
        
        // Initialize Squad Member Selection
        initializeSquadMemberSelect();
        
        // Initialize timers
        const panel = document.getElementById('execPanel');
        if (panel) {
            startTimers();
        }
    });

    // Cleanup on page unload
    window.addEventListener('beforeunload', stopTimers);
})();

