package br.com.rafaelvieira.taskmanagement.domain.enums;

/** Tipos de squads disponíveis no sistema */
public enum SquadType {
    BACKEND("Backend", "Desenvolvimento de APIs e serviços"),
    FRONTEND("Frontend", "Desenvolvimento de interfaces e experiência do usuário"),
    FULLSTACK("Fullstack", "Desenvolvimento full-stack"),
    DEVOPS("DevOps", "Infraestrutura, CI/CD e operações"),
    MOBILE("Mobile", "Desenvolvimento mobile (iOS/Android)"),
    QA("QA", "Quality Assurance e testes"),
    DATA("Data", "Engenharia e ciência de dados"),
    SECURITY("Security", "Segurança da informação"),
    PLATFORM("Platform", "Desenvolvimento de plataforma"),
    OTHER("Outro", "Outro tipo de squad");

    private final String displayName;
    private final String description;

    SquadType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
