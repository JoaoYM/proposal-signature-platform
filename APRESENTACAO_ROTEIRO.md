# Roteiro de apresentação

1. Subir uma cópia limpa com `docker compose up --build` e observar os healthchecks.
2. Criar proposta no CRM e guardar o `id`.
3. Consultar `GET /api/v1/contratos?propostaId=<id>` e mostrar a atualização assíncrona CRM → SIGN → CRM.
4. Demonstrar ordem obrigatória, recusa e conclusão usando `ROTEIRO_TESTES.md`.
5. Mostrar outbox com confirmação, envelope v1, inbox idempotente e mensagens em DLQ preservadas para inspeção/reprocessamento controlado.
6. Explicar que o aceite é uma simulação e separar as evidências implementadas das decisões pendentes em `SECURITY_SCOPE.md`.

## Evolução condicionada

BDD executável, coleção Insomnia e o framework `.ai/` só devem ser apresentados depois que a suíte de integração com MySQL/RabbitMQ/Testcontainers estiver verde. O fluxo futuro será: card → Gherkin → teste → implementação → validações → Insomnia → evidências.
