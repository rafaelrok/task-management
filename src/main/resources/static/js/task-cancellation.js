/**
 * Task Cancellation JavaScript
 * Handles task cancellation requests with approval flow
 */

const TaskCancellationManager = {
    currentTaskId: null,

    /**
     * Show cancellation modal
     */
    showCancellationModal: function(taskId, taskTitle) {
        this.currentTaskId = taskId;
        
        // Update modal content
        document.getElementById('cancel-task-title').textContent = taskTitle;
        document.getElementById('cancel-reason').value = '';
        
        // Show modal
        const modal = new bootstrap.Modal(document.getElementById('taskCancellationModal'));
        modal.show();
    },

    /**
     * Submit cancellation request
     */
    submitCancellationRequest: async function() {
        const reason = document.getElementById('cancel-reason').value.trim();
        
        if (!reason) {
            showToast('Por favor, informe o motivo do cancelamento', 'warning');
            return;
        }

        if (reason.length < 10) {
            showToast('O motivo deve ter pelo menos 10 caracteres', 'warning');
            return;
        }

        try {
            const response = await fetch(`/api/tasks/${this.currentTaskId}/request-cancel`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ reason })
            });

            if (response.ok) {
                showToast('Pedido de cancelamento enviado para aprovação', 'success');
                
                // Close modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('taskCancellationModal'));
                modal.hide();
                
                // Reload page after delay
                setTimeout(() => window.location.reload(), 2000);
            } else {
                const error = await response.text();
                showToast(`Erro ao solicitar cancelamento: ${error}`, 'error');
            }
        } catch (error) {
            console.error('Error requesting cancellation:', error);
            showToast('Erro ao solicitar cancelamento', 'error');
        }
    },

    /**
     * Approve cancellation request (LEAD only)
     */
    approveCancellation: async function(requestId) {
        if (!confirm('Aprovar este pedido de cancelamento?')) {
            return;
        }

        try {
            const response = await fetch(`/api/cancel-requests/${requestId}/approve`, {
                method: 'POST'
            });

            if (response.ok) {
                showToast('Cancelamento aprovado', 'success');
                setTimeout(() => window.location.reload(), 1500);
            } else {
                const error = await response.text();
                showToast(`Erro ao aprovar: ${error}`, 'error');
            }
        } catch (error) {
            console.error('Error approving cancellation:', error);
            showToast('Erro ao aprovar cancelamento', 'error');
        }
    },

    /**
     * Reject cancellation request (LEAD only)
     */
    rejectCancellation: async function(requestId) {
        const reason = prompt('Motivo da rejeição (opcional):');
        
        try {
            const response = await fetch(`/api/cancel-requests/${requestId}/reject`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ reason: reason || '' })
            });

            if (response.ok) {
                showToast('Cancelamento rejeitado', 'info');
                setTimeout(() => window.location.reload(), 1500);
            } else {
                const error = await response.text();
                showToast(`Erro ao rejeitar: ${error}`, 'error');
            }
        } catch (error) {
            console.error('Error rejecting cancellation:', error);
            showToast('Erro ao rejeitar cancelamento', 'error');
        }
    },

    /**
     * Load pending cancellation requests for LEAD
     */
    loadPendingRequests: async function() {
        try {
            const response = await fetch('/api/cancel-requests/pending');
            if (response.ok) {
                const requests = await response.json();
                this.renderPendingRequests(requests);
                return requests;
            }
        } catch (error) {
            console.error('Error loading cancellation requests:', error);
        }
        return [];
    },

    /**
     * Render pending cancellation requests
     */
    renderPendingRequests: function(requests) {
        const container = document.getElementById('pending-cancellations-container');
        if (!container) return;

        if (requests.length === 0) {
            container.innerHTML = '<p class="text-muted text-center">Nenhum pedido pendente</p>';
            return;
        }

        container.innerHTML = requests.map(request => `
            <div class="card mb-3 shadow-sm">
                <div class="card-body">
                    <h6 class="card-title">${request.task.title}</h6>
                    <p class="card-text"><strong>Solicitado por:</strong> ${request.requestedBy.fullName}</p>
                    <p class="card-text"><strong>Motivo:</strong></p>
                    <p class="card-text text-muted">${request.reason}</p>
                    <p class="small text-muted">
                        <i class="bi bi-clock"></i> ${new Date(request.createdAt).toLocaleString('pt-BR')}
                    </p>
                    <div class="d-flex gap-2">
                        <button class="btn btn-success btn-sm" onclick="TaskCancellationManager.approveCancellation(${request.id})">
                            <i class="bi bi-check-lg"></i> Aprovar
                        </button>
                        <button class="btn btn-danger btn-sm" onclick="TaskCancellationManager.rejectCancellation(${request.id})">
                            <i class="bi bi-x-lg"></i> Rejeitar
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    }
};

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Load pending requests if container exists
    if (document.getElementById('pending-cancellations-container')) {
        TaskCancellationManager.loadPendingRequests();
    }
});

// Export for use in other scripts
window.TaskCancellationManager = TaskCancellationManager;
