# Escopo de segurança da POC

## Implementado

- Conteúdo do contrato marcado como imutável no mapeamento de persistência.
- Evidência SHA-256 de contrato, proposta, conteúdo, participante, instante e IP observado.
- IP obtido da conexão HTTP; cabeçalhos de proxy não são aceitos sem uma lista explícita de proxies confiáveis.
- Auditoria funcional de geração, aceite, recusa e conclusão.
- Idempotência de eventos por `eventId`, envelope versionado e rejeição de eventos antigos.

## Não implementado — bloqueia qualquer alegação jurídica

- Token individual, temporário, de uso único e armazenado apenas como hash.
- Autenticação, autorização e gestão de identidade dos participantes.
- TLS, gestão/rotação de segredos, criptografia em repouso e política LGPD de retenção/eliminação.
- Rate limiting, antifraude, carimbo de tempo confiável e cadeia de custódia externa.
- Documento final canônico (por exemplo PDF), hash antes do convite e preservação WORM.
- Definição jurídica do tipo de assinatura e requisitos de consentimento/evidência.

Esses itens exigem threat model, requisitos de produto e validação jurídica antes de implementação produtiva.
