# Architecture Guide — Proposal Signature Platform

Documentação técnica aprofundada da arquitetura de microsserviços, decisões de design e topologia do sistema.

---

## Índice

1. [Estrutura de Diretórios (Clean Architecture)](#1-estrutura-de-diretórios-clean-architecture)
2. [Diagramas C4 e de Sequência](#2-diagramas-c4-e-de-sequência)
   - [2.1 Diagrama de Componentes (C4 Container Level)](#21-diagrama-de-componentes-c4-container-level)
   - [2.2 Diagrama de Sequência de Negócio](#22-diagrama-de-sequência-de-negócio)
   - [2.3 Diagrama de Sequência Técnico (Transactional Outbox)](#23-diagrama-de-sequência-técnico-transactional-outbox)
   - [2.4 Diagrama Entidade-Relacionamento (ERD)](#24-diagrama-entidade-relacionamento-erd)
3. [Defesas Técnicas](#3-defesas-técnicas)

---

## 1. Estrutura de Diretórios (Clean Architecture)

Abaixo está a estrutura de diretórios do microsserviço CRM (o SIGN segue o mesmo padrão). Cada camada tem responsabilidades rigidamente isoladas.

```
crm/src/main/java/com/powermobile/crm/
│
├── CrmApiApplication.java              # Ponto de entrada Spring Boot
│
├── adapters/                           # 🔌 FRAMEWORKS & DRIVERS
│   ├── inbound/
│   │   └── web/
│   │       ├── PropostaController.java     # REST endpoints
│   │       ├── dto/                         # Request/Response DTOs
│   │       │   ├── PropostaRequest.java
│   │       │   ├── PropostaResponseDTO.java
│   │       │   ├── ItemPropostaRequest.java
│   │       │   └── ItemResponseDTO.java
│   │       └── handler/
│   │           └── GlobalExceptionHandler.java  # Tratamento global de erros
│   │
│   └── outbound/
│       ├── persistence/
│       │   ├── OutboxRelayWorker.java         # Relay do Outbox → RabbitMQ
│       │   └── JpaPropostaRepository.java     # Implementação JPA (implícita)
│       ├── rabbitmq/
│       │   ├── RabbitMQConfig.java            # Filas, exchanges, DLQs
│       │   ├── ContratoStatusListener.java    # Consumidor de eventos do SIGN
│       │   └── DlqConsumer.java               # Consumidor Dead Letter Queue
│       └── config/
│           └── CacheConfig.java               # @EnableCaching
│
├── application/                         # 🎯 CASOS DE USO
│   └── port/
│       └── in/
│           └── CreatePropostaUseCase.java     # Interface de entrada (porta)
│
└── domain/                              # 🏛️ NÚCLEO DO NEGÓCIO
    ├── entity/
    │   ├── Proposta.java                    # Agregado raiz
    │   ├── ItemProposta.java                # Entidade filha
    │   └── OutboxEvent.java                 # Evento do Transactional Outbox
    ├── port/
    │   └── out/
    │       └── EventPublisher.java          # Porta de saída (DIP)
    ├── exception/
    │   ├── PropostaNotFoundException.java
    │   └── PropostaDomainException.java
    └── valueobject/
        └── PropostaStatus.java              # Enum → Value Object
```

### Papel de cada camada

| Camada | Responsabilidade | Depende de |
|---|---|---|
| **adapters/inbound** | Receber requisições HTTP, validar entrada, converter para DTOs, delegar para casos de uso | `application/port/in` |
| **adapters/outbound** | Implementar portas de saída (JPA, RabbitMQ, Redis). Conter toda a lógica de infraestrutura | `domain/port/out` |
| **application** | Orquestrar casos de uso. Não contém regras de negócio — apenas coordenação | `domain/port/out` |
| **domain** | Entidades, regras de negócio, value objects, exceções de domínio e portas (interfaces) | Nenhuma (camada mais interna) |

**Regra de Ouro:** O `domain` não importa nada de `infrastructure`, `adapters` ou `application`. A inversão de dependência é garantida porque as portas de saída (`EventPublisher`) são interfaces definidas no domínio e implementadas nos adapters.

---

## 2. Diagramas C4 e de Sequência

### 2.1 Diagrama de Componentes (C4 Container Level)

```mermaid
graph TD
    subgraph "Atores Externos"
        U[👤 Usuário / Cliente]
        S[🔧 Sistema Externo]
    end

    subgraph "Microsserviço CRM :8081"
        PC[PropostaController]
        PS[PropostaServiceImpl]
        EP[EventPublisher]
        OW[OutboxRelayWorker]
        CSL[ContratoStatusListener]
    end

    subgraph "Microsserviço SIGN :8082"
        CC[ContratoController]
        CS[ContratoServiceImpl]
        EP2[EventPublisher]
        OW2[OutboxRelayWorker]
        PCL[PropostaCriadaListener]
    end

    subgraph "Infraestrutura Compartilhada"
        MQ[🐰 RabbitMQ<br/>proposta.exchange]
        MQ2[🐰 RabbitMQ DLQ<br/>proposta.criada.dlq<br/>contrato.status.dlq]
        RD[🗄️ Redis 7<br/>Cache de consultas]
        ZK[🔍 Zipkin<br/>Tracing distribuído]
    end

    subgraph "Bancos de Dados"
        DB1[(MySQL CRM<br/>propostas, outbox_events)]
        DB2[(MySQL SIGN<br/>contratos, audit_log, outbox_events)]
    end

    U -->|HTTP POST/GET| PC
    U -->|HTTP POST/GET| CC
    PC --> PS
    PS --> EP
    EP -->|Salva evento PENDING| DB1
    OW -->|Poll a cada 5s| DB1
    OW -->|Publica mensagem| MQ
    MQ -->|proposta.criada| PCL
    PCL --> CS
    CS -->|Cria contrato| DB2
    CS --> EP2
    EP2 -->|Salva evento PENDING| DB2
    OW2 -->|Poll a cada 5s| DB2
    OW2 -->|Publica mensagem| MQ
    MQ -->|contrato.status| CSL
    CSL -->|Atualiza status| DB1
    MQ -->|Falha após 3 retries| MQ2
    PC -.->|Cacheable| RD
    CC -.->|Cacheable / CacheEvict| RD
    PC -.-> ZK
    CC -.-> ZK
    MQ -.-> ZK
```

### 2.2 Diagrama de Sequência de Negócio

```mermaid
sequenceDiagram
    actor Cliente
    participant CRM as CRM API
    participant DB1 as MySQL CRM
    participant MQ as RabbitMQ
    participant SIGN as SIGN API
    participant DB2 as MySQL SIGN
    participant Empresa as Diretor

    Note over Cliente,Empresa: 🔵 FLUXO DE SUCESSO

    Cliente->>CRM: POST /api/v1/propostas
    CRM->>DB1: Salva Proposta (status=CRIADA)
    CRM->>DB1: Salva OutboxEvent (status=PENDING)
    CRM-->>Cliente: 201 Created (PropostaResponseDTO)

    Note over CRM,SIGN: OutboxRelayWorker (poll a cada 5s)
    CRM->>DB1: SELECT * FROM outbox_events WHERE status='PENDING'
    CRM->>MQ: Publica evento (proposta.criada)
    CRM->>DB1: UPDATE outbox_events SET status='PROCESSED'

    MQ->>SIGN: Entrega mensagem
    SIGN->>DB2: Verifica idempotência (findByPropostaId)
    SIGN->>DB2: Cria Contrato (status=AGUARDANDO_ASSINATURA)
    SIGN->>DB2: Cria Participante 1 (Cliente, ordem=1)
    SIGN->>DB2: Cria Participante 2 (Empresa, ordem=2)
    SIGN->>DB2: Registra auditoria (CONTRATO_GERADO)

    Note over Cliente,Empresa: 🔵 ASSINATURA SEQUENCIAL

    Cliente->>SIGN: POST /contratos/{id}/assinaturas (aceitou=true)
    SIGN->>DB2: Verifica ordem (Participante 1 pode assinar)
    SIGN->>DB2: Registra assinatura do Cliente
    SIGN->>DB2: Registra auditoria (ASSINATURA_REGISTRADA)
    SIGN-->>Cliente: 200 OK

    Empresa->>SIGN: POST /contratos/{id}/assinaturas (aceitou=true)
    SIGN->>DB2: Verifica ordem (Participante 2 pode assinar)
    SIGN->>DB2: Registra assinatura do Diretor
    SIGN->>DB2: Registra auditoria (ASSINATURA_REGISTRADA)
    SIGN->>DB2: Atualiza Contrato (status=CONCLUIDO)
    SIGN->>DB2: Registra auditoria (CONTRATO_FINALIZADO)
    SIGN->>DB2: Salva OutboxEvent (status=PENDING)
    SIGN-->>Empresa: 200 OK

    Note over CRM,SIGN: OutboxRelayWorker (poll a cada 5s)
    SIGN->>DB2: SELECT * FROM outbox_events WHERE status='PENDING'
    SIGN->>MQ: Publica evento (contrato.status)
    MQ->>CRM: Entrega mensagem
    CRM->>DB1: Atualiza Proposta (status=ASSINATURA_CONCLUIDA)

    Note over Cliente,Empresa: 🔴 CENÁRIO DE ERRO (FORA DE ORDEM)

    Empresa->>SIGN: POST /contratos/{id}/assinaturas (antes do Cliente)
    SIGN->>DB2: Verifica ordem (Participante 2 NÃO pode assinar)
    SIGN-->>Empresa: 422 UNPROCESSABLE_ENTITY (DOMAIN_ERROR)

    Note over Cliente,Empresa: 🟡 CENÁRIO DE RECUSA

    Cliente->>SIGN: POST /contratos/{id}/assinaturas (aceitou=false)
    SIGN->>DB2: Atualiza Contrato (status=CANCELADO)
    SIGN->>DB2: Registra auditoria (ASSINATURA_RECUSADA)
    SIGN->>DB2: Salva OutboxEvent (status=PENDING)
    SIGN-->>Cliente: 200 OK
    Note over CRM,SIGN: OutboxRelayWorker → RabbitMQ → CRM
    CRM->>DB1: Atualiza Proposta (status=ASSINATURA_RECUSADA)
```

### 2.3 Diagrama de Sequência Técnico (Transactional Outbox)

```mermaid
sequenceDiagram
    participant App as Aplicação (Service)
    participant DB as MySQL
    participant Worker as OutboxRelayWorker
    participant MQ as RabbitMQ
    participant DLQ as Dead Letter Queue

    Note over App,DLQ: 🔄 TRANSAÇÃO ATÔMICA (PASSO 1)

    App->>DB: BEGIN TRANSACTION
    App->>DB: INSERT INTO propostas (...)
    App->>DB: INSERT INTO outbox_events (aggregate_type, aggregate_id, payload, status='PENDING')
    App->>DB: COMMIT
    Note right of DB: ✅ Garantia: proposta E evento<br/>são salvos ou nenhum é

    Note over App,DLQ: 🔄 RELAY WORKER (PASSO 2) — Poll a cada 5s

    Worker->>DB: SELECT * FROM outbox_events WHERE status='PENDING' ORDER BY created_at ASC
    DB-->>Worker: Lista de eventos pendentes

    loop Para cada evento pendente
        Worker->>MQ: convertAndSend(exchange, routingKey, payload)
        
        alt Publicação bem-sucedida
            MQ-->>Worker: ACK (confirmação)
            Worker->>DB: UPDATE outbox_events SET status='PROCESSED' WHERE id=?
            Note right of DB: ✅ Mensagem publicada,<br/>evento marcado como processado
        else Falha na publicação (ex: RabbitMQ offline)
            Worker->>Worker: Log de erro
            Note right of Worker: ⏳ Transação faz rollback<br/>Evento permanece PENDING<br/>→ Tentará novamente no próximo ciclo
        end
    end

    Note over App,DLQ: 🔄 CONSUMO E RETRY (PASSO 3)

    MQ->>Consumer: Entrega mensagem
    Consumer->>Consumer: Processa mensagem

    alt Processamento OK
        Consumer-->>MQ: ACK (mensagem removida da fila)
    else Falha no processamento
        Consumer-->>MQ: NACK (não reenfileirar)
        MQ->>MQ: Aplica retry policy (3 tentativas, backoff 2s, 1.5x)
        
        loop Retry (até 3 tentativas)
            MQ->>Consumer: Re-entrega mensagem
            alt Sucesso no retry
                Consumer-->>MQ: ACK
            end
        end
        
        Note over MQ,DLQ: Após 3 tentativas falhas
        MQ->>DLQ: Encaminha mensagem para Dead Letter Queue
        Note right of DLQ: 🚨 Mensagem em DLQ<br/>para análise manual
    end
```

### 2.4 Diagrama Entidade-Relacionamento (ERD)

```mermaid
erDiagram
    PROPOSTA ||--o{ ITEM_PROPOSTA : "contém"
    PROPOSTA {
        uuid id PK
        string cliente_nome
        string cliente_email
        enum status "CRIADA | ENVIADA_PARA_ASSINATURA | ASSINATURA_CONCLUIDA | ASSINATURA_RECUSADA"
        bigint version "Lock otimista (@Version)"
    }
    ITEM_PROPOSTA {
        uuid id PK
        string nome
        int quantidade
        decimal preco_unitario
        uuid proposta_id FK
    }

    CONTRATO ||--o{ PARTICIPANTE : "possui"
    CONTRATO {
        uuid id PK
        string proposta_id UK "ID da proposta no CRM"
        text conteudo "JSON com termos do contrato"
        enum status "GERADO | AGUARDANDO_ASSINATURA | CONCLUIDO | CANCELADO"
        bigint version "Lock otimista (@Version)"
    }
    PARTICIPANTE {
        uuid id PK
        string nome
        string email
        int ordem "Ordem de assinatura (1, 2, 3...)"
        uuid contrato_id FK
        datetime assinatura_data_hora "Embedded (@Embeddable)"
        string assinatura_ip_origem "Embedded (@Embeddable)"
        string assinatura_hash_validacao "Embedded (@Embeddable)"
    }

    CONTRATO_EVENT {
        uuid id PK
        string contrato_id
        enum acao "CONTRATO_GERADO | ASSINATURA_REGISTRADA | ASSINATURA_RECUSADA | CONTRATO_FINALIZADO"
        string ator "Sistema | Nome do participante"
        text detalhes "JSON flexível para metadados"
        datetime created_at "Auditable (@CreatedDate)"
    }

    OUTBOX_EVENT {
        uuid id PK
        string aggregate_type "PROPOSTA | CONTRATO_STATUS"
        string aggregate_id
        text payload "JSON do evento"
        string status "PENDING | PROCESSED"
        datetime created_at
    }

    CONTRATO ||--o{ CONTRATO_EVENT : "gera eventos de auditoria"
```

---

## 3. Defesas Técnicas

### 3.1 Consistência em Falhas: Retry e Dead Letter Queue

**Problema:** Em sistemas distribuídos, mensagens podem falhar por razões transitórias (banco indisponível, timeout de rede) ou permanentes (payload inválido, bug no consumidor).

**Solução em camadas:**

| Camada | Mecanismo | Comportamento |
|---|---|---|
| **1. Retry com Backoff** | `spring.rabbitmq.listener.simple.retry` | 3 tentativas com intervalo inicial de 2s e multiplicador 1.5x. Erros transitórios são resolvidos automaticamente. |
| **2. Dead Letter Queue** | Filas DLQ configuradas no `RabbitMQConfig` | Após esgotar o retry, a mensagem é enviada para a DLQ. O `DlqConsumer` registra o payload em log para diagnóstico. |
| **3. Transactional Outbox** | Tabela `outbox_events` + `OutboxRelayWorker` | Garante que a mensagem só é publicada após o commit da transação. Se o RabbitMQ falhar, o evento permanece `PENDING` e é retentado no próximo ciclo de 5s. |

**Por que isso é superior a um simples try/catch?** O padrão Outbox garante **consistência eventual forte**: ou a transação inteira é commitada (proposta + evento) ou nada é salvo. Não há janela de inconsistência entre o banco e a fila.

### 3.2 DDD com Value Objects (@Embeddable)

**Problema:** Em modelos anêmicos, dados como `dataHora`, `ipOrigem` e `hashValidacao` da assinatura seriam colunas soltas na tabela `participantes`, sem encapsulamento ou invariantes.

**Solução:** `Assinatura` é um `@Embeddable` que agrupa esses 3 campos em um Value Object imutável:

```java
@Embeddable
@Data
@Builder
public class Assinatura {
    private LocalDateTime dataHora;
    private String ipOrigem;
    private String hashValidacao;
}
```

**Benefícios:**
- **Encapsulamento:** O `Participante` só expõe `registrarAssinatura(Assinatura)` — o caller não precisa saber quais campos compõem uma assinatura.
- **Imutabilidade:** Uma vez criada, a assinatura não pode ser alterada (sem setters públicos no Value Object).
- **Testabilidade:** `Assinatura.builder().dataHora(...).ipOrigem(...).build()` é fácil de construir em testes.
- **Achatamento no banco:** O JPA mapeia as 3 propriedades como colunas na tabela `participantes` (`assinatura_data_hora`, `assinatura_ip_origem`, `assinatura_hash_validacao`), sem necessidade de tabela extra.

### 3.3 Redis: Cache de Consultas e Idempotência

**Problema:** Os endpoints `GET /api/v1/propostas/{id}` e `GET /api/v1/contratos/{id}` são consultados repetidamente por participantes que verificam o contrato antes de assinar. Cada consulta ia ao MySQL sem necessidade.

**Solução — Cache de Consulta na Borda (Controller):**

O cache é aplicado nos Controllers, retornando DTOs serializáveis ao invés de Entidades JPA. Isso evita problemas de serialização do Hibernate (lazy loading, proxies) e mantém a camada de domínio isolada de preocupações de infraestrutura.

```java
@Cacheable(value = "propostas", key = "#id")
@ResponseStatus(HttpStatus.OK)
public PropostaResponseDTO buscarPorId(@PathVariable UUID id) {
    Proposta proposta = propostaService.buscarPorId(id);
    return PropostaResponseDTO.fromEntity(proposta);
}
```

> **Nota:** O cache armazena DTOs na borda (Controller) para evitar problemas de serialização de entidades JPA (como `LazyInitializationException` ou proxies do Hibernate) e proteger a camada de domínio de dependências de infraestrutura. Como os DTOs são POJOs simples, são perfeitamente serializáveis pelo Redis sem configuração adicional.

| Aspecto | Sem Redis | Com Redis |
|---|---|---|
| Latência (primeira chamada) | ~100ms (MySQL) | ~100ms (MySQL + populates cache) |
| Latência (chamadas subsequentes) | ~100ms (MySQL) | ~5ms (cache hit) |
| Carga no banco | N consultas por segundo | 1 consulta a cada TTL (5 min) |

**Potencial Futuro — Idempotência via Redis:**

Atualmente, a idempotência no `PropostaCriadaListener` é feita via banco (`findByPropostaId`). Para cenários de alto throughput, Redis oferece uma alternativa mais performática:

```
SET NX proposta:<propostaId> "processing" EX 60
```

Se retorna `OK` → primeira vez, processa. Se retorna `nil` → já processado, descarta. Isso reduz a carga no MySQL e oferece TTL automático para limpeza de chaves expiradas.

**Por que não usar Redis para tudo?** O cache é ideal para dados de consulta frequente com baixa taxa de atualização. Para dados transacionais (status de proposta/contrato), o MySQL continua sendo a fonte da verdade. O Redis é um acelerador, não um substituto.

---

## Resumo das Decisões Arquiteturais

| Decisão | Alternativa Rejeitada | Motivo |
|---|---|---|
| **Transactional Outbox** vs. Publicação direta no RabbitMQ | Publicação direta dentro da transação | Se o RabbitMQ falhar, a transação é perdida. O Outbox garante que nenhum evento seja perdido. |
| **DLQ + Retry** vs. Requeue infinito | Requeue infinito (default do Spring) | Mensagens inválidas entrariam em loop infinito, consumindo recursos. DLQ isola falhas para análise. |
| **@Embeddable (Value Object)** vs. Tabela separada para Assinatura | Tabela `assinaturas` com FK para `participantes` | Assinatura não é uma entidade — não tem identidade própria. Como Value Object, é achada na mesma tabela, sem JOIN. |
| **Redis para cache** vs. Redis para estado da sessão | Sessão em Redis | O projeto não tem autenticação. Cache de consulta é o uso mais natural e demonstrável. |
| **Micrometer Tracing** vs. OpenTelemetry Agent | OpenTelemetry Agent (instrumentação automática) | Micrometer é nativo do Spring Boot 4.x, mais simples de configurar e suficiente para tracing distribuído. |