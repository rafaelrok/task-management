# Task Management System - JUnit 6 + Spring Boot 4.0 + Java 25

Sistema completo de gerenciamento de tarefas desenvolvido para demonstrar todas as funcionalidades do **JUnit 6**, **Spring Boot 4.0** e **Java 25**.

## Tecnologias Utilizadas

- **Java 25** (Early Access)
- **Spring Boot 4.0.0-SNAPSHOT**
- **JUnit 6.0.0** (Latest)
- **Gradle** (Build Tool)
- **PostgreSQL** (Database)
- **Testcontainers** (Integration Tests)
- **Lombok** (Boilerplate Reduction)
- **AssertJ** (Fluent Assertions)

## Funcionalidades

### API REST Completa

- ✅ CRUD completo de tarefas
- ✅ Categorização de tarefas
- ✅ Atribuição de usuários
- ✅ Sistema de prioridades (LOW, MEDIUM, HIGH, URGENT)
- ✅ Status de tarefas (TODO, IN_PROGRESS, DONE, CANCELLED)
- ✅ Detecção de tarefas atrasadas
- ✅ Filtros avançados (status, prioridade, categoria, usuário)
- ✅ Contagem de tarefas por status

### Testes Completos

#### Testes Unitários (JUnit 6)
- ✅ Testes de Service Layer com Mockito
- ✅ Testes Parametrizados com `@ParameterizedTest`
- ✅ Testes com `@EnumSource` para todos os enums
- ✅ Testes com `@ValueSource` para múltiplos valores
- ✅ Testes Nested com `@Nested`
- ✅ Assertions agrupadas com `assertAll`
- ✅ AssertJ para assertions fluentes

#### Testes de Integração
- ✅ Testes de Repository com PostgreSQL real (Testcontainers)
- ✅ Testes de Controller (end-to-end)
- ✅ Validação de toda a stack
- ✅ Testes transacionais

## Configuração do Ambiente

### Pré-requisitos

1. **Java 25** instalado
2. **Docker** (para Testcontainers)
3. **PostgreSQL** (para desenvolvimento local)
4. **Gradle** (wrapper incluído)

### Instalação do Java 25

```bash
# Download do Java 25 Early Access
# https://jdk.java.net/25/

# Configure JAVA_HOME
export JAVA_HOME=/path/to/jdk-25
export PATH=$JAVA_HOME/bin:$PATH

# Verifique a instalação
java --version
```

## Construindo o Projeto

### 1. Clone ou crie o projeto via Spring Initializr

Acesse: https://start.spring.io/

Configurações:
- Project: **Gradle - Groovy**
- Language: **Java**
- Spring Boot: **4.0.0 (SNAPSHOT)**
- Java: **25**
- Dependencies: Spring Web, Spring Data JPA, PostgreSQL Driver, Validation, Lombok

### 2. Configure o PostgreSQL

```bash
# Via Docker
docker run --name postgres-taskdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=taskdb \
  -p 5432:5432 \
  -d postgres:16-alpine

# Ou instale localmente e crie o banco
createdb taskdb
```

### 3. Build do Projeto

```bash
# Linux/Mac
./gradlew clean build

# Windows
gradlew.bat clean build
```

## Executando os Testes

### Todos os Testes

```bash
./gradlew test
```

### Apenas Testes Unitários

```bash
./gradlew test --tests "*.unit.*"
```

### Apenas Testes de Integração

```bash
./gradlew test --tests "*.integration.*"
```

### Teste Específico

```bash
./gradlew test --tests TaskServiceTest
```

### Com Relatório Detalhado

```bash
./gradlew test --info
```

## Executando a Aplicação

```bash
# Via Gradle
./gradlew bootRun

# Via Java (após build)
java -jar build/libs/task-management-0.0.1-SNAPSHOT.jar
```

A aplicação estará disponível em: `http://localhost:8080`

## Endpoints da API

### Tasks

```http
# Criar tarefa
POST /api/tasks
Content-Type: application/json

{
  "title": "Implementar feature",
  "description": "Descrição detalhada",
  "status": "TODO",
  "priority": "HIGH",
  "categoryId": 1,
  "assignedUserId": 1,
  "dueDate": "2025-12-31T23:59:59"
}

# Listar todas as tarefas
GET /api/tasks

# Buscar tarefa por ID
GET /api/tasks/{id}

# Atualizar tarefa
PUT /api/tasks/{id}

# Mudar status da tarefa
PATCH /api/tasks/{id}/status?status=IN_PROGRESS

# Deletar tarefa
DELETE /api/tasks/{id}

# Listar por status
GET /api/tasks/status/{status}

# Listar por prioridade
GET /api/tasks/priority/{priority}

# Listar por categoria
GET /api/tasks/category/{categoryId}

# Listar por usuário
GET /api/tasks/user/{userId}

# Listar tarefas atrasadas
GET /api/tasks/overdue

# Contar tarefas por status
GET /api/tasks/count/{status}
```

### Exemplos com cURL

```bash
# Criar tarefa
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Estudar JUnit 6",
    "description": "Aprender todas as novas features",
    "status": "TODO",
    "priority": "HIGH"
  }'

# Listar todas
curl http://localhost:8080/api/tasks

# Buscar por status
curl http://localhost:8080/api/tasks/status/TODO

# Tarefas atrasadas
curl http://localhost:8080/api/tasks/overdue
```

## Funcionalidades do JUnit 6 Demonstradas

### 1. Testes Parametrizados Avançados

```java
@ParameterizedTest
@EnumSource(TaskStatus.class)
void shouldFindTasksByAllStatuses(TaskStatus status) {
    // Testa todos os valores do enum automaticamente
}

@ParameterizedTest
@ValueSource(longs = {1L, 2L, 3L, 100L})
void shouldGetTaskByDifferentIds(Long id) {
    // Testa múltiplos valores
}

@ParameterizedTest
@CsvSource({
    "TODO, false",
    "DONE, true"
})
void shouldIdentifyCompletedStatus(TaskStatus status, boolean expected) {
    // Testa combinações de valores
}
```

### 2. Testes Nested

```java
@Nested
@DisplayName("Task with User Assignment Tests")
class TaskWithUserTests {
    // Agrupa testes relacionados
}
```

### 3. Assertions Agrupadas

```java
assertAll("Task Creation Assertions",
    () -> assertNotNull(result),
    () -> assertEquals("Test", result.getTitle()),
    () -> assertEquals(Priority.HIGH, result.getPriority())
);
```

### 4. AssertJ Fluent Assertions

```java
assertThat(result)
    .isNotNull()
    .satisfies(dto -> {
        assertThat(dto.getTitle()).isEqualTo("Test");
        assertThat(dto.getStatus()).isEqualTo(TaskStatus.TODO);
    });
```

### 5. Test Ordering

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MyTests {
    @Test
    @Order(1)
    void firstTest() {}
}
```

## Testcontainers

Os testes de integração utilizam Testcontainers para criar um PostgreSQL real:

```java
@Bean
@ServiceConnection
public PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);
}
```

## Estrutura de Pacotes

```
com.example.taskmanagement/
├── config/           # Configurações
├── controller/       # REST Controllers
├── dto/             # Data Transfer Objects
├── exception/       # Exception Handlers
├── model/           # JPA Entities
├── repository/      # Spring Data Repositories
└── service/         # Business Logic
```

## Checklist de Testes

- [x] Testes Unitários de Service
- [x] Testes Unitários de Model
- [x] Testes de Integração de Repository
- [x] Testes de Integração de Controller
- [x] Testes Parametrizados
- [x] Testes com Mockito
- [x] Testes com Testcontainers
- [x] Validação de Inputs
- [x] Exception Handling
- [x] Transações de Banco de Dados

## Cobertura de Testes

Execute para gerar relatório:

```bash
./gradlew test jacocoTestReport
```

Relatório disponível em: `build/reports/jacoco/test/html/index.html`

## Features do Java 25 Utilizadas

- Virtual Threads (Project Loom)
- Pattern Matching
- Record Patterns
- Structured Concurrency

## Notas Importantes

1. **Java 25 está em Early Access** - Use para aprendizado e testes
2. **Spring Boot 4.0 está em SNAPSHOT** - Ainda não é production-ready
3. **JUnit 6** traz melhorias significativas em assertions e parametrização
4. **Testcontainers** requer Docker rodando
5. Os testes são **transacionais** e fazem **rollback** automático

## Contribuindo

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -am 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## Licença

Este projeto é livre para uso educacional e demonstrativo.

## Autor

**Rafael Vieira (@rafaelrok)**
- Projeto criado em: 01/11/2025
- Objetivo: Demonstração completa de JUnit 6, Spring Boot 4.0 e Java 25

## Referências

- [JUnit 6 Documentation](https://docs.junit.org/6.0.0/release-notes/)
- [Spring Boot 4.0 Documentation](https://docs.spring.io/spring-boot/4.0-SNAPSHOT/index.html)
- [Java 25 Documentation](https://openjdk.org/projects/jdk/25/)
- [Testcontainers Documentation](https://www.testcontainers.org/)

---

⭐ Se este projeto foi útil, deixe uma estrela!