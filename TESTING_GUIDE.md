# Guia de Testes - Extensão de Tempo de Tarefas

## 🧪 Cenários de Teste

### Teste 1: Fluxo Completo de Extensão

#### Pré-requisitos:

- Aplicação rodando
- Usuário autenticado
- Uma tarefa com tempo de execução e pomodoro configurados

#### Passos:

1. Criar uma tarefa com:
    - Tempo de execução: 1 minuto (para teste rápido)
    - Pomodoro: 1 minuto
    - Status: TODO

2. No Dashboard, clicar em "Iniciar" na tarefa

3. Aguardar 1 minuto

4. Verificar que:
    - ✅ Timer para automaticamente
    - ✅ Status muda para OVERDUE
    - ✅ Barra de progresso fica em 100%
    - ✅ Ícone de info (ℹ️) aparece no título
    - ✅ Botão "Estender Tempo" fica visível
    - ✅ Outros botões ficam desabilitados (exceto Finalizar)

5. Clicar no botão "Estender Tempo"

6. Verificar que:
    - ✅ Modal abre
    - ✅ Título da tarefa está correto
    - ✅ Campos de data estão pré-preenchidos

7. Preencher:
    - Tempo extra: 2 minutos
    - Justificativa: "Teste de extensão - necessário mais tempo"
    - Deixar datas como estão

8. Clicar em "Estender Tarefa"

9. Verificar que:
    - ✅ Modal fecha
    - ✅ Página recarrega
    - ✅ Tarefa volta ao estado TODO
    - ✅ Tempo de execução agora é 3 minutos (1 + 2)

10. Iniciar a tarefa novamente

11. Verificar que:
    - ✅ Timer começa do zero
    - ✅ Barra de progresso considera os 3 minutos totais

### Teste 2: Validações do Modal

#### Teste 2.1: Campo Obrigatório

1. Abrir modal de extensão
2. Deixar campo "Tempo Extra" vazio
3. Clicar em "Estender Tarefa"
4. Verificar: ✅ Mensagem de erro aparece

#### Teste 2.2: Valor Mínimo

1. Abrir modal de extensão
2. Informar 0 ou valor negativo no tempo extra
3. Clicar em "Estender Tarefa"
4. Verificar: ✅ Validação impede envio

#### Teste 2.3: Justificativa Longa

1. Abrir modal de extensão
2. Informar justificativa com mais de 2000 caracteres
3. Verificar: ✅ Sistema aceita ou trunca adequadamente

### Teste 3: Múltiplas Extensões

#### Passos:

1. Criar tarefa com 1 minuto de execução
2. Iniciar e aguardar terminar
3. Estender com 1 minuto e justificativa "Primeira extensão"
4. Iniciar novamente e aguardar terminar
5. Estender com 1 minuto e justificativa "Segunda extensão"
6. Abrir a tarefa via API ou banco de dados
7. Verificar:
    - ✅ `extra_time_minutes` = 2
    - ✅ `execution_time_minutes` = 3
    - ✅ `extension_justification` contém ambas separadas por "---"

### Teste 4: Atualização de Datas

#### Passos:

1. Criar tarefa com datas específicas
2. Aguardar tempo terminar
3. Abrir modal de extensão
4. Alterar data de início e vencimento
5. Estender tarefa
6. Verificar:
    - ✅ `scheduled_start_at` atualizado
    - ✅ `due_date` atualizado

### Teste 5: API Endpoint

#### Usando curl ou Postman:

```bash
# 1. Obter tarefa
curl -X GET http://localhost:8080/api/tasks/1 \
  -H "Cookie: JSESSIONID=..." \
  -H "Accept: application/json"

# 2. Estender tarefa
curl -X PATCH http://localhost:8080/api/tasks/1/extend \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=..." \
  -d '{
    "extraTimeMinutes": 30,
    "justification": "Teste via API",
    "scheduledStartAt": "2025-11-19T14:00:00",
    "dueDate": "2025-11-19T18:00:00"
  }'

# 3. Verificar atualização
curl -X GET http://localhost:8080/api/tasks/1 \
  -H "Cookie: JSESSIONID=..." \
  -H "Accept: application/json"
```

### Teste 6: Banco de Dados

#### Verificar migração aplicada:

```sql
-- Verificar colunas adicionadas
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'tasks'
  AND column_name IN ('extra_time_minutes', 'extension_justification');

-- Verificar dados após extensão
SELECT id, title, execution_time_minutes, extra_time_minutes, extension_justification
FROM tasks
WHERE extra_time_minutes > 0;
```

### Teste 7: Comportamento do Frontend

#### Teste 7.1: Botão Estender Oculto Inicialmente

1. Abrir dashboard
2. Verificar tarefa em andamento
3. Verificar: ✅ Botão "Estender" não está visível

#### Teste 7.2: Botão Estender Aparece no Tempo Certo

1. Aguardar tempo terminar
2. Verificar: ✅ Botão aparece automaticamente

#### Teste 7.3: Modal Fecha ao Clicar em Fechar

1. Abrir modal de extensão
2. Clicar em "Fechar"
3. Verificar: ✅ Modal fecha sem salvar

#### Teste 7.4: Refresh Após Salvar

1. Estender tarefa
2. Verificar: ✅ Página recarrega automaticamente

### Teste 8: Status OVERDUE

#### Verificar listagem:

1. Criar tarefa e aguardar tempo terminar
2. Verificar que aparece na lista de tarefas ativas
3. Verificar via API:

```bash
curl -X GET http://localhost:8080/api/tasks/status/OVERDUE \
  -H "Cookie: JSESSIONID=..." \
  -H "Accept: application/json"
```

### Teste 9: Integração com Pomodoro

#### Passos:

1. Criar tarefa com 2 minutos de execução e 1 minuto de pomodoro
2. Iniciar tarefa
3. Aguardar 1 minuto (1º pomodoro termina)
4. Verificar pausa/intervalo
5. Continuar e aguardar terminar os 2 minutos
6. Verificar:
    - ✅ Timer principal para
    - ✅ Timer de pomodoro para
    - ✅ Sessões de pomodoro são finalizadas
    - ✅ Botão estender aparece

### Teste 10: Edge Cases

#### Teste 10.1: Extensão com Valor Grande

- Tempo extra: 999999 minutos
- Verificar: Sistema aceita e funciona

#### Teste 10.2: Extensão Sem Justificativa

- Deixar justificativa vazia
- Verificar: ✅ Sistema permite

#### Teste 10.3: Múltiplos Usuários

- Usuário A estende tarefa
- Usuário B visualiza tarefa
- Verificar: ✅ Dados consistentes

#### Teste 10.4: Extensão Durante Execução

- Tarefa em IN_PROGRESS mas ainda não terminou
- Tentar abrir modal de extensão
- Verificar: ✅ Botão não está disponível

## 🐛 Possíveis Problemas e Soluções

### Problema 1: Modal não abre

**Solução**: Verificar console do navegador para erros JavaScript

### Problema 2: Dados não salvam

**Solução**: Verificar network tab no DevTools, checar resposta da API

### Problema 3: Timer não para

**Solução**: Verificar se status está mudando para OVERDUE no backend

### Problema 4: Botão não aparece

**Solução**: Verificar classe CSS `time-finished` e lógica JavaScript

### Problema 5: Migração não aplicada

**Solução**:

```bash
# Verificar logs do Flyway
# Ou executar manualmente:
gradlew flywayMigrate
```

## 📊 Métricas de Teste

Ao completar todos os testes, você deve ter:

- ✅ Pelo menos 1 tarefa com extensão de tempo
- ✅ Justificativas salvas no banco
- ✅ Tempo extra acumulado corretamente
- ✅ Histórico de múltiplas extensões
- ✅ Datas atualizadas conforme esperado

## 🎯 Checklist de Qualidade

- [ ] Timer para automaticamente quando tempo termina
- [ ] Status muda para OVERDUE
- [ ] Botão "Estender" aparece no momento certo
- [ ] Modal abre corretamente
- [ ] Todos os campos do modal funcionam
- [ ] Validações impedem dados inválidos
- [ ] Justificativa é salva no banco
- [ ] Tempo extra é somado corretamente
- [ ] Tarefa volta ao estado TODO após extensão
- [ ] Tarefa pode ser reiniciada
- [ ] Timer reseta corretamente
- [ ] Múltiplas extensões funcionam
- [ ] Histórico de justificativas é mantido
- [ ] Datas são atualizadas
- [ ] API endpoint funciona
- [ ] Frontend e backend estão sincronizados

---

**Última atualização**: 2025-11-19
