# POST-IT — Entrega 04 — 09/11

## US09 — Rastreabilidade completa da bolsa

**Como** gestor de estoque do hemocentro, **eu quero** consultar os eventos registrados automaticamente para cada bolsa, **para que** acompanhe seu ciclo com histórico auditável e preservado.

**Perfil:** gestor de hemocentro.

### Funcionalidades implementadas

- Eventos de cadastro, atualização com retrato antes/depois, descarte, entrega atestada, recebimento íntegro ou não conforme, utilização e confirmação atrasada.
- Bolsa identificada por ID, tipo do evento, data/hora UTC, origem declarada e detalhes persistidos no PostgreSQL.
- Ordem cronológica crescente e ID como desempate, API `GET /bolsas/{id}/historico` e consulta nas telas de estoque e atendimento.
- Mudança de estado e evento na mesma transação; falha do evento reverte a operação.
- Sem endpoints de escrita no histórico; repositório de leitura, callbacks JPA contra atualização/exclusão e gatilho PostgreSQL contra `UPDATE`, `DELETE` e `TRUNCATE`.
- Origem `PERFIL:GESTOR_HEMOCENTRO`, `PERFIL:HOSPITAL_SOLICITANTE;HOSPITAL:{id}`, `SISTEMA:VALIDADE` ou `SISTEMA:PENDENCIAS`, conforme a operação.

### Critérios de aceitação BDD

**Cenário 1 — Consulta do histórico**

**Dado** que uma bolsa passou por diferentes estados, **quando** o gestor consultar seu histórico, **então** o sistema apresenta os eventos registrados em ordem cronológica, com data/hora e responsável ou origem.

**Cenário 2 — Proteção dos registros**

**Dado** que um evento foi registrado, **quando** alguém tentar modificá-lo ou excluí-lo, **então** a operação é bloqueada e o registro original preservado.

### Evidências e testes

`AtendimentoHospitalarAceitacaoTest`: sequência cadastro → entrega → recebimento → uso, cronologia, origem, edição/descarte, API sem PUT/DELETE e legado. `RastreabilidadeTransacaoTest`: rollback provocado por falha de auditoria. `HistoricoPostgresIT`: SQL direto bloqueado e preservação do conteúdo original. Resultados efetivamente executados e capturas constam em [homologação](homologacao.md).

### Limitações e política de legado

Não há retroatividade: bolsas anteriores não ganham eventos reconstruídos. O antigo registro de descarte permanece na entidade original. Não são gerados eventos de transporte ou uso presumidos. A proteção de banco não impede um administrador com privilégio DDL de remover gatilhos ou reescrever a base; não é blockchain nem prova criptográfica. O mecanismo didático não autentica individualmente o responsável.

## US10 — Confirmação de recebimento e uso hospitalar

**Como** hospital solicitante, **eu quero** confirmar a condição de recebimento e registrar a utilização das bolsas entregues para minha requisição, **para que** o hemocentro acompanhe o desfecho e os recebimentos ainda não confirmados.

**Perfil:** hospital solicitante; gestor registra entrega ocorrida e acompanha; logística consulta alertas.

### Funcionalidades implementadas

- Associação explícita entre requisição e bolsa por atestação de entrega já realizada, com validação ABO/Rh de hemácias e escolha manual entre sugestões FEFO.
- Bloqueio de bolsa indisponível, vencida, incompatível, já entregue e atendimento além da quantidade solicitada.
- Confirmação pelo hospital correspondente ao contexto declarado `X-Hospital-Id`; condição `INTEGRA` ou `NAO_CONFORME`; observação obrigatória para não conformidade.
- Recebimento íntegro habilita uso enquanto a bolsa permanece válida; não conformidade bloqueia o uso.
- Status agregado da requisição, incluindo estados parciais para quantidades maiores que um.
- Registro de utilização sem dados de paciente, com data/hora e origem.
- Travas pessimistas e unicidade da bolsa evitam entrega dupla, confirmação e utilização duplicadas.
- Prazo configurável `recebimento.prazo` (padrão `PT24H`) armazenado por entrega; alerta persistido uma vez após ultrapassar o prazo, na inicialização, agendamento ou consulta de pendências.
- Recebimento tardio resolve a pendência ativa, preservando o alerta e o evento anteriores.
- Eventos integrados à US09 na mesma transação e interface `atendimento.html`.

### Critérios de aceitação BDD

**Cenário 1 — Recebimento íntegro**

**Dado** que uma bolsa foi entregue ao hospital e a entrega foi atestada, **quando** o hospital autorizado confirmar recebimento com condição íntegra, **então** a requisição de uma bolsa passa a `RECEBIDA` e a bolsa fica apta ao registro de utilização, respeitada a validade.

**Cenário 2 — Confirmação pendente**

**Dado** que uma bolsa entregue ultrapassou o prazo configurado de confirmação, **quando** a verificação de pendências for executada, **então** um alerta persistente é gerado e a requisição é sinalizada como `RECEBIMENTO_PENDENTE`.

### Evidências e testes

`AtendimentoHospitalarAceitacaoTest`: os dois cenários obrigatórios, prazo exato sem atraso, atraso de um segundo, resolução, quantidade parcial, recebimento inválido, duplicidades, permissões, não conformidade e vencimento posterior. `AtendimentoHospitalarServiceTest`: prazo configurado e validações. `HistoricoPostgresIT`: duas requisições disputando a bolsa e confirmações simultâneas. Ver [BDD](../docs/bdd/rastreabilidade-recebimento.feature) e [homologação](homologacao.md).

### Limitações reais

Reserva automática da US03 permanece pendente. Não há logística de transporte; o momento registrado é o da atestação da entrega. Fluxo hospitalar desta entrega cobre hemácias. Alertas são locais, sem e-mail. Não conformidades não acionam reposição automática. Contexto hospitalar é declarado por cabeçalho, seguindo o controle acadêmico existente, sem autenticação robusta. Protótipo não homologado para uso hospitalar real.

## Uso de IA

Implementação, testes, documentação e materiais preparados com apoio do Codex. A equipe deve revisar, compreender e declarar o uso conforme as regras da disciplina.
