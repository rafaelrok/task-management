# Implementação de Extensão de Tempo de Tarefas

## Resumo das Alterações

Esta implementação adiciona a funcionalidade de estender o tempo de execução de uma tarefa quando o tempo principal
termina.

### 1. Banco de Dados

**Arquivo:** `V16__add_extra_time_fields.sql`

Adicionadas duas novas colunas à tabela `tasks`:

- `extra_time_minutes` (INTEGER): Armazena o tempo extra adicionado em minutos
- `extension_justification` (TEXT): Armazena a justificativa para a extensão

### 2. Model

**Arquivo:** `Task.java`

Adicionados dois novos campos:

- `extraTimeMinutes`: Tempo extra em minutos
- `extensionJustification`: Justificativa para extensão de tempo

### 3. Records

**Arquivo:** `TaskRecord.java`

- Atualizado para incluir `extraTimeMinutes` e `extensionJustification`

**Arquivo:** `TaskExtensionRecord.java` (NOVO)

- Record para encapsular os dados de extensão de tarefa
- Campos:
    - `extraTimeMinutes`: Tempo extra (obrigatório, mínimo 1 minuto)
    - `justification`: Justificativa (opcional, máximo 2000 caracteres)
    - `scheduledStartAt`: Nova data de início (opcional)
    - `dueDate`: Nova data de vencimento (opcional)

### 4. Service

**Arquivo:** `TaskService.java`

- Adicionado método `extendTask(Long id, TaskExtensionRecord extension)`

**Arquivo:** `TaskServiceImpl.java`

- Implementação do método `extendTask`:
    - Adiciona o tempo extra ao tempo de execução
    - Atualiza as datas se fornecidas
    - Salva a justificativa (concatena se já houver uma anterior)
    - Reseta o status de OVERDUE para TODO para permitir reiniciar a tarefa
    - Limpa o pomodoroUntil para reset do timer

### 5. Controller

**Arquivo:** `TaskController.java`

- Adicionado endpoint `PATCH /api/tasks/{id}/extend`
- Aceita `TaskExtensionRecord` no body
- Retorna o `TaskRecord` atualizado

### 6. Frontend - JavaScript

**Arquivo:** `dashboard.js`

#### Alterações na função `updateCard`:

- Atualizada para detectar status `OVERDUE`
- Quando o tempo termina (timeDone):
    - Para o timer
    - Desabilita todos os botões exceto "Finalizar" e "Estender"
    - Mostra o botão "Estender Tempo"

#### Alterações na função `cardTemplate`:

- Adicionado botão "Estender Tempo" com ícone de relógio
- Botão inicialmente oculto (display:none)

#### Alterações na função `wireActions`:

- Adicionado handler para ação 'extend'
- Chama `openExtendModal(id)` quando clicado

#### Alterações na função `getActive`:

- Adiciona busca por tarefas com status `OVERDUE`
- Inclui essas tarefas na lista de tarefas ativas

#### Novas funções:

**`openExtendModal(taskId)`**:

- Carrega os dados da tarefa via API
- Preenche o modal com informações da tarefa
- Define valores padrão para os campos de data
- Exibe o modal

**`submitExtendTask()`**:

- Coleta os dados do formulário
- Valida tempo extra
- Envia requisição PATCH para `/api/tasks/{id}/extend`
- Recarrega a página após sucesso
- Exibe mensagens de erro/sucesso

### 7. Frontend - HTML

**Arquivo:** `dashboard.html`

#### Modal de Extensão (`extendTaskModal`):

- Campo para tempo extra (obrigatório)
- Campo para nova data de início
- Campo para nova data de vencimento
- Campo para justificativa/observação
- Botão "Estender Tarefa"
- Botão "Fechar"
- Mensagem informativa sobre o funcionamento

#### Seção de Tarefas Ativas:

- Adicionado botão "Estender Tempo" no template server-side
- Botão inicialmente oculto, será mostrado pelo JavaScript quando apropriado

## Fluxo de Uso

1. Uma tarefa está em execução (IN_PROGRESS)
2. O tempo de execução termina (elapsed >= executionTimeMinutes * 60)
3. O sistema:
    - Para o timer principal
    - Para os pomodoros ativos
    - Muda status para OVERDUE
    - Desabilita botões de controle (exceto Finalizar e Estender)
    - Mostra o botão "Estender Tempo"
4. Usuário clica em "Estender Tempo"
5. Modal é aberto com:
    - Título da tarefa
    - Campo para informar tempo extra em minutos
    - Campos para ajustar datas de início e vencimento
    - Campo de justificativa
6. Usuário preenche e clica em "Estender Tarefa"
7. Sistema:
    - Adiciona tempo extra ao tempo de execução
    - Atualiza datas se fornecidas
    - Salva justificativa
    - Reseta status para TODO
    - Limpa timer de pomodoro
8. Tarefa volta ao card padrão, pronta para ser reiniciada

## Melhorias Implementadas

1. **Rastreabilidade**: Todas as extensões são registradas com justificativa
2. **Flexibilidade**: Permite ajustar datas junto com a extensão
3. **Histórico**: Justificativas são concatenadas, mantendo histórico
4. **UX**: Modal claro e intuitivo com validações
5. **Feedback**: Mensagens de sucesso/erro para o usuário
6. **Estado visual**: Indicadores visuais claros quando tempo termina

## Próximos Passos (Opcional)

1. Adicionar histórico de extensões em uma tabela separada
2. Adicionar relatórios de tarefas com extensões
3. Adicionar limite de extensões permitidas
4. Adicionar notificações quando tarefa precisa extensão
5. Adicionar dashboard com métricas de extensões
