package br.com.rafaelvieira.taskmanagement.domain.enums;

public enum Role {
    ADMIN,
    LEAD,
    MEMBER;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
