/**
 * Sistema Robusto de Notificações com Toast
 * Gerencia todos os tipos de notificações do sistema
 */
const NotificationManager = {
  // Configurações de duração
  DURATIONS: {
    SHORT: 4000,
    MEDIUM: 7000,
    LONG: 10000,
    STICKY: false // não fecha automaticamente
  },

  // Tipos de notificação e suas configurações
  TYPES: {
    TASK_CREATED: { icon: 'bi-plus-circle', color: 'info', duration: 'MEDIUM', sticky: false },
    TASK_STARTED: { icon: 'bi-play-circle', color: 'info', duration: 'MEDIUM', sticky: false },
    TASK_PAUSED: { icon: 'bi-pause-circle', color: 'warning', duration: 'STICKY', sticky: true, requiresConfirmation: true },
    TASK_RESUMED: { icon: 'bi-play-circle', color: 'info', duration: 'MEDIUM', sticky: false },
    TASK_FINISHED: { icon: 'bi-check-circle', color: 'success', duration: 'LONG', sticky: false },
    TASK_CANCELLED: { icon: 'bi-x-circle', color: 'danger', duration: 'LONG', sticky: false, requiresConfirmation: true },
    TASK_TIME_UP: { icon: 'bi-alarm', color: 'danger', duration: 'STICKY', sticky: true },
    TASK_PENDING: { icon: 'bi-clock-history', color: 'primary', duration: 'STICKY', sticky: true },
    TASK_OVERDUE: { icon: 'bi-exclamation-triangle', color: 'danger', duration: 'STICKY', sticky: true }
  },

  // Armazena toasts ativos
  activeToasts: new Map(),

  /**
   * Inicializa o sistema de notificações
   */
  init() {
    this.ensureContainer();
    this.loadStickyNotifications();
    console.log('[NotificationManager] Sistema inicializado');
  },

  /**
   * Garante que o container de toasts existe
   */
  ensureContainer() {
    if (!document.getElementById('toastContainer')) {
      const container = document.createElement('div');
      container.id = 'toastContainer';
      container.className = 'position-fixed top-0 end-0 p-3';
      container.style.zIndex = '9999';
      container.style.marginTop = '70px';
      document.body.appendChild(container);
    }
  },

  /**
   * Carrega notificações sticky ao iniciar
   */
  loadStickyNotifications() {
    // Não carregar em páginas de perfil
    if (window.location.pathname.includes('/profile')) {
      return;
    }

    fetch('/api/notifications/sticky')
      .then(res => res.ok ? res.json() : [])
      .then(notifications => {
        notifications.forEach((notif, index) => {
          setTimeout(() => this.show(notif), index * 300);
        });
      })
      .catch(err => console.warn('Erro ao carregar sticky notifications:', err));
  },

  /**
   * Exibe uma notificação
   */
  show(notification) {
    // Não exibir sticky toasts na página de perfil
    if (window.location.pathname.includes('/profile') && notification.sticky) {
      return;
    }

    const config = this.TYPES[notification.type] || this.TYPES.TASK_CREATED;
    const duration = config.duration === 'STICKY' ? false : this.DURATIONS[config.duration];
    
    const toastId = `toast-${notification.id}`;
    
    // Remove toast existente se houver
    if (this.activeToasts.has(toastId)) {
      this.remove(toastId);
    }

    const toast = this.createToastElement(notification, config, toastId, duration);
    document.getElementById('toastContainer').appendChild(toast);

    const bsToast = new bootstrap.Toast(toast, {
      autohide: duration !== false,
      delay: duration || 0
    });

    bsToast.show();
    this.activeToasts.set(toastId, { element: toast, instance: bsToast, notification });

    // Remove do mapa quando esconder
    toast.addEventListener('hidden.bs.toast', () => {
      this.activeToasts.delete(toastId);
      toast.remove();
    });
  },

  /**
   * Cria elemento HTML do toast
   */
  createToastElement(notification, config, toastId, duration) {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const isSticky = config.sticky;
    
    const toast = document.createElement('div');
    toast.id = toastId;
    toast.className = `toast ${isDark ? 'bg-dark text-white' : 'bg-white text-dark'}`;
    if (isSticky) toast.classList.add('notification-toast-sticky');
    if (notification.type === 'TASK_PENDING') toast.classList.add('pending');
    
    toast.setAttribute('role', 'alert');
    toast.setAttribute('data-notification-id', notification.id);

    const headerClass = `toast-header bg-${config.color} text-white`;
    const stickyBadge = isSticky ? 
      `<span class="notification-sticky-badge ${notification.type === 'TASK_PENDING' ? 'pending' : ''}">
        <i class="bi bi-pin-fill notification-sticky-icon"></i> Fixada
      </span>` : '';

    const stickyIndicator = isSticky ?
      `<div class="notification-requires-action ${notification.type === 'TASK_PENDING' ? 'pending' : ''}">
        <i class="bi bi-pin-angle-fill"></i>
        <span>Esta notificação requer sua ação</span>
      </div>` : '';

    // Botões de ação para TASK_TIME_UP
    const actionButtons = notification.type === 'TASK_TIME_UP' ?
      `<div class="d-flex gap-2 mt-2">
        <button onclick="NotificationManager.finishTask(${notification.taskId}, ${notification.id})" 
                class="btn btn-sm btn-success flex-fill">
          <i class="bi bi-check-circle"></i> Finalizar
        </button>
        <button onclick="NotificationManager.cancelTask(${notification.taskId}, ${notification.id})" 
                class="btn btn-sm btn-danger flex-fill">
          <i class="bi bi-x-circle"></i> Cancelar
        </button>
      </div>` : '';

    toast.innerHTML = `
      <div class="${headerClass}">
        <i class="bi ${config.icon} me-2"></i>
        <strong class="me-auto">${notification.title}</strong>
        ${stickyBadge}
        <small class="ms-2">${this.formatTime(notification.createdAt)}</small>
        <button type="button" class="btn-close btn-close-white ms-2" data-bs-dismiss="toast"></button>
      </div>
      <div class="toast-body">
        <p class="mb-1">${notification.message}</p>
        ${stickyIndicator}
        ${actionButtons}
        <div class="mt-2 pt-2 border-top d-flex justify-content-between align-items-center">
          <a href="/tasks/${notification.taskId}" class="btn btn-sm btn-outline-primary">Ver Tarefa</a>
          <button onclick="NotificationManager.markAsRead(${notification.id})" 
                  class="btn btn-sm btn-link text-decoration-none" title="Marcar como lida">
            <i class="bi bi-check2-all"></i>
          </button>
        </div>
      </div>
    `;

    return toast;
  },

  /**
   * Marca notificação como lida
   */
  async markAsRead(notificationId) {
    try {
      const response = await fetch(`/api/notifications/${notificationId}/read`, { method: 'POST' });
      if (response.ok) {
        this.remove(`toast-${notificationId}`);
        // Atualiza badge
        if (typeof NotificationSystem !== 'undefined' && NotificationSystem.updateBadgeCount) {
          NotificationSystem.updateBadgeCount(-1, true);
        }
      }
    } catch (error) {
      console.error('Erro ao marcar como lida:', error);
    }
  },

  /**
   * Remove um toast
   */
  remove(toastId) {
    const toastData = this.activeToasts.get(toastId);
    if (toastData) {
      toastData.instance.hide();
      this.activeToasts.delete(toastId);
    }
  },

  /**
   * Finaliza tarefa da notificação
   */
  async finishTask(taskId, notificationId) {
    try {
      const response = await fetch(`/api/tasks/${taskId}/status?status=DONE`, { method: 'PATCH' });
      if (response.ok) {
        this.markAsRead(notificationId);
        this.showSimpleToast('success', '✅ Tarefa finalizada com sucesso!', 10000);
        if (typeof window.refreshDashboard === 'function') {
          window.refreshDashboard();
        }
      }
    } catch (error) {
      this.showSimpleToast('danger', '❌ Erro ao finalizar tarefa', 5000);
    }
  },

  /**
   * Cancela tarefa com confirmação
   */
  async cancelTask(taskId, notificationId) {
    const confirmed = await this.showConfirmModal(
      'Cancelar Tarefa',
      'Tem certeza que deseja CANCELAR esta tarefa? Esta ação NÃO PODE ser revertida!',
      'danger'
    );

    if (!confirmed) return;

    try {
      const response = await fetch(`/api/tasks/${taskId}/status?status=CANCELLED`, { method: 'PATCH' });
      if (response.ok) {
        this.markAsRead(notificationId);
        this.showSimpleToast('danger', '✅ Tarefa cancelada', 10000, 'toast-cancel-confirm');
        if (typeof window.refreshDashboard === 'function') {
          window.refreshDashboard();
        }
      }
    } catch (error) {
      this.showSimpleToast('danger', '❌ Erro ao cancelar tarefa', 5000);
    }
  },

  /**
   * Exibe modal de confirmação
   */
  showConfirmModal(title, message, type = 'warning') {
    return new Promise((resolve) => {
      const modalId = 'confirmModal_' + Date.now();
      const iconClass = type === 'danger' ? 'bi-exclamation-triangle' : 'bi-question-circle';
      const btnClass = type === 'danger' ? 'btn-danger' : 'btn-warning';

      const modalHTML = `
        <div class="modal fade" id="${modalId}" tabindex="-1">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title">
                  <i class="bi ${iconClass} me-2"></i>${title}
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
              </div>
              <div class="modal-body">
                <div class="text-center mb-3">
                  <i class="bi ${iconClass} warning-icon"></i>
                </div>
                <p class="text-center">${message}</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                <button type="button" class="btn ${btnClass}" id="${modalId}_confirm">Confirmar</button>
              </div>
            </div>
          </div>
        </div>
      `;

      document.body.insertAdjacentHTML('beforeend', modalHTML);
      const modalEl = document.getElementById(modalId);
      const modal = new bootstrap.Modal(modalEl);

      document.getElementById(`${modalId}_confirm`).addEventListener('click', () => {
        modal.hide();
        resolve(true);
      });

      modalEl.addEventListener('hidden.bs.modal', () => {
        modalEl.remove();
        resolve(false);
      });

      modal.show();
    });
  },

  /**
   * Toast simples para feedback
   */
  showSimpleToast(type, message, duration = 4000, extraClass = '') {
    const colors = {
      success: 'bg-success',
      danger: 'bg-danger',
      warning: 'bg-warning',
      info: 'bg-info'
    };

    const toast = document.createElement('div');
    toast.className = `toast ${colors[type]} text-white ${extraClass}`;
    toast.innerHTML = `
      <div class="toast-body d-flex justify-content-between align-items-center">
        <span>${message}</span>
        <button type="button" class="btn-close btn-close-white ms-2" data-bs-dismiss="toast"></button>
      </div>
    `;

    document.getElementById('toastContainer').appendChild(toast);
    const bsToast = new bootstrap.Toast(toast, { autohide: true, delay: duration });
    bsToast.show();

    toast.addEventListener('hidden.bs.toast', () => toast.remove());
  },

  /**
   * Formata hora
   */
  formatTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  }
};

// Inicializa quando DOM estiver pronto
document.addEventListener('DOMContentLoaded', () => {
  NotificationManager.init();
});

// Expõe globalmente
window.NotificationManager = NotificationManager;

// Integração com WebSocket
window.handleWebSocketNotification = function(data) {
  console.log('[WebSocket] Nova notificação:', data);
  NotificationManager.show(data);
  
  // Atualiza badge
  if (typeof NotificationSystem !== 'undefined' && NotificationSystem.updateBadgeCount) {
    NotificationSystem.updateBadgeCount(1, true);
  }
};
