> **Registro histórico da integração local, anterior à autorização do túnel.** Em 27/11/2026 o acesso remoto foi autorizado e validado como Viewer. Para a situação atual e as pendências, consulte a [matriz final](matriz-final.md) e a [evidência remota](acesso-remoto.md). Os resultados locais e as decisões abaixo foram preservados como histórico.

# Integração local de observabilidade — 27/11/2026

## Resultado

A stack obrigatória foi integrada e validada localmente. O Quick Tunnel não foi iniciado. Não houve commit, push, merge, publicação ou deploy. O repositório configurado é [mateusoliveiraadev/HemoChain](https://github.com/mateusoliveiraadev/HemoChain); as alterações desta integração permanecem locais.

Foram aproveitados o patch e os dashboards do integrante, com adaptações pontuais. A composição da demonstração foi separada em `compose.observabilidade.yaml`, projeto `hemochain-observabilidade`, preservando o Compose de trabalho e evitando reutilizar o banco existente.

## Alterações executadas

- Dependências Actuator/Micrometer Prometheus, AOP, tracing OTel, exporter OTLP, JDBC e Loki4j; este último ajustado de 2.0.0 para 2.0.1.
- Perfil `observabilidade`, aspectos condicionados a ele, instrumentação JDBC desabilitada por padrão e habilitada somente na demonstração.
- Actuator em 8081 internamente, com health, info, metrics e Prometheus; tag `application=hemochain` e histograma HTTP.
- Duas métricas priorizadas: tentativas de registrar entrega e duração das tentativas. Descrições não afirmam commit. Nenhuma dependência nova foi acrescentada ao domínio.
- Logs operacionais de tentativa e processamento da entrega, com traceId/spanId no Loki; spans HTTP, de aplicação e JDBC no Tempo.
- Três datasources e três dashboards provisionados. Painéis de negócio separados e acumulados, mantendo resultados visíveis depois do lote limitado.
- Prometheus com sete jobs; cAdvisor ajustado de 0.49.1 para 0.55.1 por incompatibilidade comprovada com o image store overlayfs/containerd.
- Node Exporter na rede da VM Linux, escutando somente no gateway interno; ajuste do mount e do painel de disco aos dados efetivamente produzidos no Docker Desktop.
- Labels do cAdvisor limitadas a projeto/serviço; Prometheus conserva somente séries de containers desta demonstração.
- Banco PostgreSQL e cinco volumes nomeados isolados pelo projeto Compose. Portas da API, Grafana e Prometheus publicadas somente no loopback.
- Carga k6 com duração finita, teto de fluxos, leituras leves, erros controlados e rejeição de destino externo.
- Retenções curtas, rotação de logs e limites por container. Nenhuma configuração global de Docker/WSL foi alterada.
- Viewer local criado e confirmado sem privilégios administrativos. Sua criação pode ser feita manualmente pela interface; os scripts de criação do patch foram dispensados.
- README reproduzível, arquitetura e material para o PDF; evidências reais em `evidencias/`.

## Complexidade dispensada

Não foram implementados gauge de estoque, snapshots, caches, agendadores, listeners após commit, métricas opcionais adicionais como etapas independentes, exemplars, alertas, domínio próprio ou túnel nomeado. Não houve refatoração geral de DDD, autenticação ou schema.

O contador registra execuções do método por resultado, e o timer mede a duração correspondente. Uma transação revertida posteriormente não torna essas métricas falsas: elas não estão definidas como fatos persistidos. Rejeições anteriores à entrada no service não entram nessa contagem.

## Preservação e regressão

`evidencias/preservacao.json` compara hashes com a árvore inicial, incluindo alterações locais dos integrantes. Permaneceram idênticos à entrada desta integração: workflow CI/CD, Compose de trabalho, Dockerfile, bug tracker e os arquivos locais de evolução H2/EntregaHospitalar e seu teste. O conteúdo anterior do README foi preservado e o JaCoCo 0.8.14 continua configurado no POM.

Resultados reais registrados em `evidencias/testes.json`:

| Verificação | Resultado |
| --- | --- |
| Suíte Maven padrão | 100 testes, zero falhas/erros/ignorados; inclui os 98 existentes e dois novos |
| Build Docker com Java 17 | 100 testes aprovados; imagem produzida |
| PostgreSQL real isolado | 17 cenários de aceitação/rollback + 4 testes de integração, aprovados |
| Interface existente | Cadastro, requisição, FEFO, entrega, recebimento e uso aprovados; zero erros JavaScript |
| Perfil prod local | Health 200; Prometheus e erro simulado 404 |
| Carga curta de 1 minuto | 26/26 checks; 12 iterações; somente dois fluxos novos |
| Limite de dados | Bolsas/requisições/entregas passaram de 13/13/13 para 15/15/15 no teste curto |
| Bloqueio de destino | URL externa rejeitada antes de emitir HTTP |
| Telemetria indisponível | Container temporário separado iniciou com destinos Loki/OTLP inválidos; health UP e três leituras 200 |

O relatório JaCoCo da suíte padrão foi regenerado ao final: 731/862 linhas (**84,80%**) e 229/295 branches (**77,63%**). Os contadores estão em `evidencias/testes.json`; o HTML local está em `target/site/jacoco/index.html`. O denominador inclui o código novo de observabilidade.

Os três erros HTTP da carga curta foram esperados: duas recusas 409 e um erro 500 de demonstração. O threshold usado verifica os resultados esperados, inclusive esses erros.

A checagem de indisponibilidade foi curta e não comprova comportamento de longa duração nem ausência de perda de telemetria. A revisão automática bloqueou uma tentativa de parar Loki/Tempo temporariamente, sem executar a ação e sem informar outro motivo além do bloqueio por política. A verificação foi concluída de forma isolada, sem parar a stack validada.

## Evidências funcionais

Os sete targets ficaram UP. As 53 consultas PromQL dos dashboards retornaram séries e valores finitos. Health, info e metrics foram consultados internamente; a resposta Prometheus foi preservada em TXT. As famílias HTTP, JVM, processo e Hikari estão presentes.

Há logs reais de sucesso e erro no Loki, trace com 19 spans no Tempo e capturas da navegação pelo campo derivado **Abrir trace no Tempo**. O JSON do trace e o log correspondente estão preservados. Os JSONs de dashboards foram validados e as telas foram inspecionadas visualmente.

O Viewer local conseguiu listar dashboards (200), não acessou a administração de usuários (403) e possui `isGrafanaAdmin=false`. As capturas de negócio e infraestrutura também foram obtidas com esse usuário local. Nenhum acesso pela internet foi testado.

## Consumo observado

Três amostras, registradas em `evidencias/consumo.json`, com nove serviços em baixa carga:

- RAM total: **731,97–734,29 MiB**, aproximadamente 0,72 GiB.
- CPU somada: **4,16–22,33%**, na convenção Docker em que 100% corresponde a um núcleo. Não é percentual da máquina inteira.
- Cinco volumes: **51,49 MiB** no instante medido.
- Soma dos tamanhos das imagens em uso: **2,24 GiB**, sem descontar camadas compartilhadas. Isso não equivale ao incremento físico no disco e inclui a imagem PostgreSQL já existente.

As amostras não incluem overhead Docker/WSL, cache de build, outros projetos ou picos de compilação. A VM já estava configurada com aproximadamente 7,7 GiB; essa configuração não foi alterada. Os limites da stack obrigatória somam 3 GiB; carga e túnel adicionam seus limites individuais.

Prometheus: 3 dias/512 MB de blocos; Loki: 72h com retenção do Compactor; Tempo: 48h; logs Docker: 5 MB × 2 por container. As configurações foram aceitas na inicialização, mas não se afirma ter observado o expurgo após dias. WAL/head e banco podem ocupar espaço adicional. Reiniciar deliberadamente a carga cria outro lote limitado.

## Matriz acadêmica

| Etapa | Máximo oficial | Atendimento nesta integração | Evidência / pendência |
| --- | ---: | --- | --- |
| E1 — Instrumentação com Actuator | 0,75 | Comprovado localmente | Endpoint interno, tag e métricas preservados |
| E2 — Exporters de infraestrutura | 0,75 | Comprovado localmente | Targets UP e painéis da VM/containers |
| E3 — Stack via Docker Compose | 0,75 | Comprovado localmente | Comando único com arquivo Compose de demonstração e datasources provisionados |
| E4 — Dashboards no Grafana | 0,75 | Comprovado localmente | JSONs, 53 consultas com dados e capturas |
| E5 — Logs com Loki | 0,75 | Comprovado localmente | Logs filtráveis, sucesso e erro |
| E6 — Traces com Tempo e correlação | 0,75 | Comprovado localmente | Trace de 19 spans e clique real log → trace |
| E7 — Acesso remoto | 0,75 | Parcial; dependente de autorização e validação externa | Viewer local funciona; túnel não iniciado |
| E8 — Repositório e PDF | 0,75 | Parcial | Organização local e material prontos; publicação e PDF final pendentes |
| Adicional — Métricas de negócio | 1,00 | Comprovado localmente | Duas métricas descritas, exportadas e em painéis próprios |
| Total | **7,00** | Potencial após concluir pendências | Não se declara nota integral antecipada |

E1–E6 e o adicional correspondem a **5,50 pontos de critérios técnicos comprovados localmente**. A atribuição acadêmica depende do professor e da apresentação das evidências. E7 e E8 não recebem nota integral antes de acesso externo, publicação autorizada e PDF.

## Próximos passos autorizáveis

1. Autorizar especificamente a abertura do Quick Tunnel; testar em outra rede com Viewer e registrar URL/período.
2. Confirmar equipe/turma e montar o PDF com o diagrama, tabela e evidências datadas, seguindo `material-pdf.md`.
3. Revisar e autorizar commit/push dos arquivos e evidências. A branch e todas as alterações continuam locais.
4. Entregar PDF e link no Classroom; manter a máquina ligada e o ambiente disponível durante a correção.

As credenciais permanecem no arquivo local ignorado `.env.observabilidade` e devem ser fornecidas exclusivamente pelo canal autorizado da disciplina.
