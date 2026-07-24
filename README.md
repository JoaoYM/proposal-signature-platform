# Proposal Signature Platform

Uma plataforma SaaS de **gestão de propostas e assinatura eletrônica de contratos**, construída sobre uma arquitetura de microsserviços com comunicação assíncrona orientada a eventos.

---

## Visão Geral

A plataforma é composta por dois microsserviços independentes que se comunicam via mensageria:

| Microsserviço | Responsabilidade | Porta |
|---|---|---|
| **CRM API** (`crm-api`) | Gestão de propostas comerciais. Criação, consulta e acompanhamento do ciclo de vida de propostas. | `8081` |
| **SIGN API** (`sign-api`) | Gestão de contratos e assinatura eletrônica. Geração de contratos, fluxo sequencial de assinaturas e auditoria. | `8082` |

### Fluxo de Negócio

1. Uma **Proposta** é criada no CRM com itens e dados do cliente.
2. O CRM publica um evento `proposta.criada` no RabbitMQ (via Transactional Outbox).
3. O SIGN consome o evento e gera automaticamente um **Contrato** com dois participantes (cliente + empresa).
4. Os participantes assinam o contrato **em ordem sequencial** (cliente → empresa).
5. Ao finalizar (todos assinam ou alguém recusa), o SIGN notifica o CRM com o status atualizado.

---

## Stack Tecnológica

| Categoria | Tecnologia |
|---|---|
| **Linguagem** | Java 17 |
| **Framework** | Spring Boot 4.1.0 |
| **Banco Relacional** | MySQL 8.0 (Flyway para migrações) |
| **Mensageria** | RabbitMQ 3 (Direct Exchange, Dead Letter Queue) |
| **Cache** | Redis 7 |
| **Observabilidade** | Micrometer Tracing (Brave/Zipkin), Actuator |
| **Containerização** | Docker / Docker Compose |
| **Testes** | JUnit 5, Mockito, Testcontainers |
| **CI/CD** | GitHub Actions |
| **Documentação API** | SpringDoc OpenAPI (Swagger UI) |

---

## Quick Start

### Pré-requisitos

- Docker e Docker Compose instalados
- JDK 17+ (apenas para desenvolvimento local)

### Execução com Docker Compose

```bash
# 1. Clone o repositório
git clone https://github.com/JoaoYM/proposal-signature-platform.git
cd proposal-signature-platform

# 2. Gere os artefatos (.jar) dos microsserviços ignorando os testes
cd crm && ./mvnw clean package -DskipTests
cd ../sign && ./mvnw clean package -DskipTests

# 3. Volte para a raiz e suba a infraestrutura
cd ..
docker-compose up -d --build
```

Aguarde aproximadamente 30 segundos para todos os serviços iniciarem.

### Acessos

| Recurso | URL |
|---|---|
| **CRM API (Swagger)** | http://localhost:8081/swagger-ui.html |
| **SIGN API (Swagger)** | http://localhost:8082/swagger-ui.html |
| **RabbitMQ Management** | http://localhost:15672 (guest/guest) |
| **Zipkin (Tracing)** | http://localhost:9411 |
| **MySQL** | localhost:3306 (root/root) |
| **Redis** | localhost:6379 |

---

## Boas Práticas Adotadas

### SOLID

- **S** — Cada classe tem uma responsabilidade única (Controllers delegam para Use Cases, que delegam para Ports).
- **O** — As portas (`EventPublisher`, `ContratoUseCase`) são interfaces abertas para extensão.
- **L** — Implementações concretas são substituíveis via injeção de dependência.
- **I** — Interfaces segregadas: `EventPublisher` expõe apenas `publish()`, sem métodos de infraestrutura.
- **D** — Módulo `domain` não conhece `infrastructure`. A inversão de dependência é garantida por portas e adaptadores.

### Domain-Driven Design (DDD)

- **Value Object**: `Assinatura` é um `@Embeddable` imutável que encapsula `dataHora`, `ipOrigem` e `hashValidacao`.
- **Rich Domain Model**: `Participante` contém métodos de negócio como `jaAssinou()` e `registrarAssinatura()`.
- **Agregados**: `Proposta` (com `ItemProposta`) e `Contrato` (com `Participante`) são raízes de agregado com `@Version` para lock otimista.
- **Repositórios**: Interfaces no domínio, implementações JPA na infraestrutura.

### Clean Architecture

```
adapters/    → Frameworks e drivers (Web, JPA, RabbitMQ)
application/ → Casos de uso (portas de entrada)
domain/      → Entidades, regras de negócio, portas de saída
```

### Resiliência

- **Transactional Outbox**: Eventos são persistidos no mesmo banco da entidade, garantindo consistência transacional.
- **Dead Letter Queue (DLQ)**: Mensagens que falham após 3 retries são enviadas para filas DLQ para análise.
- **Retry com Backoff**: 3 tentativas com intervalo inicial de 2s e multiplicador 1.5x.
- **Lock Otimista**: `@Version` em `Proposta` e `Contrato` previne concorrência.

### Observabilidade

- **Logs Estruturados**: Formato JSON com `traceId` e `spanId` para correlação distribuída.
- **Tracing Distribuído**: Integração com Zipkin via Micrometer Tracing (Brave).
- **Métricas**: Endpoints `/actuator/metrics` e `/actuator/health` expostos.

---

## Estrutura do Projeto

```
proposal-signature-platform/
├── crm/                          # Microsserviço CRM
│   ├── src/main/java/com/powermobile/crm/
│   │   ├── CrmApiApplication.java
│   │   ├── adapters/
│   │   │   ├── inbound/
│   │   │   │   ├── web/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── handler/
│   │   │   └── outbound/
│   │   │       ├── persistence/
│   │   │       ├── rabbitmq/
│   │   │       └── config/
│   │   ├── application/
│   │   │   └── port/
│   │   │       └── in/
│   │   └── domain/
│   │       ├── entity/
│   │       ├── port/
│   │       │   └── out/
│   │       ├── exception/
│   │       └── valueobject/
│   ├── src/test/java/com/powermobile/crm/
│   │   └── service/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-test.yml
│   │   └── db/migration/
│   ├── Dockerfile
│   └── pom.xml
├── sign/                         # Microsserviço SIGN
│   ├── src/main/java/com/powermobile/sign/
│   │   ├── SignApiApplication.java
│   │   ├── adapters/
│   │   │   ├── inbound/
│   │   │   │   ├── web/
│   │   │   │   ├── dto/
│   │   │   │   └── exception/
│   │   │   └── outbound/
│   │   │       ├── persistence/
│   │   │       ├── rabbitmq/
│   │   │       └── config/
│   │   ├── application/
│   │   │   └── port/
│   │   │       └── in/
│   │   └── domain/
│   │       ├── entity/
│   │       ├── port/
│   │       │   └── out/
│   │       ├── exception/
│   │       └── valueobject/
│   ├── src/test/java/com/powermobile/sign/
│   │   └── service/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-test.yml
│   │   └── db/migration/
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.yml
├── .github/workflows/ci.yml
├── README.md
├── ROTEIRO_TESTES.md
└── ARCHITECTURE.md
```

---

## Licença

Projeto de portfólio — código aberto para fins educacionais e de demonstração técnica.