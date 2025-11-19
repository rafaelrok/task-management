package br.com.rafaelvieira.taskmanagement.controller;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Task Management", description = "Endpoints para gerenciamento completo de tarefas")
@RequestMapping("/api/tasks")
public interface SwaggerTaskController {

    @Operation(summary = "Criar nova tarefa", description = "Cria uma nova tarefa no sistema")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "201", description = "Tarefa criada com sucesso"),
                @ApiResponse(responseCode = "400", description = "Dados inválidos"),
                @ApiResponse(responseCode = "422", description = "Entidade não processável")
            })
    @PostMapping
    ResponseEntity<@NotNull TaskRecord> createTask(
            @Parameter(description = "Dados para criação da tarefa", required = true)
                    @Valid
                    @RequestBody
                    TaskCreateRecord taskCreate);

    @Operation(
            summary = "Atualizar tarefa",
            description = "Atualiza os dados de uma tarefa existente")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Tarefa atualizada com sucesso"),
                @ApiResponse(responseCode = "404", description = "Tarefa não encontrada"),
                @ApiResponse(responseCode = "400", description = "Dados inválidos")
            })
    @PutMapping("/{id}")
    ResponseEntity<@NotNull TaskRecord> updateTask(
            @Parameter(description = "ID da tarefa", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Novos dados da tarefa", required = true) @Valid @RequestBody
                    TaskCreateRecord taskCreate);

    @Operation(
            summary = "Buscar tarefa por ID",
            description = "Recupera os detalhes de uma tarefa específica")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Tarefa encontrada"),
                @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
            })
    @GetMapping("/{id}")
    ResponseEntity<@NotNull TaskRecord> getTaskById(
            @Parameter(description = "ID da tarefa", required = true) @PathVariable("id") Long id);

    @Operation(
            summary = "Listar todas as tarefas",
            description = "Recupera uma lista com todas as tarefas cadastradas")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Lista de tarefas recuperada com sucesso"),
                @ApiResponse(responseCode = "204", description = "Nenhuma tarefa encontrada")
            })
    @GetMapping
    ResponseEntity<@NotNull List<TaskRecord>> getAllTasks();

    @Operation(
            summary = "Buscar tarefas por status",
            description = "Recupera tarefas filtradas pelo status")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Lista de tarefas recuperada"),
                @ApiResponse(responseCode = "400", description = "Status inválido")
            })
    @GetMapping("/status/{status}")
    ResponseEntity<@NotNull List<TaskRecord>> getTasksByStatus(
            @Parameter(
                            description = "Status da tarefa (TODO, IN_PROGRESS, DONE, CANCELLED)",
                            required = true)
                    @PathVariable("status")
                    TaskStatus status);

    @Operation(
            summary = "Buscar tarefas por prioridade",
            description = "Recupera tarefas filtradas pela prioridade")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Lista de tarefas recuperada"),
                @ApiResponse(responseCode = "400", description = "Prioridade inválida")
            })
    @GetMapping("/priority/{priority}")
    ResponseEntity<@NotNull List<TaskRecord>> getTasksByPriority(
            @Parameter(
                            description = "Prioridade da tarefa (LOW, MEDIUM, HIGH, URGENT)",
                            required = true)
                    @PathVariable("priority")
                    Priority priority);

    @Operation(
            summary = "Buscar tarefas por categoria",
            description = "Recupera todas as tarefas de uma categoria específica")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Lista de tarefas recuperada"),
                @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
            })
    @GetMapping("/category/{categoryId}")
    ResponseEntity<@NotNull List<TaskRecord>> getTasksByCategoryId(
            @Parameter(description = "ID da categoria", required = true) @PathVariable("categoryId")
                    Long categoryId);

    @Operation(
            summary = "Buscar tarefas por usuário",
            description = "Recupera todas as tarefas atribuídas a um usuário")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Lista de tarefas recuperada"),
                @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
            })
    @GetMapping("/user/{userId}")
    ResponseEntity<@NotNull List<TaskRecord>> getTasksByUserId(
            @Parameter(description = "ID do usuário", required = true) @PathVariable("userId")
                    Long userId);

    @Operation(
            summary = "Buscar tarefas atrasadas",
            description = "Recupera todas as tarefas que passaram da data de vencimento")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Lista de tarefas atrasadas recuperada")
            })
    @GetMapping("/overdue")
    ResponseEntity<@NotNull List<TaskRecord>> getOverdueTasks();

    @Operation(
            summary = "Alterar status da tarefa",
            description = "Atualiza apenas o status de uma tarefa")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
                @ApiResponse(responseCode = "404", description = "Tarefa não encontrada"),
                @ApiResponse(responseCode = "400", description = "Status inválido")
            })
    @PatchMapping("/{id}/status")
    ResponseEntity<@NotNull TaskRecord> changeTaskStatus(
            @Parameter(description = "ID da tarefa", required = true) @PathVariable("id") Long id,
            @Parameter(description = "Novo status da tarefa", required = true)
                    @RequestParam("status")
                    TaskStatus status);

    @Operation(summary = "Deletar tarefa", description = "Remove uma tarefa do sistema")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Tarefa deletada com sucesso"),
                @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
            })
    @DeleteMapping("/{id}")
    ResponseEntity<@NotNull Void> deleteTask(
            @Parameter(description = "ID da tarefa", required = true) @PathVariable("id") Long id);

    @Operation(
            summary = "Contar tarefas por status",
            description = "Retorna o número de tarefas com um status específico")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Contagem realizada com sucesso"),
                @ApiResponse(responseCode = "400", description = "Status inválido")
            })
    @GetMapping("/count/{status}")
    ResponseEntity<@NotNull Long> countTasksByStatus(
            @Parameter(description = "Status para contagem", required = true)
                    @PathVariable("status")
                    TaskStatus status);

    @Operation(
            summary = "Tempo decorrido da tarefa",
            description = "Retorna o tempo total decorrido em formato legível e em segundos")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Tempo retornado")})
    @GetMapping("/{id}/elapsed")
    ResponseEntity<Map<String, Object>> getReadableElapsed(@PathVariable("id") Long id);

    @Operation(
            summary = "Histórico de pomodoros",
            description = "Lista sessões de pomodoro para a tarefa")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Lista retornada")})
    @GetMapping("/{id}/pomodoros")
    ResponseEntity<List<Map<String, Object>>> getPomodoroHistory(@PathVariable("id") Long id);

    @Operation(
            summary = "Abortar pomodoro em execução",
            description = "Finaliza a sessão de pomodoro atual e retoma a tarefa em IN_PROGRESS")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Pomodoro abortado")})
    @PatchMapping("/{id}/pomodoro/abort")
    ResponseEntity<TaskRecord> abortPomodoro(@PathVariable("id") Long id);
}
