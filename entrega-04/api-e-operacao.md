# API e operação — Entrega 04

Preserva Java 17/Spring Boot 3, PostgreSQL no perfil `prod`, H2 local e o mecanismo didático de acesso. Apenas dados sintéticos.

## API

| Método e caminho | Perfil / contexto | Corpo e resultado |
|---|---|---|
| `GET /bolsas/{id}/historico` | gestor | Eventos crescentes por momento UTC e ID; bolsa inexistente: 404 |
| `POST /requisicoes/{id}/entregas` | gestor | `{"bolsaId":1,"entregaRealizada":true}`; 201 com entrega efetiva atestada |
| `GET /requisicoes/{id}/entregas` | gestor ou hospital correspondente | Bolsa, requisição, prazo, condição, origens e datas |
| `POST /requisicoes/{id}/entregas/{entregaId}/recebimento` | hospital correspondente | `{"condicao":"INTEGRA","observacao":"Embalagem íntegra"}` |
| `POST /requisicoes/{id}/entregas/{entregaId}/utilizacao` | hospital correspondente | Sem corpo; uso registrado pelo servidor |
| `GET /requisicoes/pendencias` | gestor ou operador de logística | Executa verificação e lista entregas atrasadas sem confirmação |
| `GET /requisicoes` | gestor ou hospital | Hospital deve enviar `X-Hospital-Id`; listagem filtrada |
| `POST /requisicoes` | hospital | `hospitalId` do corpo deve coincidir com `X-Hospital-Id` |

Todos os caminhos exigem `X-Perfil`. As operações hospitalares exigem também `X-Hospital-Id`. Perfil ausente/incorreto ou hospital diferente: 403; vínculo inexistente: 404; duplicidade/transição inválida: 409; dados inválidos: 400; escrita no histórico: 405. Não são aceitos momento, status ou origem de entrada nos DTOs de entrega/recebimento. Esses valores são controlados pelo servidor.

## Estados e transações

Bolsa: `DISPONIVEL → ENTREGUE → RECEBIDA → UTILIZADA`. Recebimento não conforme: `ENTREGUE → BLOQUEADA`. O descarte automático existente continua `DISPONIVEL → DESCARTADO`. Entrega, recebimento e uso exigem validade posterior à data atual; não conforme pode registrar a chegada de bolsa já vencida.

Requisição inicia `SOLICITADA`. Uma entrega para quantidade 1: `ENTREGUE → RECEBIDA → UTILIZADA`. Para várias bolsas, estados `PARCIALMENTE_ENTREGUE`, `PARCIALMENTE_RECEBIDA` e `PARCIALMENTE_UTILIZADA` evitam declarar atendimento total prematuramente. Pendência ativa tem prioridade sobre não conformidade, que tem prioridade sobre os demais estados. A situação de cada bolsa continua disponível nas entregas.

Ordem de travas das operações hospitalares: requisição → bolsa. O verificador de pendências trava requisições em ordem de ID e reavalia após obter a trava. Restrição única por bolsa evita associação duplicada. Eventos são inseridos dentro da transação obrigatória da operação. Não há transação independente de auditoria que permita status alterado sem evento.

## Prazo e alertas

- `recebimento.prazo=${RECEBIMENTO_PRAZO:PT24H}`: duração ISO-8601 positiva; exemplos `PT30M`, `PT24H`.
- `recebimento.verificacao-ms=${RECEBIMENTO_VERIFICACAO_MS:60000}`: intervalo do verificador.
- Prazo calculado e persistido no registro da entrega. Alterar configuração afeta somente novas entregas.
- O instante exato do prazo não constitui atraso; somente um instante posterior gera alerta.
- `alertaEm` persiste na entrega, e `CONFIRMACAO_ATRASADA` no histórico. Não é duplicado ao reexecutar. A consulta lista apenas alertas sem recebimento confirmado.
- Novos eventos e entregas usam UTC. Datas da US01/US07 preservam o contrato anterior, com data local do servidor; no container e US10, UTC. O navegador apresenta instantes no fuso local.

## Banco, atualização e Render

O Hibernate mantém a estratégia anterior `ddl-auto=update`. A inicialização, após o EntityManager, amplia o CHECK de status preservando dados. Em PostgreSQL, a troca do CHECK e a instalação do gatilho são transacionais; erro impede subir a aplicação. Em H2 local há evolução equivalente do CHECK. Requisições anteriores recebem o estado inicial `SOLICITADA` por default; isso não cria entregas nem eventos.

O usuário PostgreSQL de implantação precisa ser proprietário das tabelas e poder criar função/gatilho. A função bloqueia UPDATE/DELETE/TRUNCATE inclusive via SQL. O administrador do banco pode remover proteções: não há promessa de resistência a administração maliciosa. Em H2, a proteção é de API/JPA; a garantia SQL é validada no PostgreSQL.

Dockerfile e variáveis do Render continuam compatíveis, incluindo `PORT`, datasource e Actuator. Não foi disparado deploy de produção. Antes do merge, revisar backup do banco e permissões DDL com a equipe. O GitHub Actions passa a incluir aceitação, rollback, imutabilidade SQL e concorrência em PostgreSQL 16 antes do container/publicação.

## Reprodução dos testes PostgreSQL

Use um banco exclusivo de homologação (não o banco de produção). Defina `HEMOCHAIN_TEST_DB_URL`, `HEMOCHAIN_TEST_DB_USER`, `HEMOCHAIN_TEST_DB_PASSWORD` e `SPRING_PROFILES_ACTIVE=postgres-test` no ambiente. Execute:

```text
./mvnw.cmd -B -Ppostgres-it -Dtest=AtendimentoHospitalarAceitacaoTest,RastreabilidadeTransacaoTest verify
```

Os testes inserem dados sintéticos e tentam operações bloqueadas no histórico. Não use uma base com dados reais. Suíte padrão: `./mvnw.cmd -B verify`. O Docker também executa a suíte padrão com JDK 17.
