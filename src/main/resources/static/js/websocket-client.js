/**
 * WebSocket Client for Real-time Dashboard Updates
 * Conecta ao servidor via STOMP/WebSocket e recebe atualizações em tempo real
 * Versão: 3.0 (Com polling fallback e refresh agressivo)
 */

class WebSocketClient {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.reconnectDelay = 3000;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 15;
        this.pollingInterval = null;
        this.lastUpdateTime = Date.now();
    }

    /**
     * Conecta ao servidor WebSocket
     */
    connect() {
        console.log('[WebSocket] Iniciando conexão...');
        this.updateConnectionStatus('Conectando...');

        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.error('[WebSocket] Bibliotecas SockJS ou Stomp não carregadas!');
            this.updateConnectionStatus('Erro Libs');
            this.startPollingFallback();
            return;
        }

        try {
            const socket = new SockJS('/ws-task-management');
            this.stompClient = Stomp.over(socket);

            // Desabilita debug verboso
            this.stompClient.debug = null;

            this.stompClient.connect(
                {},
                (frame) => this.onConnected(frame),
                (error) => this.onError(error)
            );
        } catch (e) {
            console.error('[WebSocket] Erro ao criar conexão:', e);
            this.startPollingFallback();
        }
    }

    /**
     * Callback executado quando conectado
     */
    onConnected(frame) {
        console.log('[WebSocket] ✅ Conectado com sucesso!');
        this.connected = true;
        this.reconnectAttempts = 0;
        this.updateConnectionStatus('Conectado');
        this.stopPollingFallback();

        // Subscreve ao tópico de atualizações do dashboard
        this.stompClient.subscribe('/topic/dashboard', (message) => {
            try {
                const data = JSON.parse(message.body);
                this.onDashboardUpdate(data);
            } catch (e) {
                console.error('[WebSocket] Erro ao parsear mensagem:', e);
            }
        });

        // Subscreve ao tópico de notificações (Broadcast)
        this.stompClient.subscribe('/topic/notifications', (message) => {
            try {
                const data = JSON.parse(message.body);
                this.onNotificationReceived(data);
            } catch (e) {
                console.error('[WebSocket] Erro ao parsear notificação broadcast:', e);
            }
        });

        // Subscreve à fila de notificações do usuário (Pessoal)
        this.stompClient.subscribe('/user/queue/notifications', (message) => {
            try {
                const data = JSON.parse(message.body);
                this.onNotificationReceived(data);
            } catch (e) {
                console.error('[WebSocket] Erro ao parsear notificação pessoal:', e);
            }
        });

        console.log('[WebSocket] 📡 Subscrito aos tópicos: /topic/dashboard, /topic/notifications, /user/queue/notifications');
        
        // Força refresh imediato após conectar
        this.forceRefreshAll();
    }

    /**
     * Callback executado quando há erro
     */
    onError(error) {
        console.error('[WebSocket] ❌ Erro na conexão:', error);
        this.connected = false;
        this.updateConnectionStatus('Desconectado');

        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`[WebSocket] 🔄 Reconectando (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            this.updateConnectionStatus(`Reconectando...`);
            setTimeout(() => this.connect(), this.reconnectDelay);
        } else {
            console.warn('[WebSocket] Usando polling fallback...');
            this.startPollingFallback();
        }
    }

    /**
     * Inicia polling como fallback caso WebSocket falhe
     */
    startPollingFallback() {
        if (this.pollingInterval) return;
        
        console.log('[WebSocket] 📊 Iniciando polling fallback a cada 5s...');
        this.pollingInterval = setInterval(() => {
            this.pollDashboardData();
        }, 5000);
        
        // Executa imediatamente
        this.pollDashboardData();
    }

    stopPollingFallback() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
            console.log('[WebSocket] Polling fallback parado');
        }
    }

    /**
     * Busca dados do dashboard via HTTP (fallback)
     */
    async pollDashboardData() {
        try {
            const response = await fetch('/api/dashboard/stats');
            if (response.ok) {
                const data = await response.json();
                this.onDashboardUpdate({
                    totalTodo: data.todo,
                    totalInProgress: data.inProgress,
                    totalDone: data.done,
                    totalCancelled: data.cancelled,
                    countLow: data.countLow || 0,
                    countMedium: data.countMedium || 0,
                    countHigh: data.countHigh || 0,
                    countUrgent: data.countUrgent || 0
                });
            }
        } catch (e) {
            console.error('[WebSocket] Erro no polling:', e);
        }
    }

    /**
     * Atualiza os KPIs do dashboard com os dados recebidos
     */
    onDashboardUpdate(data) {
        const now = Date.now();
        console.log('[WebSocket] 📥 Dashboard update recebido:', data);
        this.lastUpdateTime = now;

        try {
            // Atualiza KPIs de Status
            this.updateElement('val-total-todo', data.totalTodo);
            this.updateElement('val-total-in-progress', data.totalInProgress);
            this.updateElement('val-total-done', data.totalDone);
            this.updateElement('val-total-cancelled', data.totalCancelled);

            // Atualiza KPIs de Prioridade
            this.updateElement('val-count-low', data.countLow);
            this.updateElement('val-count-medium', data.countMedium);
            this.updateElement('val-count-high', data.countHigh);
            this.updateElement('val-count-urgent', data.countUrgent);

            // Atualiza gráficos
            if (typeof window.updateChartsWithData === 'function') {
                window.updateChartsWithData({
                    todo: data.totalTodo || 0,
                    inProgress: data.totalInProgress || 0,
                    done: data.totalDone || 0,
                    cancelled: data.totalCancelled || 0
                });
            }

            // Refresh fragmentos HTML
            this.forceRefreshAll();

            // Indicador visual
            this.animateUpdate();
        } catch (error) {
            console.error('[WebSocket] Erro ao processar update:', error);
        }
    }

    /**
     * Força refresh de todos os componentes do sistema
     */
    async forceRefreshAll() {
        console.log('[WebSocket] 🔄 Iniciando refresh completo do sistema...');
        
        try {
            // 1. Refresh do Dashboard completo (se disponível)
            if (typeof window.refreshDashboard === 'function') {
                await window.refreshDashboard();
                console.log('[WebSocket] ✅ Dashboard refreshed');
            }

            // 2. Refresh de Active Tasks (fallback)
            if (typeof window.refreshActiveTasks === 'function') {
                await window.refreshActiveTasks();
                console.log('[WebSocket] ✅ Active tasks refreshed');
            }

            // 3. Refresh de Scheduled Tasks
            if (typeof window.refreshScheduledTasks === 'function') {
                await window.refreshScheduledTasks();
                console.log('[WebSocket] ✅ Scheduled tasks refreshed');
            }

            // 4. Refresh de Overdue Tasks
            if (typeof window.refreshOverdueTasks === 'function') {
                await window.refreshOverdueTasks();
                console.log('[WebSocket] ✅ Overdue tasks refreshed');
            }

            // 5. Refresh de Due Today Tasks
            if (typeof window.refreshDueTodayTasks === 'function') {
                await window.refreshDueTodayTasks();
                console.log('[WebSocket] ✅ Due today tasks refreshed');
            }

            // 6. Atualiza gráficos (se disponível)
            if (typeof window.refreshCharts === 'function') {
                window.refreshCharts();
                console.log('[WebSocket] ✅ Charts refreshed');
            }

            // 7. Recarrega notificações sticky
            if (typeof NotificationManager !== 'undefined' && NotificationManager.loadStickyNotifications) {
                NotificationManager.loadStickyNotifications();
                console.log('[WebSocket] ✅ Sticky notifications reloaded');
            }

            // 8. Atualiza badge de notificações
            if (typeof NotificationManager !== 'undefined' && NotificationManager.loadUnreadCount) {
                NotificationManager.loadUnreadCount();
                console.log('[WebSocket] ✅ Notification badge updated');
            }

            console.log('[WebSocket] ✅ Refresh completo finalizado');
        } catch (error) {
            console.error('[WebSocket] ❌ Erro durante refresh:', error);
        }
    }

    /**
     * Atualiza um elemento específico pelo ID
     */
    updateElement(id, value) {
        const el = document.getElementById(id);
        if (el) {
            const current = el.textContent.trim();
            const newValue = String(value ?? 0);

            if (current !== newValue) {
                console.log(`[WebSocket] 🔄 ${id}: ${current} → ${newValue}`);
                el.textContent = newValue;
                this.addPulseEffect(el);
            }
        }
    }

    /**
     * Adiciona efeito de pulso ao elemento atualizado
     */
    addPulseEffect(element) {
        element.classList.remove('pulse-update');
        void element.offsetWidth;
        element.classList.add('pulse-update');
        setTimeout(() => element.classList.remove('pulse-update'), 1000);
    }

    /**
     * Animação global de atualização
     */
    animateUpdate() {
        const indicator = document.getElementById('ws-update-indicator');
        if (indicator) {
            indicator.classList.remove('active');
            void indicator.offsetWidth;
            indicator.classList.add('active');
        }
    }

    /**
     * Atualiza o status visual da conexão na UI
     */
    updateConnectionStatus(status) {
        const el = document.getElementById('ws-connection-status');
        if (el) {
            el.textContent = status;
            el.className = 'badge rounded-pill';
            if (status === 'Conectado') {
                el.classList.add('text-bg-success');
            } else if (status.includes('Conectando') || status.includes('Reconectando')) {
                el.classList.add('text-bg-warning');
            } else {
                el.classList.add('text-bg-danger');
            }
        }
    }

    /**
     * Processa notificação recebida
     */
    onNotificationReceived(data) {
        console.log('[WebSocket] 🔔 Notificação:', data);
        if (typeof window.handleWebSocketNotification === 'function') {
            window.handleWebSocketNotification(data);
        }
        // Também força refresh ao receber notificação
        this.forceRefreshAll();
    }

    /**
     * Desconecta do servidor
     */
    disconnect() {
        this.stopPollingFallback();
        if (this.stompClient !== null && this.connected) {
            this.stompClient.disconnect(() => {
                console.log('[WebSocket] Desconectado');
                this.updateConnectionStatus('Desconectado');
            });
            this.connected = false;
        }
    }
}

// Instância global
let wsClient = null;

// Inicializa quando o DOM estiver pronto
document.addEventListener('DOMContentLoaded', function () {
    console.log('[WebSocket] 🚀 Inicializando cliente v3.0...');
    wsClient = new WebSocketClient();
    wsClient.connect();

    // Desconecta quando a página é fechada
    window.addEventListener('beforeunload', function () {
        if (wsClient) wsClient.disconnect();
    });
});

// Exporta para uso global
window.wsClient = wsClient;
