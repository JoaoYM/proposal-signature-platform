# Roteiro de Testes — Proposal Signature Platform

Este documento guia o avaliador técnico através de todos os fluxos de negócio exigidos, utilizando comandos `curl` e payloads JSON. Os testes assumem que o ambiente está rodando via `docker compose up --build`.

Para tal, é necessário a compilação e execução do projeto, conforme instruções no README principal.

---

## Índice

1. [Criação de Proposta no CRM](#1-criação-de-proposta-no-crm)
2. [Verificação da Criação Automática do Contrato no SIGN](#2-verificação-da-criação-automática-do-contrato-no-sign)
3. [Cenário de Sucesso: Assinatura Sequencial](#3-cenário-de-sucesso-assinatura-sequencial)
4. [Cenário de Erro: Assinatura Fora de Ordem](#4-cenário-de-erro-assinatura-fora-de-ordem)
5. [Cenário de Recusa: Cancelamento do Fluxo](#5-cenário-de-recusa-cancelamento-do-fluxo)
6. [Verificação da Tabela de Auditoria](#6-verificação-da-tabela-de-auditoria)

---

## 1. Criação de Proposta no CRM

Cria uma proposta com dois itens para o cliente "João Silva".

```bash
curl -X POST http://localhost:8081/api/v1/propostas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteNome": "João Silva",
    "clienteEmail": "joao@email.com",
    "itens": [
      {
        "nome": "Consultoria de Arquitetura",
        "quantidade": 1,
        "precoUnitario": 5000.00
      },
      {
        "nome": "Licença de Software",
        "quantidade": 3,
        "precoUnitario": 1200.00
      }
    ]
  }'
```

**Resposta esperada (HTTP 201):**

```json
{
  "id": "...",
  "clienteNome": "João Silva",
  "clienteEmail": "joao@email.com",
  "status": "CRIADA",
  "itens": [
    { "id": "...", "nome": "Consultoria de Arquitetura", "quantidade": 1, "precoUnitario": 5000.00 },
    { "id": "...", "nome": "Licença de Software", "quantidade": 3, "precoUnitario": 1200.00 }
  ]
}
```

> **Anote o `id` da proposta.** Você precisará dele para as próximas etapas.

---

## 2. Verificação da Criação Automática do Contrato no SIGN

Após criar a proposta, o CRM publica um evento no RabbitMQ. O SIGN consome e gera automaticamente um contrato. Aguarde **5 segundos** (tempo do OutboxRelayWorker + processamento) e consulte:

> **Nota:** O SIGN não possui endpoint de listagem por propostaId exposto via controller. Para verificar, consulte diretamente o banco ou utilize o endpoint GET por ID após descobrir o ID do contrato.

**Alternativa — Buscar contrato por ID (após obter o ID via banco ou logs):**

```bash
curl -X GET http://localhost:8082/api/v1/contratos/<CONTRATO_ID> \
  -H "Content-Type: application/json"
```

**Resposta esperada (HTTP 200):**

```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "propostaId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "AGUARDANDO_ASSINATURA",
  "participantes": [
    { "nome": "João Silva", "email": "joao@email.com", "ordem": 1, "assinou": false, "assinatura": null },
    { "nome": "Diretor Power Mobile", "email": "diretor@powermobile.com", "ordem": 2, "assinou": false, "assinatura": null }
  ]
}
```

> **Anote o `id` do contrato.** Você precisará dele para os testes de assinatura.
> O `id` do contrato pode ser obtido através de logs do SIGN ou consultando diretamente a tabela `contrato` no banco de dados.
> Para consultar os logs do SIGN: docker-compose logs sign-api

```sql

---

## 3. Cenário de Sucesso: Assinatura Sequencial

### 3.1 Participante 1 (Cliente) assina o contrato

```bash
curl -X POST http://localhost:8082/api/v1/contratos/<CONTRATO_ID>/assinaturas \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@email.com",
    "aceitou": true
  }'
```

**Resposta esperada (HTTP 200):**

```
Assinatura registrada com sucesso.
```

### 3.2 Participante 2 (Empresa) assina o contrato

```bash
curl -X POST http://localhost:8082/api/v1/contratos/<CONTRATO_ID>/assinaturas \
  -H "Content-Type: application/json" \
  -d '{
    "email": "diretor@powermobile.com",
    "aceitou": true
  }'
```

**Resposta esperada (HTTP 200):**

```
Assinatura registrada com sucesso.
```

### 3.3 Verificar status final do contrato

```bash
curl -X GET http://localhost:8082/api/v1/contratos/<CONTRATO_ID> \
  -H "Content-Type: application/json"
```

**Resposta esperada (HTTP 200):**

```json
{
  "id": "660e8400-...",
  "propostaId": "550e8400-...",
  "status": "CONCLUIDO",
  "participantes": [
    {
      "nome": "João Silva",
      "email": "joao@email.com",
      "ordem": 1,
      "assinou": true,
      "assinatura": {
        "dataHora": "2026-07-23T19:30:00",
        "ipOrigem": "127.0.0.1",
        "hashValidacao": "abc123..."
      }
    },
    {
      "nome": "Diretor Power Mobile",
      "email": "diretor@powermobile.com",
      "ordem": 2,
      "assinou": true,
      "assinatura": {
        "dataHora": "2026-07-23T19:31:00",
        "ipOrigem": "127.0.0.1",
        "hashValidacao": "def456..."
      }
    }
  ]
}
```

### 3.4 Verificar que o CRM foi notificado

Consulte a proposta no CRM para confirmar que o status foi atualizado para `ASSINATURA_CONCLUIDA`:

```bash
curl -X GET http://localhost:8081/api/v1/propostas/<PROPOSTA_ID> \
  -H "Content-Type: application/json"
```

**Resposta esperada:** `"status": "ASSINATURA_CONCLUIDA"`

---

## 4. Cenário de Erro: Assinatura Fora de Ordem

Este teste requer um **novo contrato** (crie uma nova proposta e aguarde a geração do contrato).

### 4.1 Tentar assinar com o Participante 2 (Empresa) antes do Participante 1 (Cliente)

```bash
curl -X POST http://localhost:8082/api/v1/contratos/<NOVO_CONTRATO_ID>/assinaturas \
  -H "Content-Type: application/json" \
  -d '{
    "email": "diretor@powermobile.com",
    "aceitou": true
  }'
```

**Resposta esperada (HTTP 422 — UNPROCESSABLE_ENTITY):**

```json
{
  "message": "Não é o turno deste participante assinar. Aguarde o anterior.",
  "errorCode": "DOMAIN_ERROR",
  "details": []
}
```

### 4.2 Verificar que o contrato permanece em `AGUARDANDO_ASSINATURA`

```bash
curl -X GET http://localhost:8082/api/v1/contratos/<NOVO_CONTRATO_ID> \
  -H "Content-Type: application/json"
```

**Resposta esperada:** `"status": "AGUARDANDO_ASSINATURA"`

---

## 5. Cenário de Recusa: Cancelamento do Fluxo

Este teste requer um **novo contrato** (crie outra proposta e aguarde a geração do contrato).

### 5.1 Participante 1 (Cliente) recusa o contrato

```bash
curl -X POST http://localhost:8082/api/v1/contratos/<OUTRO_CONTRATO_ID>/assinaturas \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@email.com",
    "aceitou": false
  }'
```

**Resposta esperada (HTTP 200):**

```
Assinatura recusada. Fluxo do contrato cancelado.
```

### 5.2 Verificar status do contrato

```bash
curl -X GET http://localhost:8082/api/v1/contratos/<OUTRO_CONTRATO_ID> \
  -H "Content-Type: application/json"
```

**Resposta esperada:** `"status": "CANCELADO"`

### 5.3 Verificar que o CRM foi notificado

```bash
curl -X GET http://localhost:8081/api/v1/propostas/<PROPOSTA_ID> \
  -H "Content-Type: application/json"
```

**Resposta esperada:** `"status": "ASSINATURA_RECUSADA"`

---

## 6. Verificação da Tabela de Auditoria

A tabela `contrato_audit_log` registra cronologicamente todas as ações do fluxo de assinatura.

### 6.1 Conectar ao banco MySQL

```bash
docker exec -it proposal-signature-platform_mysql_1 mysql -uroot -proot proposal_platform
```

### 6.2 Consultar auditoria de um contrato específico

```sql
SELECT * FROM contrato_audit_log 
WHERE contrato_id = '<CONTRATO_ID>' 
ORDER BY created_at ASC;
```

**Resultado esperado (ordem cronológica):**

| acao | ator | detalhes |
|---|---|---|
| `CONTRATO_GERADO` | Sistema | Contrato criado a partir da proposta |
| `ASSINATURA_REGISTRADA` | João Silva | Assinatura realizada com sucesso |
| `ASSINATURA_REGISTRADA` | Diretor Power Mobile | Assinatura realizada com sucesso |
| `CONTRATO_FINALIZADO` | Sistema | Todas as assinaturas concluídas |

### 6.3 Para o cenário de recusa

```sql
SELECT * FROM contrato_audit_log 
WHERE contrato_id = '<CONTRATO_CANCELADO_ID>' 
ORDER BY created_at ASC;
```

**Resultado esperado:**

| acao | ator | detalhes |
|---|---|---|
| `CONTRATO_GERADO` | Sistema | Contrato criado a partir da proposta |
| `ASSINATURA_RECUSADA` | João Silva | Participante recusou o contrato |

---

## Resumo dos Cenários Testados

| # | Cenário | Status Esperado | Código HTTP |
|---|---|---|---|
| 1 | Criação de proposta | `CRIADA` | 201 |
| 2 | Contrato gerado automaticamente | `AGUARDANDO_ASSINATURA` | 200 |
| 3 | Assinatura sequencial (cliente → empresa) | `CONCLUIDO` | 200 |
| 4 | Tentativa de assinar fora de ordem | `DOMAIN_ERROR` | 422 |
| 5 | Recusa de contrato | `CANCELADO` | 200 |
| 6 | Auditoria cronológica | 4 eventos (sucesso) / 2 eventos (recusa) | — |