# Relatório de homologação — Entrega 04

Execução local em 10/11/2026; data acadêmica da entrega: 09/11. Este relatório descreve um **protótipo funcional acadêmico**, sem homologação para uso hospitalar real.

## CONCLUÍDO — implementado e validado

| Item | Resultado e evidência |
|---|---|
| Auditoria inicial | Cópia íntegra existente, 21 commits atrás; fast-forward para `924d580`; branch específica; alteração preexistente preservada. [Auditoria](auditoria.md) |
| US09 | Eventos automáticos, bolsa identificada, tipo, UTC, origem e detalhes; cronologia, leitura restrita ao gestor, histórico integrado à tela |
| US09 — imutabilidade | API sem PUT/DELETE; callbacks JPA e repositório sem mutação; PostgreSQL bloqueia UPDATE/DELETE/TRUNCATE; FK preserva bolsa referenciada |
| US09 — legado | Histórico vazio para operações anteriores; nenhum evento retroativo inventado |
| US09 — transações | Falha induzida no evento reverte cadastro, entrega, recebimento e uso |
| US10 | Bolsa/requisição associadas por entrega ocorrida atestada, confirmação e condição, status total/parcial, uso, consulta e validação de hospital |
| US10 — negativos | Incompatível, vencida, já entregue, excesso de quantidade, duplicidade, uso precoce, não conformidade, hospital diferente e contexto ausente bloqueados |
| US10 — pendência | Prazo configurável persistido, limite exato e ultrapassagem testados, alerta único e evento, status pendente e resolução após recebimento tardio |
| Integração anterior | Estoque, validade/descarte, requisições, compatibilidade ABO/Rh/FEFO e perfis preservados; correções justificadas no tracker |
| Interface real | Cadastro → requisição → FEFO → atestação → recebimento → uso → histórico; segunda entrega com alerta após prazo real; zero erros JavaScript |
| PostgreSQL | Aceitação/rollback, SQL direto, FK, duas requisições concorrentes, confirmação concorrente e migração de CHECK em schema isolado |
| Build/Java 17 | Dockerfile executa Maven verify com Java 17 e produz imagem de runtime não root |
| Persistência e health | Compose down sem -v seguido de up --wait; API e PostgreSQL saudáveis; quatro eventos e entrega idênticos, bolsa UTILIZADA preservada |
| Documentação | POST-IT, BDD, API/operação, tracker e minutas de issues/PR, dois roteiros de screencast e roteiro da apresentação |

## Resultados efetivamente executados

| Execução | Ambiente | Resultado |
|---|---|---|
| Testes afetados durante implementação | JVM local Java 21, compilação release 17, H2 | 19 testes sem falha na rodada registrada antes da ampliação de rollback |
| `./mvnw.cmd -B verify` | JVM local Java 21, release 17, H2 | 82 testes, 0 falhas/erros/ignorados, antes da inclusão do teste adicional de rollback de recebimento/uso |
| Build Docker final `mvn -B verify` | Linux, Maven, **Java 17**, H2 | 83 testes; resultado final no resumo de evidências |
| `mvn -B -Ppostgres-it -Dtest=AtendimentoHospitalarAceitacaoTest,RastreabilidadeTransacaoTest verify` | PostgreSQL 16 real, JVM local | 17 testes de aceitação/rollback + 4 testes Failsafe de integridade/concorrência/migração, 0 falhas/erros/ignorados |
| `node scripts/entrega04-ui.cjs` | Edge headless/Playwright, Compose/PostgreSQL | APROVADO; capturas e `resultado-ui.json` |

Os testes Gherkin são especificações mapeadas aos testes JUnit/MockMvc, seguindo o padrão anterior; não há runner Cucumber. A suíte padrão preserva os testes antigos. O teste de FK em PostgreSQL registra um erro SQL esperado dentro de `assertThrows`; isso comprova a rejeição, não uma falha da suíte.

Logs brutos ficam localmente em `.git/entrega04-test-afetados.log`, `.git/entrega04-verify.log`, `.git/entrega04-postgres.log`, `.git/entrega04-docker-build.log`, `.git/entrega04-compose.log` e `.git/entrega04-ui.log`. O resumo versionado registra contagens, comandos e resultados sem credenciais.

## Homologação operacional e integridade

O ambiente usa projeto Compose isolado `hemochain-entrega04`, API na porta 18084 e PostgreSQL 16 na porta local 15434. Os testes PostgreSQL usam o banco `hemochain`; a interface usa `hemochain_ui` para separar relógio controlado dos testes e apresentação. Somente dados sintéticos.

A demonstração reduz o prazo para `PT5S`, com verificação automática espaçada e verificação explícita na tela. Isso é configuração exclusiva de homologação; o padrão versionado permanece `PT24H` e verificação a cada 60 segundos.

Conferir o resumo final de [evidências](evidencias/README.md), incluindo health, persistência após reinicialização, Git e revisão de segredos. A imagem e o Compose não disparam deploy no Render.

## PENDENTE — equipe, autorização ou publicação externa

- Autorizar publicação da branch, das issues e do PR draft; minutas estão completas. Sem autorização, permanecem locais.
- Executar/conferir os checks remotos do GitHub Actions para o HEAD publicado. O workflow foi preparado, mas execução local não equivale a execução remota.
- Revisão da equipe e decisão de merge. **Não foi autorizado nem executado merge na main ou deploy de produção.**
- Gravar os dois screencasts com áudio ou legendas, publicar no YouTube pela conta da equipe e inserir os links reais no README. Roteiros estão prontos; não há vídeo YouTube desta entrega.
- Ensaiar e apresentar o produto, confirmar divisão entre integrantes e declarar o apoio de IA conforme a disciplina.
- Validar o novo fluxo no Render depois de eventual merge/deploy autorizado; foi preservado o contrato do deploy, sem alegar homologação desta versão em produção.

## BLOQUEADO — impedimentos técnicos

Nenhum impedimento técnico local remanescente identificado na homologação registrada. Docker inicialmente indisponível foi iniciado e os testes PostgreSQL foram executados. Publicações externas sem autorização são pendências de equipe, não falhas mascaradas.

## Checklist da disciplina

| Requisito | Situação real |
|---|---|
| Verificação local/remota, preservação e atualização segura | CONCLUÍDO |
| Auditoria de README, histórias, código/testes, Docker, CI, BDD e histórico | CONCLUÍDO |
| US09 funcional com PostgreSQL e interface | CONCLUÍDO |
| Dois BDD obrigatórios US09 | CONCLUÍDO — aceitação + SQL PostgreSQL |
| US10 funcional com permissão hospitalar declarada e interface | CONCLUÍDO |
| Dois BDD obrigatórios US10 | CONCLUÍDO — aceitação + demonstração real de atraso |
| Integração ponta a ponta com dados sintéticos | CONCLUÍDO |
| Reserva automática da US03 | PENDENTE anterior; fora do escopo desta entrega |
| US04, US05 e US06 novas | Não implementadas nesta entrega, conforme escopo |
| Testes unitários, integração, aceitação e regressão | CONCLUÍDO |
| Build Maven, Docker/Compose e healthchecks | CONCLUÍDO, ver evidências |
| Persistência após reinicialização | CONCLUÍDO — histórico e entrega idênticos; `persistencia.json` |
| CI preparada para PostgreSQL e compatibilidade Render | CONCLUÍDO local; execução remota PENDENTE |
| Branch e commits, sem merge/deploy | CONCLUÍDO |
| Issues e PR | Preparados localmente; publicação PENDENTE de autorização |
| README e POST-IT da Entrega 04 — 09/11 | CONCLUÍDO |
| Tracker com causa, severidade, correção e testes | CONCLUÍDO local; GitHub PENDENTE |
| Captura do tracker | CONCLUÍDO — `bug-tracker.png`, tracker local identificado |
| Roteiro screencast sistema | CONCLUÍDO |
| Roteiro screencast código | CONCLUÍDO |
| Vídeos YouTube com áudio/legenda e links README | PENDENTE da equipe |
| Roteiro final do produto e limitações | CONCLUÍDO |
| Revisão de Git e credenciais | CONCLUÍDO — fsck, diff, links e padrões comuns aprovados; limites em `auditoria-final.json` |

A entrega técnica local e a entrega acadêmica publicada são etapas distintas. Os itens externos acima precisam ser encerrados pela equipe antes de declarar todos os requisitos da disciplina concluídos.
