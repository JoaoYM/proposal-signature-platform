# language: pt
Funcionalidade: Assinatura de proposta
  Como operador comercial
  Quero que uma proposta gere um contrato assinável
  Para acompanhar o resultado no CRM

  Cenário: assinatura sequencial concluída
    Dado que uma nova proposta foi criada
    Quando o cliente aceita o contrato
    E o diretor aceita o contrato
    Então o contrato deve estar "CONCLUIDO"
    E a proposta deve estar "ASSINATURA_CONCLUIDA"

  Cenário: assinatura fora de ordem é recusada
    Dado que uma nova proposta foi criada
    Quando o diretor tenta assinar antes do cliente
    Então a operação deve retornar HTTP 422
    E o contrato deve estar "AGUARDANDO_ASSINATURA"

  Cenário: recusa cancela o fluxo
    Dado que uma nova proposta foi criada
    Quando o cliente recusa o contrato
    Então o contrato deve estar "CANCELADO"
    E a proposta deve estar "ASSINATURA_RECUSADA"
