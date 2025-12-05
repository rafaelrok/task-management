package br.com.rafaelvieira.taskmanagement.domain.records;

import br.com.rafaelvieira.taskmanagement.domain.enums.SquadType;

public record SquadUpdateDTO(
        String name,
        String description,
        SquadType type,
        String techStack,
        String businessArea,
        String goal,
        Integer maxMembers) {}
