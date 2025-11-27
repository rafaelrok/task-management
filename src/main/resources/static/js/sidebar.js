document.addEventListener("DOMContentLoaded", function () {
    const sidebar    = document.querySelector("nav.sidebar");
    if (!sidebar) return;

    const toggle     = sidebar.querySelector(".toggle");
    const searchBox  = sidebar.querySelector(".search-box");
    const modeSwitch = sidebar.querySelector(".toggle-switch");
    const modeText   = sidebar.querySelector(".mode-text");
    const html       = document.documentElement;

    // ---- THEME GLOBAL (usa :root[data-theme="dark"] do app.css) ----
    const storedTheme = localStorage.getItem("theme") || "light";
    html.setAttribute("data-theme", storedTheme);
    if (modeText) {
        modeText.innerText = storedTheme === "dark" ? "Light mode" : "Dark mode";
    }

    if (modeSwitch && modeText) {
        modeSwitch.addEventListener("click", () => {
            const current = html.getAttribute("data-theme") === "dark" ? "dark" : "light";
            const next = current === "dark" ? "light" : "dark";
            html.setAttribute("data-theme", next);
            localStorage.setItem("theme", next);
            modeText.innerText = next === "dark" ? "Light mode" : "Dark mode";
        });
    }

    // ---- TOGGLE DA SIDEBAR ----
    if (toggle) {
        toggle.addEventListener("click", () => {
            sidebar.classList.toggle("close");
        });
    }

    // ---- Clique na lupa reabre a sidebar se estiver fechada ----
    if (searchBox) {
        searchBox.addEventListener("click", () => {
            if (sidebar.classList.contains("close")) {
                sidebar.classList.remove("close");
            }
        });
    }
});