/**
 * Gamification WebSocket Client
 * Handles real-time notifications for badges, points, squad invites, and cancellation requests
 */

class GamificationWebSocket {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.subscriptions = [];
    }

    /**
     * Connect to WebSocket
     */
    connect() {
        const socket = new SockJS('/ws-task-management');
        this.stompClient = Stomp.over(socket);
        
        // Disable debug logging in production
        this.stompClient.debug = null;

        this.stompClient.connect({}, (frame) => {
            console.log('Connected to WebSocket:', frame);
            this.connected = true;
            this.subscribeToChannels();
        }, (error) => {
            console.error('WebSocket connection error:', error);
            this.connected = false;
            // Retry connection after 5 seconds
            setTimeout(() => this.connect(), 5000);
        });
    }

    /**
     * Subscribe to all relevant channels
     */
    subscribeToChannels() {
        // Subscribe to badge notifications
        this.subscribe('/user/queue/badges', (message) => {
            this.handleBadgeEarned(JSON.parse(message.body));
        });

        // Subscribe to points notifications
        this.subscribe('/user/queue/points', (message) => {
            this.handlePointsEarned(JSON.parse(message.body));
        });

        // Subscribe to squad invite notifications
        this.subscribe('/user/queue/squad-invites', (message) => {
            this.handleSquadInvite(JSON.parse(message.body));
        });

        // Subscribe to cancellation request notifications (for LEADs)
        this.subscribe('/user/queue/cancel-requests', (message) => {
            this.handleCancellationRequest(JSON.parse(message.body));
        });

        // Subscribe to cancellation decision notifications
        this.subscribe('/user/queue/cancel-decisions', (message) => {
            this.handleCancellationDecision(JSON.parse(message.body));
        });

        // Subscribe to squad-specific topics if on squad page
        const squadId = this.getSquadIdFromUrl();
        if (squadId) {
            this.subscribeToSquad(squadId);
        }
    }

    /**
     * Subscribe to a channel
     */
    subscribe(channel, callback) {
        if (this.stompClient && this.connected) {
            const subscription = this.stompClient.subscribe(channel, callback);
            this.subscriptions.push(subscription);
            return subscription;
        }
    }

    /**
     * Subscribe to squad-specific channels
     */
    subscribeToSquad(squadId) {
        this.subscribe(`/topic/squad/${squadId}`, (message) => {
            this.handleSquadUpdate(JSON.parse(message.body));
        });

        this.subscribe(`/topic/squad/${squadId}/ranking`, (message) => {
            this.handleRankingUpdate(JSON.parse(message.body));
        });
    }

    /**
     * Handle badge earned notification
     */
    handleBadgeEarned(data) {
        console.log('Badge earned:', data);
        
        // Show animated toast with badge
        this.showBadgeToast(data);
        
        // Play sound effect (optional)
        this.playSound('badge-earned');
        
        // Update badge count in UI
        this.updateBadgeCount();
        
        // Trigger confetti animation
        this.triggerConfetti();
    }

    /**
     * Handle points earned notification
     */
    handlePointsEarned(data) {
        console.log('Points earned:', data);
        
        // Show points toast
        const message = `+${data.points} pontos! ${data.reason}`;
        this.showAnimatedToast(message, 'success', 'bi-star-fill');
        
        // Update points display in UI
        this.updatePointsDisplay();
    }

    /**
     * Handle squad invite notification
     */
    handleSquadInvite(data) {
        console.log('Squad invite received:', data);
        
        const message = `Você foi convidado para a squad "${data.squadName}" por ${data.invitedBy}`;
        this.showAnimatedToast(message, 'info', 'bi-people-fill');
        
        // Update invites badge
        this.updateInvitesBadge();
        
        // Play notification sound
        this.playSound('notification');
    }

    /**
     * Handle cancellation request notification (for LEADs)
     */
    handleCancellationRequest(data) {
        console.log('Cancellation request received:', data);
        
        const message = `${data.requestedBy} solicitou cancelamento de "${data.taskTitle}"`;
        this.showAnimatedToast(message, 'warning', 'bi-exclamation-triangle-fill');
        
        // Update pending requests count
        this.updateCancellationRequestsCount();
        
        // Play notification sound
        this.playSound('notification');
    }

    /**
     * Handle cancellation decision notification
     */
    handleCancellationDecision(data) {
        console.log('Cancellation decision:', data);
        
        const status = data.approved ? 'aprovado' : 'rejeitado';
        const message = `Seu pedido de cancelamento de "${data.taskTitle}" foi ${status}`;
        const type = data.approved ? 'success' : 'danger';
        
        this.showAnimatedToast(message, type, 'bi-info-circle-fill');
    }

    /**
     * Handle squad update notification
     */
    handleSquadUpdate(data) {
        console.log('Squad update:', data);
        
        if (data.type === 'SQUAD_MEMBER_JOINED') {
            const message = `${data.memberName} entrou na squad!`;
            this.showAnimatedToast(message, 'info', 'bi-person-plus-fill');
            
            // Reload members list if on squad details page
            if (window.location.pathname.includes('/squads/')) {
                setTimeout(() => window.location.reload(), 2000);
            }
        }
    }

    /**
     * Handle ranking update notification
     */
    handleRankingUpdate(data) {
        console.log('Ranking updated:', data);
        
        // Reload ranking if on ranking page
        if (window.location.pathname.includes('/ranking')) {
            setTimeout(() => window.location.reload(), 1000);
        }
    }

    /**
     * Show animated badge toast
     */
    showBadgeToast(badgeData) {
        const toastHTML = `
            <div class="toast align-items-center text-white bg-gradient-success border-0 badge-toast" role="alert">
                <div class="d-flex">
                    <div class="toast-body">
                        <div class="d-flex align-items-center">
                            <i class="${badgeData.badgeIcon} fs-1 me-3 badge-icon-animated"></i>
                            <div>
                                <strong class="d-block">${badgeData.badgeName}</strong>
                                <small>${badgeData.badgeDescription}</small>
                            </div>
                        </div>
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;
        
        this.showToastHTML(toastHTML, 5000);
    }

    /**
     * Show animated toast with icon
     */
    showAnimatedToast(message, type, icon) {
        const bgClass = {
            'success': 'bg-success',
            'danger': 'bg-danger',
            'warning': 'bg-warning',
            'info': 'bg-info'
        }[type] || 'bg-info';

        const toastHTML = `
            <div class="toast align-items-center text-white ${bgClass} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="bi ${icon} me-2"></i>
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;
        
        this.showToastHTML(toastHTML, 4000);
    }

    /**
     * Show toast HTML
     */
    showToastHTML(html, delay = 3000) {
        let container = document.getElementById('ws-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'ws-toast-container';
            container.className = 'position-fixed top-0 end-0 p-3';
            container.style.zIndex = '9999';
            document.body.appendChild(container);
        }

        container.insertAdjacentHTML('beforeend', html);
        const toastElement = container.lastElementChild;
        const toast = new bootstrap.Toast(toastElement, { delay });
        toast.show();

        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }

    /**
     * Play sound effect
     */
    playSound(soundName) {
        // Implement sound playing if audio files are available
        // const audio = new Audio(`/sounds/${soundName}.mp3`);
        // audio.play().catch(e => console.log('Could not play sound:', e));
    }

    /**
     * Trigger confetti animation
     */
    triggerConfetti() {
        // Implement confetti animation if library is available
        // if (window.confetti) {
        //     confetti({
        //         particleCount: 100,
        //         spread: 70,
        //         origin: { y: 0.6 }
        //     });
        // }
    }

    /**
     * Update badge count in UI
     */
    updateBadgeCount() {
        // Implement badge count update
        const badgeCountElement = document.getElementById('user-badge-count');
        if (badgeCountElement) {
            const currentCount = parseInt(badgeCountElement.textContent) || 0;
            badgeCountElement.textContent = currentCount + 1;
        }
    }

    /**
     * Update points display in UI
     */
    updatePointsDisplay() {
        // Reload points if on profile page
        if (window.location.pathname.includes('/profile')) {
            setTimeout(() => window.location.reload(), 2000);
        }
    }

    /**
     * Update invites badge
     */
    updateInvitesBadge() {
        const invitesBadge = document.getElementById('squad-invites-badge');
        if (invitesBadge) {
            const currentCount = parseInt(invitesBadge.textContent) || 0;
            invitesBadge.textContent = currentCount + 1;
            invitesBadge.classList.remove('d-none');
        }
    }

    /**
     * Update cancellation requests count
     */
    updateCancellationRequestsCount() {
        const requestsBadge = document.getElementById('cancel-requests-badge');
        if (requestsBadge) {
            const currentCount = parseInt(requestsBadge.textContent) || 0;
            requestsBadge.textContent = currentCount + 1;
            requestsBadge.classList.remove('d-none');
        }
    }

    /**
     * Get squad ID from current URL
     */
    getSquadIdFromUrl() {
        const match = window.location.pathname.match(/\/squads\/(\d+)/);
        return match ? match[1] : null;
    }

    /**
     * Disconnect from WebSocket
     */
    disconnect() {
        if (this.stompClient && this.connected) {
            this.subscriptions.forEach(sub => sub.unsubscribe());
            this.stompClient.disconnect();
            this.connected = false;
        }
    }
}

// Initialize WebSocket connection on page load
let gamificationWS = null;

document.addEventListener('DOMContentLoaded', function() {
    gamificationWS = new GamificationWebSocket();
    gamificationWS.connect();
});

// Disconnect on page unload
window.addEventListener('beforeunload', function() {
    if (gamificationWS) {
        gamificationWS.disconnect();
    }
});

// Export for use in other scripts
window.GamificationWebSocket = GamificationWebSocket;
window.gamificationWS = gamificationWS;
