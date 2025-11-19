# 📊 Estados Completos dos Cards - Dashboard

## ✅ Implementação Final - Todos os Estados

---

## 🎨 Mapeamento Visual Completo

### 1. 🔴 OVERDUE (Prazo Vencido)

**Condição**: `status === 'OVERDUE'`

**Visual:**

- 🎨 **Fundo**: Vermelho claro (`rgba(239, 68, 68, 0.15)`)
- 🔲 **Borda**: Vermelha grossa 2px (`rgba(239, 68, 68, 0.5)`)
- 🔔 **Ícone**: `bi-alarm` (relógio/alarme) em vermelho escuro (`#dc2626`)
- 🏷️ **Badge**: "PENDENTE FINALIZAÇÃO" (vermelho, pulsante)
- 📊 **Classe CSS**: `.overdue-state`

**Botões:**

- ✅ **Finalizar** - HABILITADO
- ✅ **Cancelar** - HABILITADO
- ✅ **Estender Tempo** - HABILITADO e VISÍVEL
- ❌ **Iniciar** - DESABILITADO
- ❌ **Pausar** - DESABILITADO
- ✅ **Abrir Task** - HABILITADO

---

### 2. 🟡 IN_PAUSE (Em Pausa)

**Condição**: `status === 'IN_PAUSE'`

**Visual:**

- 🎨 **Fundo**: Amarelo claro (`rgba(255, 221, 128, 0.18)`)
- 🔲 **Borda**: Amarela grossa 2px (`rgba(255, 196, 64, 0.45)`)
- ⏸️ **Ícone**: `bi-pause-circle` (pausa) em amarelo
- 📊 **Classe CSS**: `.in-pause`

**Botões:**

- ✅ **Iniciar/Retomar** - HABILITADO
- ✅ **Cancelar** - HABILITADO
- ✅ **Finalizar** - HABILITADO
- ❌ **Pausar** - DESABILITADO
- ❌ **Estender** - OCULTO
- ✅ **Abrir Task** - HABILITADO

---

### 3. 🔵 IN_PROGRESS - Tempo Finalizado

**Condição**: `status === 'IN_PROGRESS' && elapsed >= executionTime`

**Visual:**

- 🎨 **Fundo**: Azul claro (`rgba(59, 130, 246, 0.12)`)
- 🔲 **Borda**: Azul grossa 2px (`rgba(99, 102, 241, 0.4)`)
- 👤 **Ícone**: `bi-person-circle` (usuário) em verde (`#10b981`)
- 📊 **Progresso**: 100%
- 📊 **Classe CSS**: `.time-finished`

**Botões:**

- ✅ **Finalizar** - HABILITADO
- ✅ **Estender Tempo** - HABILITADO e VISÍVEL
- ❌ **Iniciar** - DESABILITADO
- ❌ **Pausar** - DESABILITADO
- ❌ **Cancelar** - DESABILITADO
- ✅ **Abrir Task** - HABILITADO

---

### 4. 🟣 IN_PROGRESS - Contando (Ativo)

**Condição**: `status === 'IN_PROGRESS' && mainStart !== null && elapsed < executionTime`

**Visual:**

- 🎨 **Fundo**: Normal (branco/padrão)
- ⏳ **Ícone**: `bi-hourglass-split` (ampulheta) em roxo (`#6366f1`)
- 📊 **Progresso**: Crescendo conforme tempo
- 💬 **Tooltip**: "Contando tempo"

**Botões:**

- ✅ **Pausar** - HABILITADO
- ❌ **Iniciar** - DESABILITADO
- ❌ **Cancelar** - DESABILITADO (enquanto contando)
- ❌ **Finalizar** - DESABILITADO (enquanto contando)
- ❌ **Estender** - OCULTO
- ✅ **Abrir Task** - HABILITADO

---

### 5. 🔴 IN_PROGRESS - Não Iniciado (Atrasado)

**Condição**: `status === 'IN_PROGRESS' && !mainStart && scheduledStart < now`

**Visual:**

- 🎨 **Fundo**: Vermelho claro (`rgba(255, 100, 100, 0.15)`)
- 🔲 **Borda**: Vermelha grossa 2px (`rgba(239, 68, 68, 0.5)`)
- ⚠️ **Ícone**: `bi-exclamation-triangle-fill` (alerta) em vermelho (`#ef4444`)
- 🏷️ **Badge**: "PENDENTE INICIAR" (vermelho, pulsante)
- 📊 **Classe CSS**: `.pending-start`

**Botões:**

- ✅ **Iniciar** - HABILITADO
- ✅ **Cancelar** - HABILITADO
- ❌ **Pausar** - DESABILITADO
- ❌ **Finalizar** - DESABILITADO
- ❌ **Estender** - OCULTO
- ✅ **Abrir Task** - HABILITADO

---

## 🔄 Fluxo de Estados

```
┌─────────────────────────────────────────────────────────┐
│ TODO (scheduledStart futuro)                            │
└────────────────────┬────────────────────────────────────┘
                     │ (tempo passa)
                     ↓
┌─────────────────────────────────────────────────────────┐
│ IN_PROGRESS - Não Iniciado 🔴                           │
│ Vermelho + Alerta + "PENDENTE INICIAR"                  │
└────────────────────┬────────────────────────────────────┘
                     │ (usuário clica "Iniciar" OU auto-start)
                     ↓
┌─────────────────────────────────────────────────────────┐
│ IN_PROGRESS - Contando 🟣                               │
│ Normal + Timer (ampulheta)                              │
└───────┬─────────────────────┬───────────────────────────┘
        │                     │
        │ (pause)             │ (tempo completa)
        ↓                     ↓
┌──────────────┐    ┌────────────────────────────────────┐
│ IN_PAUSE 🟡 │    │ Tempo Finalizado 🔵                 │
│ Amarelo      │    │ Azul + Usuário + Estender          │
└──────┬───────┘    └────────┬───────────────────────────┘
       │                     │
       │ (resume)            │ (extend ou finish)
       ↓                     ↓
┌──────────────────────────────────────────────────────────┐
│ IN_PROGRESS (retoma) ou DONE ou TODO (estendido)        │
└──────────────────────────────────────────────────────────┘
```

### Caso Especial: OVERDUE

```
┌─────────────────────────────────────────────────────────┐
│ Qualquer task que ultrapasse dueDate                    │
└────────────────────┬────────────────────────────────────┘
                     │ (sistema detecta)
                     ↓
┌─────────────────────────────────────────────────────────┐
│ OVERDUE 🔴                                              │
│ Vermelho + Alarme + "PENDENTE FINALIZAÇÃO"             │
│ Botões: Finalizar, Cancelar, Estender                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ├─→ (finish) → DONE
                     ├─→ (cancel) → CANCELLED
                     └─→ (extend) → TODO (com novo prazo)
```

---

## 🎯 Tabela Resumo de Botões

| Estado                  | Iniciar | Pausar | Cancelar | Finalizar | Estender | Abrir |
|-------------------------|---------|--------|----------|-----------|----------|-------|
| **OVERDUE** 🔴          | ❌       | ❌      | ✅        | ✅         | ✅ 👁️    | ✅     |
| **IN_PAUSE** 🟡         | ✅       | ❌      | ✅        | ✅         | ❌        | ✅     |
| **Tempo Finalizado** 🔵 | ❌       | ❌      | ❌        | ✅         | ✅ 👁️    | ✅     |
| **Contando** 🟣         | ❌       | ✅      | ❌        | ❌         | ❌        | ✅     |
| **Não Iniciado** 🔴     | ✅       | ❌      | ✅        | ❌         | ❌        | ✅     |

**Legenda:**

- ✅ = Habilitado
- ❌ = Desabilitado
- 👁️ = Visível

---

## 💾 Continuidade após Extensão

### Como Funciona

Quando uma tarefa é estendida:

1. ✅ **`extraTimeMinutes`** é somado a `executionTimeMinutes`
2. ✅ **`mainElapsedSeconds`** é MANTIDO (não resetado)
3. ✅ **`mainStartedAt`** é resetado para `null`
4. ✅ Status volta para `TODO`

### Exemplo Prático

```javascript
// Antes da extensão
executionTimeMinutes: 30
mainElapsedSeconds: 1800 (30 min já usados)
mainStartedAt: 2024-11-19T14:00:00

// Estendendo +15 minutos
extraTimeMinutes: 15

// Após extensão
executionTimeMinutes: 45 (30 + 15)
mainElapsedSeconds: 1800 (MANTIDO!)
mainStartedAt: null (resetado)
status: TODO

// Ao reiniciar
// Progresso já começa em 40% (1800s de 2700s)
// Timer mostra 00:30:00 (continua de onde parou)
```

---

## 🎨 Classes CSS Disponíveis

### Estados de Card

```css
.active-card.overdue-state     /* Fundo vermelho OVERDUE */
.active-card.pending-start     /* Fundo vermelho pendente */
.active-card.time-finished     /* Fundo azul finalizado */
.active-card.in-pause          /* Fundo amarelo pausado */
```

### Ícones de Status

```css
.status-indicator.clock        /* Alarme vermelho (OVERDUE) */
.status-indicator.warning      /* Alerta vermelho (pendente) */
.status-indicator.user         /* Usuário verde (finalizado) */
.status-indicator.timer        /* Ampulheta roxa (contando) */
.status-indicator.pause        /* Pausa amarela (IN_PAUSE) */
```

### Badges

```css
.badge-status.overdue-flag     /* "PENDENTE FINALIZAÇÃO" */
.badge-status.pending-flag     /* "PENDENTE INICIAR" */
```

---

## 🔧 Funções JavaScript Principais

### `updateCard(card)`

Função executada a cada 1 segundo para cada card ativo.

**Responsabilidades:**

1. Calcular tempo elapsed (base + runtime)
2. Atualizar timer display
3. Atualizar progress bar
4. Determinar estado atual (isOverdue, isPaused, timeFinished, etc)
5. Aplicar classes CSS apropriadas
6. Inserir ícones e badges
7. Habilitar/desabilitar botões conforme estado

### `autoStartTasks(tasks)`

Função que inicia automaticamente tasks TODO quando `scheduledStartAt` passa.

**Critérios:**

- Status = TODO
- `scheduledStartAt` <= now
- `executionTimeMinutes` configurado
- `pomodoroMinutes` configurado
- `mainStartedAt` null (ainda não iniciou)

---

## 📱 Comportamento Mobile

Todos os estados são responsivos e mantêm:

- ✅ Grid adaptativo (`minmax(320px, 1fr)`)
- ✅ Scroll horizontal quando necessário
- ✅ Ícones e badges visíveis
- ✅ Botões com tamanho touch-friendly (36x36px)

---

## 🚀 Performance

- **Refresh Rate**: 8 segundos (busca API)
- **Update Rate**: 1 segundo (atualização visual local)
- **Sticky Cards**: Cards com tempo finalizado permanecem visíveis
- **Caching**: Cards mantidos localmente entre refreshes

---

## ✅ Checklist de Implementação

### Estados Visuais

- [x] OVERDUE - vermelho + alarme + "Pendente finalização"
- [x] IN_PAUSE - amarelo + pause
- [x] Tempo Finalizado - azul + usuário + estender
- [x] Contando - normal + ampulheta
- [x] Não Iniciado - vermelho + alerta

### Botões

- [x] OVERDUE: Finalizar, Cancelar, Estender habilitados
- [x] Contando: Apenas Pausar habilitado
- [x] Finalizado: Apenas Finalizar e Estender habilitados
- [x] Não Iniciado: Apenas Iniciar e Cancelar habilitados
- [x] IN_PAUSE: Iniciar, Cancelar, Finalizar habilitados

### Funcionalidades

- [x] Início automático
- [x] Continuidade após extensão
- [x] Progress bar mantido
- [x] Timer mantido
- [x] Badges dinâmicos
- [x] Ícones por estado
- [x] Sticky cards

---

**Data de Implementação**: 19/11/2024
**Status**: ✅ COMPLETO
**Build**: ✅ SUCCESSFUL
**Testes**: Prontos para validação visual
