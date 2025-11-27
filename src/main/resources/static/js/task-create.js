// JavaScript extraído de create.html
    });
        }
            });
                }
                    e.preventDefault();
                if (!isValid) {

                }
                    isValid = false;
                if (scheduledStartField && !validateDate(scheduledStartField, 'data de agendamento')) {

                }
                    isValid = false;
                if (dueDateField && !validateDate(dueDateField, 'data de vencimento')) {

                let isValid = true;
            form.addEventListener('submit', function (e) {
        if (form) {
        const form = document.querySelector('form');
        // Validate on form submit

        }
            });
                validateDate(this, 'data de agendamento');
            scheduledStartField.addEventListener('change', function () {
        if (scheduledStartField) {

        }
            });
                validateDate(this, 'data de vencimento');
            dueDateField.addEventListener('change', function () {
        if (dueDateField) {

        }
            return true;
            }
                return false;
                field.value = '';
                alert(`A ${fieldName} não pode ser anterior à data atual.`);
            if (selectedDate < now) {

            const now = new Date();
            const selectedDate = new Date(field.value);

            if (!field || !field.value) return true;
        function validateDate(field, fieldName) {
        // Validate on change

        setMinDateTime();

        }
            }
                scheduledStartField.setAttribute('min', minDateTime);
            if (scheduledStartField) {
            }
                dueDateField.setAttribute('min', minDateTime);
            if (dueDateField) {

            const minDateTime = `${year}-${month}-${day}T${hours}:${minutes}`;
            const minutes = String(now.getMinutes()).padStart(2, '0');
            const hours = String(now.getHours()).padStart(2, '0');
            const day = String(now.getDate()).padStart(2, '0');
            const month = String(now.getMonth() + 1).padStart(2, '0');
            const year = now.getFullYear();
            // Format: YYYY-MM-DDTHH:MM
            const now = new Date();
        function setMinDateTime() {
        // Set minimum date to current date/time

        const scheduledStartField = document.getElementById('scheduledStartField');
        const dueDateField = document.getElementById('dueDateField');
        // Date validation - prevent past dates

        }
            console.error('RichEditor function not found!');
        } else {
            RichEditor('taskDescCreate', 'description');
        if (typeof RichEditor === 'function') {
        console.log('Initializing rich editor for create page...');
    document.addEventListener('DOMContentLoaded', function () {
// Initialize rich editor after DOM is fully loaded
// ...
// Inicialização do editor e validação de datas

