# Plano incremental para 1 de dezembro de 2026

O objetivo de 01/12 é demonstrar uma fatia vertical com domínio protegido, dependências dirigidas para dentro, especificação anterior ao código e RED/GREEN verificável. Migrar os seis contextos integralmente em uma semana ampliaria o risco sem necessidade acadêmica. O sistema permanece um monolito, com um processo Spring Boot e PostgreSQL compartilhado. Nenhum item pendente abaixo está implicitamente autorizado por sua inclusão no plano.

**Situação em 24/11:** R00 e R01 executados; especificações e planejamento produzidos. R02 em diante aguardam aprovação quando envolvem estrutura, banco, contratos ou funcionalidades novas. Estimativas são de esforço de engenharia, não orçamento aprovado nem compromisso atribuído a integrantes do squad.

## R00 Baseline, cobertura reproduzível e evidências

- **Prioridade/natureza:** P0; instrumentação nova de qualidade, concluída.
- **Problema/evidência:** `pom.xml:77` tinha somente o plugin Spring Boot no build principal; `.github/workflows/ci-cd.yml:34` publicava testes sem cobertura. Referências da estrutura antiga estão no commit inicial `3c3e506`.
- **Atual → proposta executada:** execução Maven sem JaCoCo → JaCoCo 0.8.14 em `verify`, relatórios separados para Surefire e Failsafe PostgreSQL; CI coleta ambos. Arquivos: `pom.xml`, `.github/workflows/ci-cd.yml`, `scripts/auditoria-metricas.py`.
- **Justificativa/risco:** medir antes de mudar; risco baixo, apenas instrumentação e publicação de artefatos, sem alterar seleção dos testes. Relatórios não são um gate percentual automático.
- **Testes/aceite:** baseline preservada; suíte completa verde; perfis PostgreSQL e Docker Java 17 executados; XMLs e comandos rastreáveis. Não somar percentuais de execuções diferentes.

## R01 Invariantes locais do recebimento e uso

- **Prioridade/natureza:** P0; correção de regra já existente, concluída com SDD-01.
- **Problema/evidência:** `src/main/java/com/hemochain/entity/EntregaHospitalar.java:36` e `:42` no commit inicial sobrescreviam fatos sem guardas; `src/main/java/com/hemochain/service/AtendimentoHospitalarService.java:70`, `:74` e `:96` protegiam exclusivamente o caminho do service.
- **Atual → executada:** atribuições livres → entidade rejeita repetição, condição ausente, não conformidade sem observação, uso sem recebimento íntegro e instante nulo, antes de mutar. Arquivos: `src/main/java/com/hemochain/entity/EntregaHospitalar.java` e novo `src/test/java/com/hemochain/entity/EntregaHospitalarTest.java`.
- **Justificativa/risco:** a invariante acompanha o objeto em qualquer chamador. Risco baixo: service já impõe o comportamento HTTP e fornece os instantes; transações e contratos foram preservados. Duplicação temporária de guardas é explícita até R02; não declarar Clean Architecture concluída.
- **Testes/aceite:** dois REDs de asserção, GREENs, valores preservados após recusa, testes HTTP/rollback e PostgreSQL verdes. Métodos `receber` e `utilizar` com todos os branches exercitados. Ver relatório e evidências.

## R02 Introduzir uma fronteira de aplicação e persistência em uma fatia

- **Prioridade/natureza:** P0; refatoração estrutural, aguarda aprovação. Dependência: R01; esforço estimado 6–8 h, em conjunto com R03.
- **Problema/evidência:** `src/main/java/com/hemochain/controller/BolsaController.java:65` recebe/retorna a entidade JPA; `src/main/java/com/hemochain/entity/RequisicaoHospitalar.java:30` contém `hospitalId` transitório para o formulário; `src/main/java/com/hemochain/service/BolsaService.java:5` importa HTTP; `src/main/java/com/hemochain/repository/BolsaRepository.java:15` é Spring Data.
- **Atual:** pacotes técnicos globais; JSON, entidade de negócio e persistência compartilham o mesmo tipo; services dependem do mecanismo de dados.
- **Proposta:** começar por `estoque/negocio/dominio/Bolsa` e `alocacao/negocio/dominio/CompatibilidadeHemacias`, sem frameworks; portas `estoque/negocio/aplicacao/portas/RepositorioBolsas` e resultados/comandos de aplicação; adapters em `estoque/dados` usando a persistência atual e mapper explícito. DTOs de `estoque/apresentacao` preservam o JSON vigente. A configuração Spring monta dependências. Não renomear todos os pacotes de uma vez, nem expor objetos de domínio na API.
- **Justificativa:** comprovar inversão da dependência com uma regra de negócio real, sem criar serviços distribuídos ou interfaces para todo método sem necessidade.
- **Risco:** médio/alto: mapeamento de ID, datas, enums e descarte; dirty checking/transações e serialização podem mudar.
- **Testes/aceite:** caracterização de campos JSON e códigos HTTP; mapper ida/volta incluindo bolsa descartada e precisão temporal; rollback existente; domínio sem Spring/JPA/Jackson; adapter implementa a porta. Tabelas e contratos preservados nesta etapa. Não remover setters do modelo JPA antes de separar entradas e reidratação.

## R03 Tornar Bolsa uma raiz que protege o ciclo de estoque

- **Prioridade/natureza:** P0; refatoração de regras existentes, aguarda aprovação. SDD-03; esforço incluído no lote R02/R03.
- **Problema/evidência:** `src/main/java/com/hemochain/entity/BolsaHemocomponente.java:64` a `:76` permitem escrita arbitrária; `:82` descarta qualquer estado exceto o já descartado. `src/main/java/com/hemochain/service/BolsaService.java:68` e `src/main/java/com/hemochain/service/ValidadeService.java:75` concentram as proteções.
- **Atual → proposta:** setters → criação válida, `corrigirCadastro`, consulta de elegibilidade e descarte por validade na raiz pura. Objetos de valor candidatos: código normalizado, volume e período coleta/validade. Criá-los somente se encapsularem invariantes; tipo sanguíneo já é enum imutável e não precisa de uma classe duplicada por estética.
- **Responsabilidades:** domínio valida dados, estado e transição; política de validade recebe prazos configurados; aplicação obtém data, consulta unicidade, bloqueia, salva e grava evento; adapter garante unique no código. Não transferir consultas/transações para a entidade.
- **Risco:** médio/alto; preservar descarte em validade igual a hoje, edição de cadastro e alertas recalculados.
- **Testes/aceite:** fronteiras -1/0/+1; cada estado proibido; repetição de descarte mantém dados; volume/código/coleta; integração alerta único e auditoria atômica. Nenhuma mudança dos prazos didáticos ou da data operacional sem decisão expressa.

## R04 Reserva exclusiva de uma bolsa por FEFO

- **Prioridade/natureza:** P0 para a história âncora; **funcionalidade nova**, aguarda aprovação do recorte e das decisões SDD-02. Estimativa 10–14 h após R02/R03, incluindo integração.
- **Problema/evidência:** `src/main/java/com/hemochain/service/BolsaService.java:51` apenas lista; `src/main/java/com/hemochain/repository/BolsaRepository.java:22` ordena sem reserva; `src/main/java/com/hemochain/entity/StatusBolsa.java:3` não contém RESERVADA; `src/main/java/com/hemochain/service/AtendimentoHospitalarService.java:38` registra entrega manual. `README.md:88` no HEAD inicial mantém T4 pendente.
- **Atual → proposta:** sugestão e entrega manual continuam existentes; acrescentar `alocacao/negocio/aplicacao/ReservarBolsa`, política pura, vínculo persistente exclusivo e adapter FEFO transacional. A API, caso aprovada, será aditiva e terá contrato definido antes do código. Campos/estados/constraints novos exigem migração H2/PostgreSQL e integração com entrega.
- **Justificativa:** tornar a história âncora observável, sem apresentar consulta como alocação exclusiva.
- **Risco:** alto; concorrência, lock ordering, deadlock, atualização de enums/CHECKs, idempotência e reserva órfã. Seleção deve manter a semântica FEFO acordada; não introduzir `SKIP LOCKED` silenciosamente se isso mudar a escolha da primeira bolsa.
- **Decisões antes do GREEN:** escopo uma bolsa/comando; chave de idempotência; consumo/liberação; atendimento parcial; resultado sem estoque; autorização e JSON/erros; transição de entrega reservada versus manual. Não inventar notificação ou estado de espera porque aparece na história extensa.
- **Testes/aceite:** sete critérios SDD-02; matriz 8×8 mantida; empate por ID; vencimento hoje; duas requisições/uma bolsa com exatamente um vencedor em PostgreSQL; evento e vínculo atômicos; repetição de comando; migração de schema legado preserva dados. Entrega manual atual segue verde.

## R05 Isolar a política de status da requisição

- **Prioridade/natureza:** P1; refatoração existente, aguarda aprovação; SDD-04, 3–4 h depois do recorte P0.
- **Problema/evidência:** `src/main/java/com/hemochain/service/AtendimentoHospitalarService.java:135` a `:151` calcula todos os estados e escreve `RequisicaoHospitalar.setStatus` (`:64`).
- **Atual → proposta:** cálculo dentro do service de Expedição → `requisicao/negocio/dominio/SituacaoAtendimento` recebe resumo imutável por contrato de aplicação. A raiz Requisição controla quantidade e alteração de situação. Não importar `EntregaHospitalarJpa` entre domínios.
- **Justificativa/risco:** separar regra de precedência da busca dos dados; risco médio de status total/parcial indevido ou de inversão da prioridade atraso/não conformidade.
- **Testes/aceite:** tabela cobrindo todos os ramos, combinações e resolução de atraso; aceitação atual preservada. Quantidade positiva e excesso caracterizados. Fila prioritária e parceiros são funcionalidades novas, não parte dessa extração.

## R06 Porta de rastreabilidade e tempo explícito

- **Prioridade/natureza:** P1; refatoração estrutural, SDD-05, 3–5 h.
- **Problema/evidência:** `src/main/java/com/hemochain/service/RastreabilidadeService.java:15` usa EntityManager; `src/main/java/com/hemochain/entity/EventoBolsa.java:30` usa `Instant.now`; `src/main/java/com/hemochain/service/BolsaService.java:40` e `src/main/java/com/hemochain/service/ValidadeService.java:80` usam relógios globais, enquanto Atendimento usa Clock (`:171`).
- **Atual → proposta:** gravação diretamente em JPA no service e relógios distintos → porta transacional de eventos e relógio explícito na aplicação; fato imutável contendo IDs, sem relação JPA entre domínios. Adapter SQL conserva a proteção atual. Preservar a precisão de microssegundos.
- **Justificativa/risco:** teste determinístico e dependência dirigida para dentro; risco alto se auditoria for desacoplada da transação. Não usar publicação assíncrona como substituição ingênua de `MANDATORY`.
- **Testes/aceite:** rollback de cadastro/entrega/recebimento/uso; histórico legado vazio; imutabilidade SQL; fronteira exata do prazo e evento único. Unificar fuso/data de estoque pode mudar comportamento; registrar decisão antes de executar essa parte.

## R07 Política de acesso e autenticação

- **Prioridade/natureza:** P1 acadêmico; **P0 antes de qualquer uso com dados reais**. Refatoração de autorização + autenticação nova, ambas dependentes de aprovação. Não incluídas na implementação mínima de 01/12.
- **Problema/evidência:** `src/main/java/com/hemochain/controller/HospitalController.java:25` documenta confiança em X-Perfil; `src/main/java/com/hemochain/controller/RequisicaoController.java:51` verifica hospital no controller; `src/main/java/com/hemochain/controller/UsuarioController.java:23` recebe cadastro com perfil; `src/main/java/com/hemochain/service/UsuarioService.java:36` não emite sessão/token; `src/main/java/com/hemochain/entity/Usuario.java:27` não tem unique para email.
- **Atual → proposta:** contexto declarado pelo cliente → primeiro centralizar regras em aplicação mantendo contratos; em lote próprio introduzir identidade autenticada e vínculo usuário/hospital, política de criação de perfis, DTOs, normalização de email independente de locale e unicidade concorrente no banco. Não prometer segurança de produção ao centralizar cabeçalhos.
- **Justificativa/risco:** evitar contornar autorização chamando aplicação diretamente; risco alto de login/cadastro e permissões quebrarem. Mudanças de schema/autenticação são novas e não autorizadas por esta auditoria.
- **Testes/aceite:** identidade verificável e hospital proprietário em todos os casos de uso; cabeçalho forjado não concede acesso na versão de autenticação nova; duplicidade concorrente de email; contratos/migração aprovados. Os testes atuais apenas comprovam a política de cabeçalhos didáticos.

## R08 Guardar as fronteiras com ArchUnit

- **Prioridade/natureza:** P0 na primeira fatia estrutural; ferramenta/testes novos, propostos; 2–3 h junto de R02.
- **Problema/evidência:** não há ArchUnit no `pom.xml`; os imports reais em `entity`, `service` e `repository` mostram que regras estritas globais falhariam hoje.
- **Atual → proposta:** convenção verbal → teste `ArquiteturaModularTest` com classes concretas do módulo introduzido: domínio sem Spring/JPA/Jackson/Web; apresentação sem acesso direto a dados; dados implementando portas internas; contratos públicos para cruzar módulos; nenhum ciclo entre os contextos migrados. A configuração pode depender das bordas para montar o grafo.
- **Justificativa/risco:** impedir regressão estrutural; risco baixo de runtime, médio de regras irreais/vazias. Usar importação de produção, falhar em seleção vazia e documentar o escopo legado. Não adicionar um teste “verde” que exclui todos os pacotes atuais e dizer que o sistema inteiro é Clean.
- **Testes/aceite:** introduzir uma violação temporária em fixture e comprovar a detecção; regras verdes no núcleo real. Não desabilitar testes ou congelar toda a dívida sem revisão. Versão compatível com Java 17 e JUnit 5 deve ser fixada ao executar o lote.

## R09 Evolução de schema e concorrência operacional

- **Prioridade/natureza:** P1 para adoção completa de migrações versionadas; alteração de banco, aguarda aprovação. As migrações mínimas da reserva são dependências obrigatórias de R04.
- **Problema/evidência:** `src/main/resources/application.properties:12` usa ddl-auto=update, herdado por prod; `src/main/java/com/hemochain/config/ProtecaoHistoricoPostgres.java:28` remove/recria CHECK no startup; `src/main/java/com/hemochain/config/EvolucaoStatusBolsaH2.java:22` pesquisa CHECK sem filtrar schema na seleção inicial. Os dois arquivos H2 localmente alterados pelo squad estão preservados e fora das alterações desta auditoria.
- **Atual → proposta:** DDL ad hoc no startup → migrações versionadas com ensaio de atualização e estratégia de recuperação sem apagar dados; app passa a validar schema após a migração. Escolher Flyway/Liquibase em decisão própria, não adicionar ambos.
- **Justificativa/risco:** evolução auditável e permissões separadas de runtime; risco alto de instalação legada, enums e proteção append-only.
- **Testes/aceite:** schema novo e legado com linhas preservadas em H2/PostgreSQL; falha DDL não deixa proteção pela metade; reserva exige unique e lock efetivos; nenhum banco do squad usado nos testes.

## R10 Planejamento U2, arquitetura e apresentação

- **Prioridade/natureza:** P0; documentação exigida ainda ausente, 3–4 h após localizar as fontes e obter os dados da equipe.
- **Problema/evidência:** `docs/arquitetura/` não existe na árvore inspecionada nem consta do histórico local consultado; nenhuma planilha de orçamento/ADR foi localizada. `User Stories 2° Entrega.md` possui dois bytes e nenhum conteúdo de planejamento.
- **Atual → proposta:** recuperar U2 e ADR da Parte 3; reconciliar com o monolito modular e com os resultados reais. Criar os artefatos faltantes somente com dados verificáveis, distinguindo propostas de decisões aprovadas.
- **Arquivos a revisar/criar, ainda propostos:** README; planejamento U2 original; `docs/arquitetura/` com contexto/componentes/deploy, diagrama de sequência da reserva e estados; ADR de monolito e fronteiras, transação/concorrência e migrações; planilha de orçamento com unidade, premissas, data de consulta e fórmulas; roteiros de apresentação. Os nomes finais dependem do modelo exigido pela disciplina.
- **Risco:** acadêmico alto: anexos citados, porém inexistentes, e diferenças entre backlog e implementação.
- **Aceite:** todos os links locais resolvem; nenhum arquivo vazio conta como evidência; orçamento não inventa valores; estados propostos são identificados; apresentação mostra 84 testes de baseline e o resultado final desta auditoria como momentos distintos; vídeo/poster não chama consulta de reserva. Os MP4/SRT locais em `Entrega 04/` não comprovam publicação em YouTube.

## R11 Robustez e lacunas adicionais

- **Prioridade/natureza:** P1 para testes/configuração; P2 para otimização posterior de indicadores; aguarda definição de escopo.
- **Problema/evidência:** `src/test/java/com/hemochain/service/RequisicaoServiceTest.java:72`/`src/test/java/com/hemochain/service/HospitalServiceTest.java:34` usam contagem mínima em vez de identidade exata; `src/test/java/com/hemochain/service/ValidadeServiceTest.java:24` injeta configuração por reflexão; `src/test/java/com/hemochain/service/RequisicaoIndicadoresServiceTest.java:28` constrói service com repository nulo e não cobre seu método público; `src/main/java/com/hemochain/service/DadosSinteticosService.java:53` não é exercitado pela baseline. `src/main/java/com/hemochain/repository/BolsaRepository.java:22` depende da verificação prévia de validade e não filtra por data na própria consulta.
- **Atual → proposta:** dados/relógios e asserções implícitas → fixtures isoladas e Clock coerente; testar cálculo público, filtro, threads fora de 1–64 e erros; testar ou desabilitar por perfil dedicado o gerador em decisão aprovada. Para o novo adapter de reserva, filtrar validade na seleção e revalidar sob lock; mudar a consulta pública atual exige caracterização prévia. Paginação e métricas de desempenho ficam em P2.
- **Justificativa/risco:** testes mais discriminantes, sem confundir volume de testes com regras comprovadas. Risco baixo/médio; agendadores habilitados e H2 compartilhado merecem isolamento de teste, não remoção de verificações.
- **Aceite:** mesmas funcionalidades, asserts de identidade/estado e fronteiras; seleção FEFO com empate, vencimento hoje e concorrência; zero testes suprimidos. Não perseguir cobertura de getters para cumprir metas.

## Sequência de entrega e contingência

| Data | Trabalho e dependência | Entregável verificável |
|---|---|---|
| 24/11 | R00/R01 e esta auditoria | Baseline, XMLs, dois ciclos TDD, SDD e plano concretos |
| 25/11 | Revisão do squad e aprovação dos lotes; localizar U2/Parte 3; iniciar R02/R03 | Escopo aprovado, testes de caracterização, núcleo de estoque com invariantes |
| 26/11 | Concluir fatia R02/R03 e R08 | Porta/adapters, DTOs preservando API, ArchUnit sobre código real |
| 27/11 | RED/GREEN do núcleo R04, após decisões | Seleção/elegibilidade/reserva unitárias com matriz e FEFO |
| 28–29/11 | Integração de R04 e migrações mínimas; regressão | Concorrência PostgreSQL e rollback; API somente se contrato aprovado |
| 30/11 | Congelar escopo, R10, suíte/Docker e revisão | Evidências reproduzíveis, diagramas coerentes, apresentação ensaiada |
| 01/12 | Demonstração da parte implementada | Reserva completa se integração aprovada e verde; caso contrário núcleo concluído com limites declarados |

A fatia P0 estrutural/reserva demanda aproximadamente 21–29 h após a aprovação, mais documentação/revisão. Não há disponibilidade do squad informada; a estimativa precisa ser confrontada com a capacidade real. Se a aprovação, U2 ou contrato atrasarem, manter R00/R01 como entrega comprovada e reduzir R04 à especificação/núcleo isolado, sem mascarar a ausência da exclusividade persistente. R05/R06/R07/R09 completos ficam após o recorte de 01/12, salvo aumento explícito de capacidade e escopo.

## Metas de cobertura justificadas

| Escopo | Meta para o recorte aprovado | Razão |
|---|---|---|
| Matriz ABO/Rh vigente | 64 pares com oráculo independente; 100% dos branches da política | Compatibilidade é central; já existe matriz completa, preservá-la |
| Recebimento/uso local | 100% linhas/branches dos dois métodos | Guardas pequenas e todos os caminhos relevantes são enumeráveis |
| Novo núcleo Bolsa/Reserva | ≥95% linhas, ≥90% branches, 100% das transições especificadas | Cobrir caminhos de estado e exceções; porcentagem não substitui cenários |
| Aplicação crítica de entrega/reserva | ≥90% linhas e ≥85% branches no recorte | Lock, rollback, quantidade, autorização e erros são prioritários |
| Bundle Java na suíte padrão | ≥85% linhas, ≥80% branches, ≥90% métodos como meta de 01/12 | Incremento moderado sobre 84,63%/75,82%/89,17%; ainda não atingido por esta correção |
| PostgreSQL | Todos os testes SQL/concorrência/rollback verdes; relatório separado | Cobertura H2 não valida semântica de lock/trigger PostgreSQL |

Não configurar um gate de 100% global: inicialização, DTOs e gerador têm risco diferente do domínio. O primeiro gate deve proteger o núcleo real introduzido em R02–R04 e um piso global justificado; seus limiares devem acompanhar denominadores e cenários, sem excluir produção para elevar números.

## Aprovação solicitada para a próxima fase

Revisar primeiro o lote **R02/R03/R08**, que cria domínio puro de Estoque, porta/adapters, DTOs compatíveis e ArchUnit. Aprovar **R04** separadamente, após fechar as decisões da SDD-02, pois acrescenta funcionalidade, persistência e potencial contrato de API. Aprovação desse plano não autoriza push, merge, deploy ou uso de dados reais.
