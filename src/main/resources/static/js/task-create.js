// Inicialização do editor e validação de datas para create.html
document.addEventListener("DOMContentLoaded", function () {
  console.log("Initializing rich editor for create page...");

  // Initialize Rich Editor
  if (typeof RichEditor === "function") {
    RichEditor("taskDescCreate", "description");
  } else {
    console.error("RichEditor function not found!");
  }

  // Initialize Squad Member Selection
  initializeSquadMemberSelect();

  // Date validation - prevent past dates
  const dueDateField = document.getElementById("dueDateField");
  const scheduledStartField = document.getElementById("scheduledStartField");

  // Set minimum date to current date/time
  function setMinDateTime() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    const hours = String(now.getHours()).padStart(2, "0");
    const minutes = String(now.getMinutes()).padStart(2, "0");
    const minDateTime = `${year}-${month}-${day}T${hours}:${minutes}`;

    if (dueDateField) {
      dueDateField.setAttribute("min", minDateTime);
    }
    if (scheduledStartField) {
      scheduledStartField.setAttribute("min", minDateTime);
    }
  }

  setMinDateTime();

  // Validate date is not in the past
  function validateDate(field, fieldName) {
    if (!field || !field.value) return true;

    const selectedDate = new Date(field.value);
    const now = new Date();

    if (selectedDate < now) {
      alert(`A ${fieldName} não pode ser anterior à data atual.`);
      field.value = "";
      return false;
    }
    return true;
  }

  if (dueDateField) {
    dueDateField.addEventListener("change", function () {
      validateDate(this, "data de vencimento");
    });
  }

  if (scheduledStartField) {
    scheduledStartField.addEventListener("change", function () {
      validateDate(this, "data de agendamento");
    });
  }

  // Sync rich editor and validate on form submit
  const form = document.querySelector("form");
  if (form) {
    form.addEventListener("submit", function (e) {
      // Sync rich editor content to hidden field
      const hiddenField = document.getElementById("taskDescCreate_hidden");
      const editorArea = document.querySelector("#taskDescCreate .editor-area");
      if (hiddenField && editorArea) {
        hiddenField.value = editorArea.innerHTML;
        console.log(
          "Synced description on submit:",
          hiddenField.value.substring(0, 100)
        );
      }

      // Validate dates
      let isValid = true;

      if (dueDateField && !validateDate(dueDateField, "data de vencimento")) {
        isValid = false;
      }

      if (
        scheduledStartField &&
        !validateDate(scheduledStartField, "data de agendamento")
      ) {
        isValid = false;
      }

      if (!isValid) {
        e.preventDefault();
      }
    });
  }

  // Initialize Squad Member Selection
  function initializeSquadMemberSelect() {
    const squadSelect = document.getElementById("squadSelect");
    const assignedUserContainer = document.getElementById(
      "assignedUserContainer"
    );
    const assignedUserSelect = document.getElementById("assignedUserSelect");

    if (!squadSelect || !assignedUserSelect || !assignedUserContainer) {
      console.warn(
        "Squad selection elements not found. User might not have permission or elements are missing."
      );
      return;
    }

    // Function to load squad members
    async function loadSquadMembers(squadId) {
      console.log("Loading members for squad:", squadId);

      if (!squadId) {
        assignedUserContainer.style.display = "none";
        assignedUserSelect.innerHTML =
          '<option value="">👤 Nenhum - Tarefa sem responsável</option>';
        return;
      }

      try {
        // Show loading state
        assignedUserSelect.innerHTML =
          '<option value="">Carregando membros...</option>';
        assignedUserContainer.style.display = "block";

        const response = await fetch(`/api/squads/${squadId}/members`);
        if (!response.ok) {
          throw new Error(`Failed to fetch squad members: ${response.status}`);
        }

        const members = await response.json();
        console.log("Members loaded:", members);

        // Clear and rebuild options with "Nenhum" as first option
        assignedUserSelect.innerHTML =
          '<option value="">👤 Nenhum - Tarefa sem responsável</option>';
        if (members.length === 0) {
          const emptyOption = document.createElement("option");
          emptyOption.disabled = true;
          emptyOption.textContent = "(Nenhum membro encontrado)";
          assignedUserSelect.appendChild(emptyOption);
        } else {
          members.forEach((member) => {
            const option = document.createElement("option");
            option.value = member.id;
            const roleIcon = member.role === "LEAD" ? "👑" : "👤";
            option.textContent = `${roleIcon} ${member.name} (@${member.username})`;
            assignedUserSelect.appendChild(option);
          });

          // Restore selection if exists (e.g. after validation error)
          const selectedUser = assignedUserSelect.dataset.selectedUser;
          if (selectedUser) {
            assignedUserSelect.value = selectedUser;
            console.log("Restored selected user:", selectedUser);
          }
        }
      } catch (error) {
        console.error("Error loading squad members:", error);
        assignedUserSelect.innerHTML =
          '<option value="">Erro ao carregar membros</option>';
        // Keep container visible so user sees the error
        assignedUserContainer.style.display = "block";
      }
    }

    // Listen for squad changes
    squadSelect.addEventListener("change", function () {
      loadSquadMembers(this.value);
    });

    // Initial load if squad is already selected
    if (squadSelect.value) {
      loadSquadMembers(squadSelect.value);
    }
  }
});
