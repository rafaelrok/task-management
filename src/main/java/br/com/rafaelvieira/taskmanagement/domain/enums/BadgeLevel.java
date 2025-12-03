package br.com.rafaelvieira.taskmanagement.domain.enums;

/** Níveis de selos mensais baseados em produtividade */
public enum BadgeLevel {
    NONE("Sem Selo", 0, "badge-none", "#6c757d"),
    BRONZE("Bronze", 1, "badge-bronze", "#cd7f32"),
    PRATA("Prata", 11, "badge-silver", "#c0c0c0"),
    OURO("Ouro", 26, "badge-gold", "#ffd700"),
    DIAMANTE("Diamante", 46, "badge-diamond", "#b9f2ff");

    private final String displayName;
    private final int minTasks;
    private final String cssClass;
    private final String color;

    BadgeLevel(String displayName, int minTasks, String cssClass, String color) {
        this.displayName = displayName;
        this.minTasks = minTasks;
        this.cssClass = cssClass;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinTasks() {
        return minTasks;
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getColor() {
        return color;
    }

    /** Calcula o nível do badge com base no número de tasks concluídas */
    public static BadgeLevel fromTaskCount(int tasksCompleted) {
        if (tasksCompleted >= DIAMANTE.minTasks) {
            return DIAMANTE;
        } else if (tasksCompleted >= OURO.minTasks) {
            return OURO;
        } else if (tasksCompleted >= PRATA.minTasks) {
            return PRATA;
        } else if (tasksCompleted >= BRONZE.minTasks) {
            return BRONZE;
        }
        return NONE;
    }

    /** Retorna o ícone Bootstrap para o nível */
    public String getIcon() {
        return switch (this) {
            case DIAMANTE -> "bi-gem";
            case OURO -> "bi-trophy-fill";
            case PRATA -> "bi-award-fill";
            case BRONZE -> "bi-award";
            case NONE -> "bi-circle";
        };
    }
}
