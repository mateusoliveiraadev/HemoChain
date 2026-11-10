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
| Documentação | POST-IT, BDD, API/operação, tracker vinculado às issues e ao PR, dois roteiros de screencast e roteiro da apresentação |

## Resultados efetivamente executados

| Execução | Ambiente | Resultado |
|---|---|---|
| Build Docker final `mvn -B verify` | Linux, Maven, **Java 17**, H2 | 83 testes; resultado final no resumo de evidências |
| `mvn -B -Ppostgres-it -Dtest=AtendimentoHospitalarAceitacaoTest,RastreabilidadeTransacaoTest verify` | PostgreSQL 16 real, JVM local | 17 testes de aceitação/rollback + 4 testes Failsafe de integridade/concorrência/migração, 0 falhas/erros/ignorados |
| `node scripts/entrega04-ui.cjs` | Edge headless/Playwright, Compose/PostgreSQL | APROVADO; capturas e `resultado-ui.json` |

Os testes Gherkin são especificações mapeadas aos testes JUnit/MockMvc, seguindo o padrão anterior; não há runner Cucumber. A suíte padrão preserva os testes antigos. O teste de FK em PostgreSQL registra um erro SQL esperado dentro de `assertThrows`; isso comprova a rejeição, não uma falha da suíte.

Os resumos versionados em [evidências](evidencias/README.md) registram comandos, contagens e resultados sem credenciais. Logs temporários e arquivos internos de execução não são versionados.

## Publicação e revisão no GitHub

- [Branch publicada](https://github.com/mateusoliveiraadev/HemoChain/tree/feature/entrega-04-rastreabilidade-recebimento), preservando os cinco commits originais até `9b379eb` e acrescentando commits de preparação da publicação.
- [PR #8](https://github.com/mateusoliveiraadev/HemoChain/pull/8): **Draft**, aberto, base `main`. Sem merge.
- Issues [#4](https://github.com/mateusoliveiraadev/HemoChain/issues/4), [#5](https://github.com/mateusoliveiraadev/HemoChain/issues/5), [#6](https://github.com/mateusoliveiraadev/HemoChain/issues/6) e [#7](https://github.com/mateusoliveiraadev/HemoChain/issues/7), abertas e associadas ao PR por `Closes`. Causa, severidade, reprodução, correção e testes disponíveis em cada issue.
- [Captura real do GitHub Issues](evidencias/github-issues.png) e [tracker detalhado](bug-tracker.md).
- Execuções remotas no commit `9b379eb`: [push aprovado](https://github.com/mateusoliveiraadev/HemoChain/actions/runs/42755213594) e [pull request aprovado](https://github.com/mateusoliveiraadev/HemoChain/actions/runs/42755395418). Maven: 83 testes; PostgreSQL: 17 + 4 testes; Docker/Compose e healthcheck aprovados. Publicação de imagem e deploy ignorados. Ver [evidência remota](evidencias/ci-github-actions.md) e [checks atuais do PR](https://github.com/mateusoliveiraadev/HemoChain/pull/8/checks).
- A `main` permaneceu em `924d580`. Os jobs de publicação de imagem e deploy são restritos a push na `main`; não foi iniciado deploy de produção.

Na preparação final, minutas de issues/PR já publicadas, resumos operacionais duplicados e seu gerador foram retirados da árvore atual. Documentação acadêmica, testes, evidências verificáveis, declarações de uso de IA e todo o histórico anterior foram preservados.

## Homologação operacional e integridade

O ambiente usa projeto Compose isolado `hemochain-entrega04`, API na porta 18084 e PostgreSQL 16 na porta local 15434. Os testes PostgreSQL usam o banco `hemochain`; a interface usa `hemochain_ui` para separar relógio controlado dos testes e apresentação. Somente dados sintéticos.

A demonstração reduz o prazo para `PT5S`, com verificação automática espaçada e verificação explícita na tela. Isso é configuração exclusiva de homologação; o padrão versionado permanece `PT24H` e verificação a cada 60 segundos.

Verificados health, persistência após reinicialização, integridade Git e padrões comuns de credenciais. Não há `.env` real versionado. A imagem preexistente da Entrega 03 mantém o hash registrado na auditoria e não integra os commits desta entrega. Essa revisão não substitui uma auditoria de segurança completa. A imagem e o Compose não disparam deploy no Render.

## PENDENTE — equipe

- Revisão da equipe e decisão de merge. **Não foi autorizado nem executado merge na main ou deploy de produção.**
- Gravar os dois screencasts com áudio ou legendas, publicar no YouTube pela conta da equipe e inserir os links reais no README. Roteiros estão prontos; não há vídeo YouTube desta entrega.
- Ensaiar e apresentar o produto, confirmar divisão entre integrantes e declarar o apoio de IA conforme a disciplina.
- Validar o novo fluxo no Render depois de eventual merge/deploy autorizado; foi preservado o contrato do deploy, sem alegar homologação desta versão em produção.

## BLOQUEADO — impedimentos técnicos

Nenhum impedimento técnico remanescente identificado. As execuções de push e PR passaram sem divergência funcional entre a homologação local e a CI. A preparação da publicação não exigiu alterações de código.

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
| CI preparada para PostgreSQL e compatibilidade Render | CONCLUÍDO — Maven, PostgreSQL e container aprovados remotamente; deploy não executado |
| Branch e commits, sem merge/deploy | CONCLUÍDO |
| Issues e PR | CONCLUÍDO — quatro issues publicadas e PR #8 Draft aberto |
| README e POST-IT da Entrega 04 — 09/11 | CONCLUÍDO |
| Tracker com causa, severidade, correção e testes | CONCLUÍDO — issues #4–#7 vinculadas ao PR |
| Captura do tracker | CONCLUÍDO — GitHub Issues e quadro detalhado local |
| Roteiro screencast sistema | CONCLUÍDO |
| Roteiro screencast código | CONCLUÍDO |
| Vídeos YouTube com áudio/legenda e links README | PENDENTE da equipe |
| Roteiro final do produto e limitações | CONCLUÍDO |
| Revisão de Git e credenciais | CONCLUÍDO — fsck, diff, links e padrões comuns; alteração preexistente preservada |

A entrega técnica local e a entrega acadêmica publicada são etapas distintas. Os itens externos acima precisam ser encerrados pela equipe antes de declarar todos os requisitos da disciplina concluídos.
