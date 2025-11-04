# 📋 Task Management System - JUnit 6 + Spring Boot 4.0 + Java 25

![Java](https://img.shields.io/badge/Java-25-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?style=for-the-badge&logo=spring)
![JUnit](https://img.shields.io/badge/JUnit-6.0.0-blue?style=for-the-badge&logo=junit5)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-enabled-blue?style=for-the-badge&logo=docker)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

Sistema completo de gerenciamento de tarefas desenvolvido para demonstrar **todas as funcionalidades** do **JUnit 6.0.0**, **Spring Boot 4.0** e **Java 25**.

> 📖 **Artigo Relacionado**: [JUnit 6.0.0: O que há de novo, por que migrar e como usar](https://www.rafaelvieiradev.com.br/blog/junit-600-o-que-ha-de-novo-por-que-migrar-e-como-usar)

---

## 📑 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Quick Start](#-quick-start)
- [Funcionalidades do Sistema](#funcionalidades-do-sistema)
- [Funcionalidades do JUnit 6](#funcionalidades-do-junit-6-demonstradas)
- [Arquitetura](#️-arquitetura-do-sistema)
- [Executando Testes](#-cobertura-de-testes)
- [Por Que Migrar](#-por-que-migrar-para-junit-6)
- [Boas Práticas](#️-boas-práticas-implementadas)
- [Troubleshooting](#-troubleshooting)
- [Recursos](#-recursos-adicionais)
- [Contribuindo](#-contribuindo)

---

## 🎯 Sobre o Projeto

Este projeto foi criado como material de estudo e demonstração prática das novidades do **JUnit 6.0.0**, a mais recente versão do framework de testes mais popular do ecossistema Java. O sistema implementa um CRUD completo de tarefas com todas as funcionalidades modernas de testes.

### 🎪 Destaques do Projeto

- 🧪 **JUnit 6.0.0** - Primeira implementação completa das features mais recentes
- 🚀 **Spring Boot 4.0** - Framework em versão snapshot com últimas features
- ☕ **Java 25** - Utilizando as features mais modernas da linguagem
- 🐘 **PostgreSQL 16** - Banco de dados robusto e confiável
- 🐳 **Testcontainers** - Testes de integração com containers reais
- 📊 **Jacoco** - Cobertura de testes configurada e automatizada
- 📝 **OpenAPI 3** - Documentação interativa da API

### 🆚 Comparação com Abordagens Tradicionais

| Aspecto | Abordagem Tradicional | Este Projeto |
|---------|---------------------|--------------|
| **Framework de Testes** | JUnit 4 ou JUnit 5 | JUnit 6.0.0 |
| **Banco de Testes** | H2 em memória | PostgreSQL real (Testcontainers) |
| **Testes Parametrizados** | Básicos ou manuais | Avançados (`@EnumSource`, `@CsvSource`) |
| **Assertions** | JUnit puro | AssertJ + JUnit 6 |
| **Organização** | Arquivos planos | `@Nested` classes |
| **Spring Boot** | 2.x ou 3.x | 4.0.0-SNAPSHOT |
| **Java** | 8, 11, 17 | 25 (Early Access) |
| **Cobertura** | Manual | Jacoco automatizado |
| **Documentação API** | Manual | Swagger/OpenAPI automático |

## Funcionalidades do Sistema

- **Java 25** (Early Access) - Últimas features da linguagem
- **Spring Boot 4.0.0-SNAPSHOT** - Framework web moderno
- **JUnit 6.0.0** - Framework de testes de última geração
- **Gradle 8.x** - Build Tool com suporte a Java 25
- **PostgreSQL 16** - Banco de dados relacional
- **Testcontainers** - Containers Docker para testes
- **Lombok** - Redução de boilerplate
- **AssertJ 3.26.3** - Assertions fluentes e poderosas
- **Mockito 5.14.2** - Mock framework
- **SpringDoc OpenAPI 2.6.0** - Documentação automática da API

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

## ⚡ Quick Start

```bash
# 1. Clone o repositório
git clone https://github.com/rafaelrok/task-management.git
cd task-management

# 2. Configurar PostgreSQL (ou use Docker)
docker run -d \
  --name postgres-taskmanagement \
  -e POSTGRES_DB=taskdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  postgres:16-alpine

# 3. Executar a aplicação
./gradlew bootRun

# 4. Acessar Swagger UI
# http://localhost:8080/swagger-ui.html

# 5. Executar testes
./gradlew test
```

## 🔧 Configuração do Ambiente

### Pré-requisitos

| Ferramenta | Versão | Obrigatório | Uso |
|-----------|--------|-------------|-----|
| **Java** | 25 EA | ✅ Sim | Runtime e compilação |
| **Docker** | 20+ | ✅ Sim | Testcontainers e PostgreSQL |
| **PostgreSQL** | 16+ | ⚠️ Opcional* | Desenvolvimento local |
| **Gradle** | 8.x | ⚠️ Opcional** | Build (wrapper incluído) |
| **Git** | Qualquer | ✅ Sim | Controle de versão |

\* PostgreSQL pode ser executado via Docker  
\*\* Gradle wrapper (`gradlew`) está incluído no projeto

### Instalação do Java 25

#### Windows
```bash
# Download do instalador
# https://jdk.java.net/25/

# Configurar JAVA_HOME
setx JAVA_HOME "C:\Program Files\Java\jdk-25"
setx PATH "%JAVA_HOME%\bin;%PATH%"

# Verificar
java --version
```

#### Linux/Mac
```bash
# Download e extração
wget https://download.java.net/java/early_access/jdk25/XX/GPL/openjdk-25-ea+XX_linux-x64_bin.tar.gz
tar -xzf openjdk-25-ea*.tar.gz
sudo mv jdk-25 /opt/

# Configurar JAVA_HOME (~/.bashrc ou ~/.zshrc)
export JAVA_HOME=/opt/jdk-25
export PATH=$JAVA_HOME/bin:$PATH

# Aplicar e verificar
source ~/.bashrc
java --version
```

### Instalação do Docker

#### Windows
```bash
# Instalar Docker Desktop
# https://www.docker.com/products/docker-desktop
```

#### Linux (Ubuntu/Debian)
```bash
# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Adicionar usuário ao grupo docker
sudo usermod -aG docker $USER
newgrp docker

# Verificar
docker --version
docker ps
```

#### Mac
```bash
# Instalar Docker Desktop
brew install --cask docker
```

## 🏗️ Construindo o Projeto

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

### 4. Principais Dependências

O projeto utiliza as seguintes dependências chave no `build.gradle`:

#### JUnit 6 e Testes
```groovy
ext {
    junitVersion = '6.0.0'
    mockitoVersion = '5.14.2'
    assertjVersion = '3.26.3'
    testcontainersVersion = '1.20.4'
}

dependencies {
    // JUnit 6 - Framework de testes
    testImplementation "org.junit.jupiter:junit-jupiter:${junitVersion}"
    testImplementation "org.junit.jupiter:junit-jupiter-api:${junitVersion}"
    testImplementation "org.junit.jupiter:junit-jupiter-params:${junitVersion}"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:${junitVersion}"
    
    // AssertJ - Assertions fluentes
    testImplementation "org.assertj:assertj-core:${assertjVersion}"
    
    // Mockito - Mocking framework
    testImplementation "org.mockito:mockito-core:${mockitoVersion}"
    testImplementation "org.mockito:mockito-junit-jupiter:${mockitoVersion}"
    
    // Testcontainers - Containers para testes
    testImplementation "org.testcontainers:testcontainers:${testcontainersVersion}"
    testImplementation "org.testcontainers:postgresql:${testcontainersVersion}"
    testImplementation "org.testcontainers:junit-jupiter:${testcontainersVersion}"
}
```

#### Spring Boot e Produção
```groovy
dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    
    // Database
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'com.zaxxer:HikariCP:5.1.0'
    
    // Documentation
    implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0"
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

#### Configuração do Test Task
```groovy
test {
    useJUnitPlatform()  // Habilita JUnit 6
    
    // Java 25 flags
    jvmArgs = [
        '--enable-preview',
        '--add-opens', 'java.base/java.lang=ALL-UNNAMED',
        '--add-opens', 'java.base/java.util=ALL-UNNAMED'
    ]
    
    // Performance
    maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
    maxHeapSize = '2G'
    
    // Logging
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}
```

#### Jacoco para Cobertura
```groovy
jacoco {
    toolVersion = "0.8.13"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
    
    classDirectories.setFrom(
        files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/config/**',
                '**/dto/**',
                '**/exception/**'
            ])
        })
    )
}
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

### 📖 Documentação Interativa da API (Swagger UI)

Após iniciar a aplicação, acesse:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

O Swagger UI permite:
- ✅ Visualizar todos os endpoints
- ✅ Testar requisições diretamente pelo navegador
- ✅ Ver schemas de request/response
- ✅ Validar payloads JSON

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

Este projeto implementa **todas** as principais funcionalidades do JUnit 6.0.0, alinhado com o artigo publicado.

### 1. Testes Parametrizados Avançados

#### `@EnumSource` - Testa todos os valores de Enum
```java
@ParameterizedTest
@EnumSource(TaskStatus.class)
void shouldFindTasksByAllStatuses(TaskStatus status) {
    // Testa automaticamente: TODO, IN_PROGRESS, DONE, CANCELLED
}
```

#### `@ValueSource` - Múltiplos valores do mesmo tipo
```java
@ParameterizedTest
@ValueSource(longs = {1L, 2L, 3L, 100L})
void shouldGetTaskByDifferentIds(Long id) {
    // Testa múltiplos IDs em um único método
}
```

#### `@CsvSource` - Combinações de valores
```java
@ParameterizedTest
@CsvSource({
    "TODO, false",
    "IN_PROGRESS, false",
    "DONE, true",
    "CANCELLED, false"
})
void shouldIdentifyCompletedStatus(TaskStatus status, boolean expected) {
    // Testa pares de entrada/saída esperada
}
```

### 2. Testes Nested - Organização Hierárquica

```java
@Nested
@DisplayName("Task with User Assignment Tests")
class TaskWithUserTests {
    
    @Test
    void shouldAssignUserToTask() { }
    
    @Test
    void shouldFindTasksByUser() { }
}
```

### 3. Assertions Agrupadas com `assertAll`

```java
assertAll("Task Creation Assertions",
    () -> assertNotNull(result),
    () -> assertEquals("Test", result.getTitle()),
    () -> assertEquals(Priority.HIGH, result.getPriority()),
    () -> assertEquals(TaskStatus.TODO, result.status())
);
```

**Vantagem**: Executa todas as assertions mesmo se uma falhar, mostrando todos os erros de uma vez.

### 4. AssertJ - Fluent Assertions

```java
assertThat(result)
    .isNotNull()
    .satisfies(dto -> {
        assertThat(dto.getTitle()).isEqualTo("Test");
        assertThat(dto.getStatus()).isEqualTo(TaskStatus.TODO);
    })
    .extracting(TaskRecord::priority)
    .isEqualTo(Priority.HIGH);
```

### 5. Test Ordering e Lifecycle

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskServiceTest {
    
    @Test
    @Order(1)
    @DisplayName("Should create task successfully")
    void test1() { }
    
    @Test
    @Order(2)
    @DisplayName("Should update task")
    void test2() { }
}
```

### 6. DisplayName Personalizado

```java
@Test
@DisplayName("POST /api/tasks - Should create task successfully")
void testCreateTask() { }
```

### 7. Exceções Esperadas com AssertJ

```java
assertThatThrownBy(() -> taskService.createTask(invalidData))
    .isInstanceOf(ResourceNotFoundException.class)
    .hasMessageContaining("Category not found with id: 999");
```

### 8. Integração com Testcontainers

```java
@TestConfiguration
public class TestContainersConfig {
    
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);
    }
}
```

### 9. Testes de Integração End-to-End

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskControllerIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldCreateTaskViaRestAPI() throws Exception {
        mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(taskDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Test Task"));
    }
}
```

### 10. BeforeEach e AfterEach Modernos

```java
@BeforeEach
void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    // Setup comum para todos os testes
}

@AfterEach
void tearDown() throws Exception {
    closeable.close();
    // Limpeza após cada teste
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

## 📊 Cobertura de Testes

### Estatísticas de Testes

- **Testes Unitários**: 15+ testes
  - `TaskServiceTest.java`: 10+ cenários diferentes
  - `TaskTest.java`: 7+ testes de modelo
  
- **Testes de Integração**: 8+ testes
  - `TaskControllerIntegrationTest.java`: 5+ testes REST
  - `TaskRepositoryIntegrationTest.java`: 3+ testes de persistência

- **Cobertura**: Configurada com Jacoco
  - Classes excluídas: config, dto, exceptions
  - Relatório gerado em: `build/reports/jacoco/`

### Executar Testes

```bash
# Todos os testes
./gradlew test

# Com relatório de cobertura
./gradlew test jacocoTestReport

# Ver relatório HTML (Linux/Mac)
open build/reports/jacoco/index.html

# Ver relatório HTML (Windows)
start build\reports\jacoco\index.html

# Apenas testes unitários
./gradlew test --tests "*unit*"

# Apenas testes de integração
./gradlew test --tests "*integration*"

# Teste específico
./gradlew test --tests TaskServiceTest

# Com logs detalhados
./gradlew test --info
```

### 🧪 Tipos de Testes Implementados

| Tipo | Ferramenta | Quantidade | Descrição |
|------|-----------|------------|-----------|
| **Unitários** | JUnit 6 + Mockito | 15+ | Testa lógica isolada com mocks |
| **Integração** | Testcontainers | 8+ | Testa componentes integrados |
| **Parametrizados** | `@ParameterizedTest` | 5+ | Testa múltiplos cenários |
| **API REST** | MockMvc + REST Assured | 5+ | Testa endpoints HTTP |
| **Repository** | Spring Data + PostgreSQL | 3+ | Testa queries e persistência |

### 📈 Relatório de Testes

O relatório HTML gerado mostra:
- ✅ Testes passados/falhados/ignorados
- ⏱️ Tempo de execução
- 📊 Cobertura de código por classe/método/linha
- 🎯 Branches cobertos vs não cobertos

## 🏗️ Arquitetura do Sistema

### Estrutura em Camadas

```
src/
├── main/java/br/com/rafaelvieira/task_management/
│   ├── controller/          # REST Controllers
│   │   └── TaskController.java
│   ├── service/             # Lógica de Negócio
│   │   ├── TaskService.java (Interface)
│   │   └── impl/TaskServiceImpl.java
│   ├── repository/          # Acesso a Dados (Spring Data JPA)
│   │   ├── TaskRepository.java
│   │   ├── CategoryRepository.java
│   │   └── UserRepository.java
│   ├── domain/
│   │   ├── model/           # Entidades JPA
│   │   │   ├── Task.java
│   │   │   ├── Category.java
│   │   │   └── User.java
│   │   ├── dto/             # Data Transfer Objects (Java Records)
│   │   │   ├── TaskRecord.java
│   │   │   └── TaskCreateRecord.java
│   │   └── enums/           # Enumerações
│   │       ├── TaskStatus.java (TODO, IN_PROGRESS, DONE, CANCELLED)
│   │       └── Priority.java (LOW, MEDIUM, HIGH, URGENT)
│   ├── exception/           # Tratamento de Exceções
│   │   ├── ResourceNotFoundException.java
│   │   └── GlobalExceptionHandler.java
│   └── config/              # Configurações Spring
│       ├── SwaggerConfig.java        # Documentação OpenAPI
│       ├── SecurityConfig.java       # Segurança básica
│       ├── DatabaseConfig.java       # Pool de conexões
│       ├── CacheConfig.java          # Cache de dados
│       ├── AsyncConfig.java          # Processamento assíncrono
│       ├── ValidationConfig.java     # Bean Validation
│       ├── LoggingConfig.java        # Logs estruturados
│       ├── WebConfig.java            # CORS e interceptors
│       └── ApplicationProperties.java
│
└── test/java/br/com/rafaelvieira/task_management/
    ├── unit/                # Testes Unitários (JUnit 6 + Mockito)
    │   ├── service/
    │   │   └── TaskServiceTest.java   # 100+ assertions
    │   └── model/
    │       └── TaskTest.java          # Testes de lógica de domínio
    ├── integration/         # Testes de Integração (Testcontainers)
    │   ├── controller/
    │   │   └── TaskControllerIntegrationTest.java  # REST API tests
    │   ├── repository/
    │   │   └── TaskRepositoryIntegrationTest.java  # Database tests
    │   └── BaseIntegrationTest.java
    └── config/
        └── TestContainersConfig.java  # PostgreSQL container config
```

### 🗄️ Modelo de Dados

#### Task (Entidade Principal)
```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @NotBlank
    @Size(min = 3, max = 100)
    String title;                    // Título da tarefa
    
    @Size(max = 1000)
    String description;              // Descrição detalhada
    
    @Enumerated(EnumType.STRING)
    TaskStatus status;               // TODO, IN_PROGRESS, DONE, CANCELLED
    
    @Enumerated(EnumType.STRING)
    Priority priority;               // LOW, MEDIUM, HIGH, URGENT
    
    LocalDateTime dueDate;           // Data de vencimento
    
    @CreatedDate
    LocalDateTime createdAt;         // Data de criação (auditoria)
    
    @LastModifiedDate
    LocalDateTime updatedAt;         // Última atualização (auditoria)
    
    @ManyToOne(fetch = FetchType.LAZY)
    Category category;               // Categoria (relacionamento)
    
    @ManyToOne(fetch = FetchType.LAZY)
    User assignedUser;               // Usuário responsável
    
    // Métodos de negócio
    public boolean isOverdue() { ... }
    public boolean isCompleted() { ... }
}
```

### 🎨 Padrões de Design Implementados

1. **Repository Pattern** - Abstração de acesso a dados
2. **Service Layer** - Lógica de negócio isolada
3. **DTO Pattern** - Transferência de dados com Records
4. **Builder Pattern** - Construção fluente de objetos (Lombok)
5. **Strategy Pattern** - Diferentes tipos de testes
6. **Dependency Injection** - Spring IoC
7. **Global Exception Handler** - Tratamento centralizado de erros

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

## 🎯 Por Que Migrar para JUnit 6?

Baseado no [artigo oficial do blog](https://www.rafaelvieiradev.com.br/blog/junit-600-o-que-ha-de-novo-por-que-migrar-e-como-usar), aqui estão as principais razões:

### Vantagens do JUnit 6.0.0

1. **Sintaxe Mais Limpa e Moderna**
   - Melhor legibilidade com `@DisplayName`
   - Testes parametrizados mais poderosos
   - Assertions mais expressivas

2. **Melhor Integração com Frameworks Modernos**
   - Spring Boot 4.0
   - Jakarta EE 11
   - Java 21+ (Records, Pattern Matching, Virtual Threads)

3. **Performance Aprimorada**
   - Execução paralela de testes otimizada
   - Menor overhead de inicialização
   - Melhor gerenciamento de memória

4. **Recursos Avançados**
   - Testes dinâmicos (`@TestFactory`)
   - Testes condicionais (`@EnabledIf`, `@DisabledIf`)
   - Timeout configurável (`@Timeout`)
   - Testes repetidos (`@RepeatedTest`)

5. **Compatibilidade com Testcontainers**
   - Integração nativa com containers
   - Suporte a `@ServiceConnection`
   - Melhor ciclo de vida de containers

### Diferenças do JUnit 5

| Feature | JUnit 5 | JUnit 6 |
|---------|---------|---------|
| **Java Mínimo** | Java 8+ | Java 17+ |
| **Testes Parametrizados** | Básico | Avançado com `@EnumSource` |
| **Spring Boot** | 2.x/3.x | 4.0+ otimizado |
| **Performance** | Bom | Excelente |
| **Assertions** | Padrão | Melhoradas |
| **Testcontainers** | Manual | Integrado |

## 🛠️ Boas Práticas Implementadas

### 1. Nomenclatura de Testes Clara
```java
@Test
@DisplayName("Should create task successfully when all data is valid")
void shouldCreateTaskSuccessfully() { }
```

### 2. Testes Isolados e Independentes
- Cada teste limpa seu próprio estado
- Uso de `@BeforeEach` para setup
- Uso de `@AfterEach` para teardown

### 3. AAA Pattern (Arrange-Act-Assert)
```java
@Test
void shouldUpdateTask() {
    // Arrange
    Task task = createTestTask();
    
    // Act
    TaskRecord result = taskService.updateTask(1L, updateData);
    
    // Assert
    assertThat(result).isNotNull();
}
```

### 4. Testes Parametrizados para Cobertura Completa
```java
@ParameterizedTest
@EnumSource(TaskStatus.class)
void testAllStatuses(TaskStatus status) {
    // Garante que todos os valores do enum são testados
}
```

### 5. Uso de Assertions Agrupadas
```java
assertAll(
    () -> assertEquals(expected.getTitle(), actual.getTitle()),
    () -> assertEquals(expected.getStatus(), actual.getStatus()),
    () -> assertNotNull(actual.getCreatedAt())
);
```

### 6. Testcontainers para Testes Reais
- Banco de dados real (não H2 em memória)
- Comportamento idêntico ao produção
- Isolamento completo entre testes

### 7. Mocks Apenas Quando Necessário
- Testes unitários: Mock de dependências externas
- Testes de integração: Componentes reais

### 8. Cobertura de Código Configurada
- Jacoco configurado no Gradle
- Exclusão de classes de configuração e DTOs
- Relatórios automáticos após testes

## 🐛 Troubleshooting

### Problema: Testcontainers não inicia

**Solução**:
```bash
# Verificar se Docker está rodando
docker ps

# No Windows, usar Docker Desktop
# No Linux, iniciar Docker daemon
sudo systemctl start docker
```

### Problema: Testes falhando com Java 25

**Solução**: Adicionar flags JVM no `build.gradle`:
```groovy
test {
    jvmArgs = [
        '--enable-preview',
        '--add-opens', 'java.base/java.lang=ALL-UNNAMED'
    ]
}
```

### Problema: Erro de conexão com PostgreSQL

**Solução**:
1. Verificar se PostgreSQL está rodando na porta 5433
2. Conferir credenciais em `application.yaml`
3. Para testes, Testcontainers gerencia automaticamente

### Problema: Out of Memory durante testes

**Solução**:
```groovy
test {
    maxHeapSize = '2G'
    minHeapSize = '512M'
}
```

### Problema: Testes muito lentos

**Solução**:
```groovy
test {
    // Execução paralela
    maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
    
    // Reutilizar containers
    // Em TestContainersConfig: .withReuse(true)
}
```

## 📚 Recursos Adicionais

### Documentação Oficial
- [JUnit 6 Documentation](https://junit.org/junit6/)
- [Spring Boot 4.0 Reference](https://docs.spring.io/spring-boot/docs/4.0.x/reference/)
- [Testcontainers Docs](https://www.testcontainers.org/)
- [AssertJ Guide](https://assertj.github.io/doc/)

### Artigos Relacionados
- 📖 [JUnit 6.0.0: O que há de novo](https://www.rafaelvieiradev.com.br/blog/junit-600-o-que-ha-de-novo-por-que-migrar-e-como-usar)
- 🎯 [Melhores Práticas de Testes em Java](https://www.rafaelvieiradev.com.br/blog)
- 🚀 [Spring Boot 4.0 Novidades](https://www.rafaelvieiradev.com.br/blog)

### Ferramentas Úteis
- **IntelliJ IDEA** - Melhor suporte para JUnit 6
- **Gradle** - Build tool recomendada
- **Docker Desktop** - Para Testcontainers
- **Postman/Insomnia** - Testar APIs REST
- **DBeaver** - Cliente PostgreSQL

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor:

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/NovaFuncionalidade`)
3. Commit suas mudanças (`git commit -m 'Add: nova funcionalidade'`)
4. Push para a branch (`git push origin feature/NovaFuncionalidade`)
5. Abra um Pull Request

### Padrão de Commits
- `Add:` Nova funcionalidade
- `Fix:` Correção de bug
- `Docs:` Atualização de documentação
- `Test:` Adição ou modificação de testes
- `Refactor:` Refatoração de código

## 📸 Screenshots e Demo

### Swagger UI - Documentação Interativa
Após iniciar a aplicação, acesse: `http://localhost:8080/swagger-ui.html`

A interface Swagger permite:
- 🔍 Explorar todos os endpoints disponíveis
- ▶️ Executar requisições diretamente no navegador
- 📄 Ver schemas JSON completos
- ✅ Validar payloads antes de enviar
- 🎯 Testar autenticação e autorizações

### Relatório de Testes JUnit
Após executar `./gradlew test`, acesse: `build/reports/tests/test/index.html`

O relatório HTML mostra:
- ✅ Taxa de sucesso dos testes (100% neste projeto)
- ⏱️ Tempo de execução por classe e método
- 📊 Estatísticas detalhadas por pacote
- 🎯 Testes que falharam com stack traces completas
- 📈 Histórico de execuções

### Relatório de Cobertura Jacoco
Após executar `./gradlew jacocoTestReport`, acesse: `build/reports/jacoco/index.html`

Visualize métricas de cobertura:
- 📈 Porcentagem de cobertura por pacote/classe
- 🎯 Linhas cobertas vs não cobertas (highlighting visual)
- 🔀 Branches testados (if/else, switch)
- 📊 Complexidade ciclomática
- 🚀 Metas de cobertura configuráveis

## 👨‍💻 Autor

**Rafael Vieira** (rafaelrok)

Desenvolvedor Full Stack especializado em Java, Spring Framework e arquitetura de microserviços.

- 🌐 Website: [rafaelvieiradev.com.br](https://www.rafaelvieiradev.com.br)
- 📝 Blog: [rafaelvieiradev.com.br/blog](https://www.rafaelvieiradev.com.br/blog)
- 💼 LinkedIn: [linkedin.com/in/rafaelrok](https://www.linkedin.com/in/rafaelrok)
- 🐙 GitHub: [@rafaelrok](https://github.com/rafaelrok)
- 📧 Email: contato@rafaelvieiradev.com.br

### Sobre este Projeto
- 📅 Criado em: **04 de Novembro de 2025**
- 🎯 Objetivo: Demonstração completa e didática de **JUnit 6.0.0**, **Spring Boot 4.0** e **Java 25**
- 📖 Artigo Relacionado: [JUnit 6.0.0: O que há de novo, por que migrar e como usar](https://www.rafaelvieiradev.com.br/blog/junit-600-o-que-ha-de-novo-por-que-migrar-e-como-usar)
- 🏷️ Tags: `junit6`, `spring-boot-4`, `java-25`, `testing`, `tdd`, `bdd`, `testcontainers`, `mockito`, `assertj`
- 📚 Finalidade: Material educacional e referência para a comunidade

## 📜 Licença

Este projeto está licenciado sob a **MIT License** - sinta-se livre para usar, modificar e distribuir.

```text
MIT License

Copyright (c) 2025 Rafael Vieira

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 📚 Referências e Documentação Oficial

### Frameworks e Bibliotecas
- 🧪 [JUnit 6 Documentation](https://junit.org/junit6/) - Framework de testes
- 🍃 [Spring Boot 4.0 Documentation](https://docs.spring.io/spring-boot/docs/4.0.x/reference/) - Framework web
- ☕ [Java 25 Documentation](https://openjdk.org/projects/jdk/25/) - Linguagem Java
- 🐳 [Testcontainers Documentation](https://www.testcontainers.org/) - Containers para testes
- ✅ [AssertJ Documentation](https://assertj.github.io/doc/) - Assertions fluentes
- 🎭 [Mockito Documentation](https://site.mockito.org/) - Mock framework
- 📖 [SpringDoc OpenAPI](https://springdoc.org/) - Documentação de APIs

### Tutoriais e Guias
- 📝 [Artigo: JUnit 6.0.0 - O que há de novo](https://www.rafaelvieiradev.com.br/blog/junit-600-o-que-ha-de-novo-por-que-migrar-e-como-usar)
- 🎓 [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki)
- 🚀 [Java 25 New Features](https://openjdk.org/projects/jdk/25/)
- 🐘 [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/)

### Ferramentas Recomendadas
- 💻 **IDE**: IntelliJ IDEA Ultimate (melhor suporte para Spring Boot)
- 🔨 **Build**: Gradle 8.x com Kotlin DSL
- 🐳 **Containers**: Docker Desktop
- 📮 **API Testing**: Postman, Insomnia ou Thunder Client
- 🗄️ **Database Client**: DBeaver, pgAdmin ou DataGrip

---

## 🌟 Agradecimentos

Obrigado por conferir este projeto! Se foi útil para você:

- ⭐ Deixe uma estrela no repositório
- 🐛 Reporte bugs ou sugira melhorias via Issues
- 🔀 Envie Pull Requests com novos recursos
- 📢 Compartilhe com outros desenvolvedores
- 💬 Entre em contato para dúvidas ou feedback

---

<div align="center">

**Feito com ❤️ e ☕ por [Rafael Vieira](https://www.rafaelvieiradev.com.br)**

⭐ Se este projeto foi útil, deixe uma estrela! ⭐

</div>