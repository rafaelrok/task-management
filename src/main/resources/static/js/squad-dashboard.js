/**
 * Squad Dashboard JavaScript
 * Handles real-time timer updates and WebSocket connections for squad dashboard
 */

(function() {
    'use strict';

    // Timer update interval
    let timerInterval = null;

    /**
     * Initialize dashboard components
     */
    function initDashboard() {
        initTimers();
        initWebSocketConnection();
    }

    /**
     * Initialize and start timer updates for active tasks
     */
    function initTimers() {
        // Clear existing interval to prevent duplicates/leaks
        if (timerInterval) {
            clearInterval(timerInterval);
            timerInterval = null;
        }

        const timerBadges = document.querySelectorAll('.timer-badge');
        
        if (timerBadges.length === 0) {
            return;
        }

        // Update timers every second
        timerInterval = setInterval(function() {
            timerBadges.forEach(function(badge) {
                updateTimer(badge);
            });
        }, 1000);

        // Initial update
        timerBadges.forEach(function(badge) {
            updateTimer(badge);
        });
    }

    /**
     * Update a single timer display
     */
    function updateTimer(badge) {
        const startedAt = badge.getAttribute('data-started');
        const elapsed = parseInt(badge.getAttribute('data-elapsed') || '0', 10);
        const timerValue = badge.querySelector('.timer-value');

        if (!startedAt || !timerValue) {
            return;
        }

        // Parse the started time
        const startTime = new Date(startedAt);
        const now = new Date();
        
        // Calculate total elapsed time
        const runningSeconds = Math.floor((now - startTime) / 1000);
        const totalSeconds = elapsed + Math.max(0, runningSeconds);
        
        // Format as HH:MM:SS
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        
        timerValue.textContent = 
            String(hours).padStart(2, '0') + ':' +
            String(minutes).padStart(2, '0') + ':' +
            String(seconds).padStart(2, '0');

        // Update badge color based on time
        updateTimerColor(badge, totalSeconds);
    }

    /**
     * Update timer badge color based on elapsed time
     */
    function updateTimerColor(badge, totalSeconds) {
        // Get execution time from parent if available
        const parent = badge.closest('.list-group-item') || badge.closest('.timer-item');
        if (!parent) return;
        
        // Try to find the estimated time text
        // In the fragment it is: <small>Est: <span th:text="...">30</span> min</small>
        // The text content of small would be "Est: 30 min"
        const execTimeContainer = parent.querySelector('small');
        if (!execTimeContainer) return;
        
        const execTimeText = execTimeContainer.textContent;
        const match = execTimeText.match(/(\d+)/);
        if (!match) return;

        const estimatedMinutes = parseInt(match[1], 10);
        const estimatedSeconds = estimatedMinutes * 60;
        
        // Calculate percentage used
        const percentage = (totalSeconds / estimatedSeconds) * 100;
        
        // Remove existing color classes
        badge.classList.remove('bg-success', 'bg-warning', 'bg-danger', 'bg-secondary');
        
        if (percentage >= 100) {
            badge.classList.add('bg-danger');
            badge.classList.remove('text-dark');
        } else if (percentage >= 75) {
            badge.classList.add('bg-warning');
            badge.classList.add('text-dark');
        } else {
            badge.classList.add('bg-success');
            badge.classList.remove('text-dark');
        }
    }

    /**
     * Initialize WebSocket connection for real-time updates
     */
    function initWebSocketConnection() {
        // Check if SockJS and Stomp are available
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.log('WebSocket libraries not available');
            return;
        }

        try {
            const socket = new SockJS('/ws');
            const stompClient = Stomp.over(socket);
            
            // Disable debug logging in production
            stompClient.debug = function() {};

            stompClient.connect({}, function(frame) {
                console.log('Connected to WebSocket');
                
                // Subscribe to squad updates
                const squadId = getSquadId();
                if (squadId) {
                    stompClient.subscribe('/topic/squad/' + squadId + '/updates', function(message) {
                        handleSquadUpdate(JSON.parse(message.body));
                    });
                    
                    stompClient.subscribe('/topic/squad/' + squadId + '/tasks', function(message) {
                        handleTaskUpdate(JSON.parse(message.body));
                    });
                }
            }, function(error) {
                console.error('WebSocket connection error:', error);
            });
        } catch (e) {
            console.error('Error initializing WebSocket:', e);
        }
    }

    /**
     * Get current squad ID from URL
     */
    function getSquadId() {
        const match = window.location.pathname.match(/\/squads\/(\d+)/);
        return match ? match[1] : null;
    }

    /**
     * Handle squad update from WebSocket
     */
    function handleSquadUpdate(data) {
        console.log('Squad update received:', data);
        
        // Optionally refresh the page or update specific elements
        if (data.type === 'MEMBER_JOINED' || data.type === 'MEMBER_LEFT') {
            // Could update member count or refresh metrics
            updateMemberCount(data.memberCount);
        }
    }

    /**
     * Handle task update from WebSocket
     */
    function handleTaskUpdate(data) {
        console.log('Task update received:', data);
        
        // Update task status in the table if visible
        if (data.type === 'STATUS_CHANGED') {
            updateTaskRow(data.taskId, data.status);
        }
        
        // Refresh active timers if a task started or stopped
        if (data.type === 'TIMER_STARTED' || data.type === 'TIMER_STOPPED') {
            refreshActiveTasks();
        }
    }

    /**
     * Refresh the active tasks panel
     */
    function refreshActiveTasks() {
        const squadId = getSquadId();
        if (!squadId) return;

        fetch(`/squads/${squadId}/dashboard/active-tasks`)
            .then(response => {
                if (!response.ok) throw new Error('Network response was not ok');
                return response.text();
            })
            .then(html => {
                const container = document.getElementById('active-tasks-wrapper');
                if (container) {
                    container.innerHTML = html;
                    // Re-initialize timers for the new elements
                    initTimers();
                }
            })
            .catch(error => console.error('Error refreshing active tasks:', error));
    }

    /**
     * Update member count display
     */
    function updateMemberCount(count) {
        const memberCountElement = document.querySelector('.border-secondary .display-6');
        if (memberCountElement) {
            memberCountElement.textContent = count;
        }
    }

    /**
     * Update task row in the table
     */
    function updateTaskRow(taskId, status) {
        const row = document.getElementById('task-row-' + taskId);
        if (!row) return;

        const statusCell = row.cells[2];
        const badge = statusCell.querySelector('.status-badge');
        if (!badge) return;

        // Update text
        badge.textContent = status;

        // Update class
        badge.className = 'status-badge'; // reset
        if (status === 'DONE') {
            badge.classList.add('success');
            badge.textContent = 'Concluída';
        } else if (status === 'IN_PROGRESS') {
            badge.classList.add('warning');
            badge.textContent = 'Em Andamento';
        } else if (status === 'OVERDUE') {
            badge.classList.add('danger');
            badge.textContent = 'Atrasada';
        } else if (status === 'TODO') {
            badge.classList.add('secondary');
            badge.textContent = 'A Fazer';
        } else if (status === 'IN_PAUSE') {
            badge.classList.add('warning');
            badge.textContent = 'Em Pausa';
        } else if (status === 'CANCELLED') {
            badge.classList.add('secondary');
            badge.textContent = 'Cancelada';
        } else {
            badge.classList.add('secondary');
        }
    }

    /**
     * Cleanup on page unload
     */
    function cleanup() {
        if (timerInterval) {
            clearInterval(timerInterval);
        }
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initDashboard);
    } else {
        initDashboard();
    }

    // Cleanup on page unload
    window.addEventListener('beforeunload', cleanup);

})();
