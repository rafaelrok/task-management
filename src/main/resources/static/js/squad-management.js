/**
 * Squad Management JavaScript
 * Handles squad invites, member management, and real-time updates
 */

// Squad Invite Management
const SquadInviteManager = {
  /**
   * Accept a squad invite
   */
  acceptInvite: async function (inviteId) {
    try {
      const response = await fetch(`/api/squads/invites/${inviteId}/accept`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        showToast("Convite aceito com sucesso!", "success");
        setTimeout(() => window.location.reload(), 1500);
      } else {
        const error = await response.text();
        showToast(`Erro ao aceitar convite: ${error}`, "error");
      }
    } catch (error) {
      console.error("Error accepting invite:", error);
      showToast("Erro ao aceitar convite", "error");
    }
  },

  /**
   * Reject a squad invite
   */
  rejectInvite: async function (inviteId) {
    try {
      const response = await fetch(`/api/squads/invites/${inviteId}/reject`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        showToast("Convite recusado", "info");
        setTimeout(() => window.location.reload(), 1500);
      } else {
        const error = await response.text();
        showToast(`Erro ao recusar convite: ${error}`, "error");
      }
    } catch (error) {
      console.error("Error rejecting invite:", error);
      showToast("Erro ao recusar convite", "error");
    }
  },

  /**
   * Load pending invites for current user
   */
  loadPendingInvites: async function () {
    try {
      const response = await fetch("/api/squads/my-invites");
      if (response.ok) {
        const invites = await response.json();
        this.renderInvites(invites);
        return invites;
      }
    } catch (error) {
      console.error("Error loading invites:", error);
    }
    return [];
  },

  /**
   * Render invites in the UI
   */
  renderInvites: function (invites) {
    const container = document.getElementById("squad-invites-container");
    if (!container) return;

    if (invites.length === 0) {
      container.innerHTML =
        '<p class="text-muted text-center">Nenhum convite pendente</p>';
      return;
    }

    container.innerHTML = invites
      .map(
        (invite) => `
            <div class="card mb-3 shadow-sm">
                <div class="card-body">
                    <h6 class="card-title">${invite.squad.name}</h6>
                    <p class="card-text text-muted small">${
                      invite.squad.description || "Sem descrição"
                    }</p>
                    <p class="small mb-2">
                        <i class="bi bi-person"></i> Convidado por: ${
                          invite.invitedBy.fullName
                        }
                    </p>
                    <div class="d-flex gap-2">
                        <button class="btn btn-success btn-sm" onclick="SquadInviteManager.acceptInvite(${
                          invite.id
                        })">
                            <i class="bi bi-check-lg"></i> Aceitar
                        </button>
                        <button class="btn btn-danger btn-sm" onclick="SquadInviteManager.rejectInvite(${
                          invite.id
                        })">
                            <i class="bi bi-x-lg"></i> Recusar
                        </button>
                    </div>
                </div>
            </div>
        `
      )
      .join("");
  },
};

// Squad Member Management
const SquadMemberManager = {
  /**
   * Remove a member from squad (LEAD only)
   */
  removeMember: async function (squadId, userId) {
    if (!confirm("Tem certeza que deseja remover este membro?")) {
      return;
    }

    try {
      const response = await fetch(`/api/squads/${squadId}/members/${userId}`, {
        method: "DELETE",
      });

      if (response.ok) {
        showToast("Membro removido com sucesso", "success");
        setTimeout(() => window.location.reload(), 1500);
      } else {
        const error = await response.text();
        showToast(`Erro ao remover membro: ${error}`, "error");
      }
    } catch (error) {
      console.error("Error removing member:", error);
      showToast("Erro ao remover membro", "error");
    }
  },
};

// Toast Notification Helper
function showToast(message, type = "info") {
  // Check if toast container exists
  let toastContainer = document.getElementById("toast-container");
  if (!toastContainer) {
    toastContainer = document.createElement("div");
    toastContainer.id = "toast-container";
    toastContainer.className = "position-fixed top-0 end-0 p-3";
    toastContainer.style.zIndex = "9999";
    document.body.appendChild(toastContainer);
  }

  const toastId = "toast-" + Date.now();
  const bgClass =
    {
      success: "bg-success",
      error: "bg-danger",
      warning: "bg-warning",
      info: "bg-info",
    }[type] || "bg-info";

  const toastHTML = `
        <div id="${toastId}" class="toast align-items-center text-white ${bgClass} border-0" role="alert">
            <div class="d-flex">
                <div class="toast-body">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

  toastContainer.insertAdjacentHTML("beforeend", toastHTML);
  const toastElement = document.getElementById(toastId);
  const toast = new bootstrap.Toast(toastElement, { delay: 3000 });
  toast.show();

  // Remove toast element after it's hidden
  toastElement.addEventListener("hidden.bs.toast", () => {
    toastElement.remove();
  });
}

// Initialize on page load
document.addEventListener("DOMContentLoaded", function () {
  // Load pending invites if container exists
  if (document.getElementById("squad-invites-container")) {
    SquadInviteManager.loadPendingInvites();
  }
});

// Export for use in other scripts
window.SquadInviteManager = SquadInviteManager;
window.SquadMemberManager = SquadMemberManager;
window.showToast = showToast;
