# Entrega 04: rastreabilidade e confirmação de recebimento/uso hospitalar

Minuta de PR da branch `feature/entrega-04-rastreabilidade-recebimento` para `main`. Publicar como draft após autorização da equipe. Não realizar merge nem deploy nesta atividade.

## Problema e comportamento resultante

As bolsas tinham apenas dados mínimos de descarte, e as requisições não se relacionavam com bolsas entregues. A US09 passa a persistir eventos cronológicos preservados de cadastro até uso. A US10 acrescenta atestação explícita de entrega ocorrida, confirmação de condição e utilização pelo hospital correspondente, com prevenção de duplicidade e alerta de confirmação atrasada.

Para uma requisição de uma bolsa, o fluxo fica `SOLICITADA → ENTREGUE → RECEBIDA → UTILIZADA`. Quantidades maiores usam estados parciais; não conformidade bloqueia a bolsa. Cada transição grava o evento na mesma transação. PostgreSQL bloqueia UPDATE/DELETE/TRUNCATE do histórico.

## Escopo

- API, serviços, entidades, persistência e telas integradas ao menu existente.
- Reaproveitamento da sugestão ABO/Rh e FEFO de hemácias, com escolha manual. Reserva automática da US03 permanece pendente.
- Correção de listagem entre contextos hospitalares, validação transitória JPA e evolução de CHECK de status dos bancos anteriores.
- Testes de aceitação, rollback, SQL direto, migração e concorrência; job PostgreSQL na CI.
- POST-IT, BDD, tracker, evidências, roteiros dos dois screencasts e apresentação; relatório com pendências reais.

## Validação

Conferir os resultados e comandos finais em `entrega-04/homologacao.md`. Há capturas da interface e relatórios reproduzíveis em `entrega-04/evidencias/`. Logs brutos locais estão em `.git/entrega04-*.log`, fora do Git por não serem artefatos de publicação necessários.

## Limites e revisão

Acesso por `X-Perfil`/`X-Hospital-Id` continua didático, sem identidade individual robusta. Associação hospitalar cobre hemácias. Entrega é atestação manual, sem transporte. Alertas são locais, sem mensagens externas. Não há uso clínico homologado. Conferir permissões DDL do usuário PostgreSQL antes do deploy.

Vídeos YouTube e execução remota da esteira só podem ser marcados concluídos depois de publicados/executados. Relacionar os números reais das issues após publicação; não usar referências fictícias.
