# Especificações SDD para a entrega de 1 de dezembro de 2026

Documento produzido antes da alteração de `EntregaHospitalar`. Fontes locais: `User Stories.md` (US01–US10), `docs/bdd/compatibilidade.feature`, `docs/bdd/rastreabilidade-recebimento.feature` e `entrega-04/api-e-operacao.md`. Os documentos U2 e a decisão da Parte 3 não foram localizados; a história âncora abaixo é uma **proposta derivada da solicitação desta auditoria**, a validar contra a U2 original. Monolito modular é uma restrição expressa do usuário, não uma ADR preexistente verificada.

## SDD-01 Recebimento e utilização protegidos pela entidade

**Natureza:** reforço de invariantes existentes; correção pontual autorizada. Sem mudança REST, banco, estados ou UI.

**Contexto e problema:** `EntregaHospitalar.receber` e `utilizar` alteram campos sem verificar o estado anterior. `AtendimentoHospitalarService` já recusa esses fluxos, mas uma chamada direta à entidade pode sobrescrever o recebimento ou registrar uso indevido.

**Comportamento esperado:** a entidade deve recusar recebimento repetido; exigir condição; exigir observação não vazia para não conformidade; permitir uso apenas após recebimento íntegro e uma única vez. A recusa deve ocorrer antes de qualquer mutação.

**Invariantes e responsabilidades:** recebimento e uso são fatos únicos e exigem instante não nulo, pois o instante representa a ocorrência do fato e também protege contra repetição. Observação de recebimento íntegro continua opcional. Recebimento não conforme é registrado, mas não habilita utilização. Validade e estado da bolsa continuam sob a coordenação atual do caso de uso, assim como autorização hospitalar, locks, transação, persistência, evento e atualização da requisição. Não adicionar validações clínicas ou de ordenação cronológica novas nesta correção.

**Critérios de aceite testáveis:**

1. Um recebimento íntegro conserva condição, observação, instante e origem informados.
2. Segunda confirmação lança `IllegalStateException` e preserva todos os dados da primeira.
3. Condição ausente ou não conformidade sem observação lança `IllegalArgumentException`, sem marcar a entrega recebida.
4. Uso sem recebimento, após não conformidade ou já registrado lança `IllegalStateException` e preserva os campos.
5. Uso válido registra instante e origem; observação nula no recebimento íntegro permanece aceita.
6. As respostas HTTP existentes (400/403/404/409), os eventos e o rollback permanecem cobertos pelos testes atuais; as guardas HTTP do service serão preservadas nesta etapa.
7. Instante nulo de recebimento ou utilização é recusado com `IllegalArgumentException` antes de modificar campos, impedindo contornar as marcas de ocorrência. Critério acrescentado na revisão, antes do segundo ciclo RED/GREEN.

```gherkin
Funcionalidade: Proteger os fatos de uma entrega hospitalar
  Cenário: Impedir confirmação repetida
    Dado uma entrega com recebimento íntegro registrado
    Quando a própria entidade recebe uma segunda confirmação não conforme
    Então rejeita a transição e preserva o primeiro recebimento

  Cenário: Impedir uso prematuro
    Dado uma entrega sem recebimento
    Quando a própria entidade registra utilização
    Então rejeita a transição sem alterar instante nem origem de uso

  Cenário: Não conformidade exige justificativa
    Dado uma entrega sem recebimento
    Quando recebe condição não conforme sem observação preenchida
    Então rejeita os dados sem modificar o recebimento

  Cenário: Recebimento íntegro habilita um único uso
    Dado uma entrega recebida em condição íntegra
    Quando registra uma utilização e depois tenta registrar outra
    Então preserva somente a primeira utilização
```

**Exceções:** estado inválido → `IllegalStateException`; condição/observação inválida → `IllegalArgumentException`. São exceções internas sem dependência HTTP. Os services conservam os mapeamentos REST anteriores; sua remoção depende da etapa de arquitetura.

**Estratégia:** JUnit puro, instantes fixos, sem Spring, banco ou reflexão; RED com falhas de asserção nos métodos existentes, GREEN com guardas mínimas e REFACTOR revisado com suíte completa. Integração MockMvc/rollback já existente e PostgreSQL real quando o ambiente estiver disponível.

**Conclusão:** critérios 1–7, suíte padrão verde, cobertura comparável antes/depois e evidências de execução. PostgreSQL, Docker e CI devem ter seu estado real registrado, sem presumir aprovação.

## SDD-02 Reserva exclusiva de hemácias compatíveis por FEFO

**Natureza:** funcionalidade nova, dependente de aprovação de modelo, persistência e eventual API. Não implementada nesta auditoria.

**Contexto e problema:** US03 atualmente consulta sugestões ordenadas; a entrega manual não representa reserva. Duas consultas podem listar a mesma bolsa. A exclusividade já testada de uma *entrega* não comprova exclusividade de uma *reserva*.

**Comportamento esperado proposto:** reservar uma bolsa para uma requisição de hemácias, escolhendo a primeira compatível e válida por `(validade ASC, id ASC)`. Executar seleção, proteção concorrente, vínculo e evento em uma transação. Revalidar após adquirir a trava. O lote inicial é uma bolsa por comando, para conter o escopo de 01/12.

**Regras e invariantes propostas:** somente `CONCENTRADO_HEMACIAS`; matriz ABO/Rh didática vigente; `validade > hoje`; bolsa disponível; uma única reserva ativa por bolsa; vínculo com requisição existente e quantidade ainda não atendida. O service coordena repositórios, locks e auditoria; domínio decide elegibilidade/transição. O adapter JPA implementa consulta FEFO e exclusividade no banco. Não usar lista encadeada da U1 como estoque persistente.

**Decisões para aprovação antes de implementar:** estado `RESERVADA` e migrações dos CHECKs H2/PostgreSQL; representação persistente da reserva e restrição única; consumo da reserva na entrega; semântica de repetição do comando (idempotência); contrato REST, autorização, reserva parcial para quantidades maiores, cancelamento/liberação/expiração. Proposta mínima: repetir comando com a mesma chave devolve a mesma reserva sem consumir outra bolsa; indisponibilidade retorna resultado de domínio sem criar estado ou notificação que ainda não existem. Isso precisa ser reconciliado com o texto completo da US03/U2 antes do GREEN.

**Critérios de aceite propostos:**

1. Para receptor A−, O− válida com vencimento mais próximo é escolhida antes de A−; A+ e B− são excluídas.
2. Empate de validade usa menor ID; vencimento no dia atual é inelegível.
3. Bolsa reservada deixa de aparecer como disponível para outra reserva.
4. Dois comandos concorrentes para uma única bolsa produzem exatamente um vínculo ativo, sem evento órfão.
5. Falha ao gravar evento reverte reserva, estado e vínculo.
6. Falta de estoque não altera bolsa nem inventa entrega. Quantidade já atendida e componente fora do escopo são recusados.
7. Comando repetido não cria reserva/evento duplicado, conforme decisão de idempotência aprovada.

```gherkin
Funcionalidade: Reservar exclusivamente uma bolsa compatível por FEFO
  Cenário: Reservar a primeira elegível
    Dado uma requisição A negativo e bolsas O negativo válidas vencendo em 3 e 10 dias
    Quando o gestor solicita reserva de uma bolsa
    Então a bolsa com vencimento em 3 dias fica vinculada à requisição
    E não fica disponível a outra reserva

  Cenário: Disputa concorrente
    Dado uma única bolsa elegível e duas requisições diferentes
    Quando os comandos de reserva são executados concorrentemente
    Então somente uma requisição obtém a bolsa
    E existe apenas um vínculo ativo e um evento de reserva

  Cenário: Fronteira de validade e empate
    Dado uma bolsa vencendo hoje e duas válidas com a mesma validade
    Quando a reserva seleciona uma bolsa
    Então exclui a que vence hoje e seleciona o menor ID entre as válidas

  Cenário: Auditoria falha
    Dado uma bolsa elegível
    Quando falha a persistência do evento durante a reserva
    Então nenhuma reserva nem mudança de estado é confirmada
```

**Exceções:** estoque indisponível, componente não suportado, requisição ausente, quantidade excedida, conflito concorrente e falha de persistência. Códigos REST só serão definidos no contrato aprovado.

**Estratégia:** matriz unitária independente 8×8; elegibilidade e transições sem frameworks; integração FEFO/empate; concorrência em PostgreSQL com barreira e timeout; rollback real sem transação externa do teste; contrato HTTP após aprovação. Testes propostos, ainda inexistentes: `ReservaFefoTest`, `ReservaConcorrentePostgresIT`, `ReservaRollbackIT` e `ReservaContratoTest`.

**Conclusão:** decisões aprovadas, migração validada em esquema isolado, RED/GREEN registrados, critérios 1–7 verificados e regressão de entrega manual preservada. Se a integração exceder o prazo, entregar até 01/12 o núcleo testado e declarar persistência/API pendentes; não anunciar reserva completa.

## SDD-03 Estoque, validade e descarte

**Natureza:** refatoração estrutural proposta de regras existentes; aguarda aprovação.

**Contexto:** `BolsaHemocomponente` expõe `setStatus`/`setValidade`; `descartar` aceita qualquer estado diferente de descartado. O service restringe o descarte a disponíveis, e usa datas globais em parte do fluxo.

**Esperado e invariantes:** proteger criação/correção de dados e transições na raiz Bolsa; validade didática calculada com a política configurável atual; validade igual a hoje impede operação; descarte automático somente de bolsa disponível vencida; descarte repetido preserva o primeiro instante/motivo/origem; alertas 5/2 dias permanecem únicos por bolsa e limiar. Validadores de entrada não substituem invariantes. Preservar volume 1–10000 e código normalizado; unicidade concorrente pertence ao banco/adapter.

**Aceite:** cada estado não disponível é recusado pelo descarte automático; datas -1/0/+1, limiares 6/5/3/2/1 e configuração inválida testados; correção de bolsa entregue recusada; cadastro/edição/descarte e evento confirmam ou revertem juntos.

```gherkin
Funcionalidade: Preservar o ciclo de estoque
  Cenário: Descarte no limite
    Dado uma bolsa disponível cuja validade é hoje
    Quando a verificação de validade é executada
    Então a bolsa é descartada com evento na mesma transação
  Cenário: Impedir descarte automático de bolsa entregue
    Dado uma bolsa entregue cuja validade é hoje
    Quando o domínio recebe um comando de descarte automático
    Então rejeita a transição sem alterar seu histórico
```

**Exceções:** cadastro incompleto, coleta futura, volume fora da faixa, código duplicado, estado inadequado, prazo/limiar inválido e falha de auditoria. Preservar os erros REST atuais através do adapter de apresentação.

**Testes:** unitários com Clock/data explícita, integração de unicidade/rollback/alerta e contratos atuais. **Conclusão:** não existir escrita arbitrária do estado no novo domínio, mapeamento JPA bidirecional testado e comportamento REST preservado. Unificar UTC/data local exige decisão explícita; não alterar a fronteira silenciosamente.

## SDD-04 Requisição e estados derivados do atendimento

**Natureza:** refatoração proposta, sem novas promessas de fila, notificação ou parceiros.

**Contexto:** status é um setter público; `AtendimentoHospitalarService.atualizarStatus` contém toda a precedência. Hospital é entidade JPA compartilhada; `hospitalId` é entrada HTTP embutida na entidade.

**Esperado e invariantes:** Requisição protege quantidade positiva e vínculo hospitalar; uma política pura calcula estado usando um resumo imutável de entregas, sem importar entidades JPA de Expedição. Preservar precedência: pendência ativa > não conformidade > utilização total/parcial > recebimento total/parcial > entrega total/parcial > solicitada. Não acrescentar proibição retroativa de prazo passado sem decisão de negócio.

**Aceite:** tabela de todos os ramos, incluindo zero entregas, quantidades 1/2, mistura de atraso/não conformidade e resolução de atraso após recebimento tardio; quantidade entregue nunca excede solicitada.

```gherkin
Funcionalidade: Derivar a situação da requisição
  Cenário: Recebimento parcial
    Dado uma requisição de duas bolsas e somente uma recebida
    Quando o estado é recalculado
    Então indica parcialmente recebida
  Cenário: Prioridade da pendência ativa
    Dado uma entrega não conforme e outra atrasada sem recebimento
    Quando o estado é recalculado
    Então indica recebimento pendente
```

**Exceções:** hospital inexistente, quantidade inválida, excesso de entrega e resumo inconsistente. **Testes:** tabela unitária + aceitação e concorrência já existentes. **Conclusão:** política independente de Spring/JPA/HTTP e mesma resposta observável para cada combinação existente. Fila de emergência da US02 continua funcionalidade nova.

## SDD-05 Rastreabilidade e prazo de confirmação

**Natureza:** refatoração de infraestrutura/portas e fortalecimento de testes; requer aprovação estrutural.

**Contexto:** registro usa `EntityManager` diretamente e `EventoBolsa` consulta o relógio global; já há transação MANDATORY, repositório sem mutação e proteção SQL PostgreSQL.

**Esperado e invariantes:** evento append-only com identificador da bolsa, instante UTC, tipo, origem e detalhes; gravação atômica junto da operação; histórico legado continua vazio; atraso apenas se `agora > prazo`, único alerta persistente; recebimento tardio encerra pendência ativa sem apagar fato anterior. Transação síncrona no monolito; eventos em memória assíncronos não substituem a atomicidade atual.

**Aceite:** SQL UPDATE/DELETE/TRUNCATE rejeitados em PostgreSQL; rollback por falha do evento; fronteira exata e +1 microssegundo; verificador repetido gera um evento; ordenação por momento/ID; mesma origem e precisão temporal atual.

```gherkin
Funcionalidade: Registrar fatos e atraso atomicamente
  Cenário: Instante exato do prazo
    Dado uma entrega sem confirmação e prazo igual ao instante atual
    Quando verifica pendências
    Então não cria alerta
  Cenário: Verificação repetida após o prazo
    Dado uma entrega sem confirmação cujo prazo foi ultrapassado
    Quando verifica pendências duas vezes
    Então existe somente um alerta e um evento de atraso
```

**Exceções:** bolsa inexistente, tentativa de mutação, operação fora de transação e falha do banco. **Testes:** unitários de tempo, integração transacional e `HistoricoPostgresIT`. **Conclusão:** porta de aplicação sem `EntityManager`, adapter transacional e proteções SQL preservadas. Migrações versionadas serão uma etapa separada.

## SDD-06 Acesso e limites entre camadas

**Natureza:** fronteiras arquiteturais propostas. Autenticação robusta é funcionalidade nova, fora desta correção e dependente de aprovação.

**Contexto:** controllers repetem verificações e confiam em `X-Perfil`/`X-Hospital-Id`; login não cria sessão/token. Services importam `ResponseStatusException`; entidades persistidas são contratos JSON.

**Esperado e invariantes da refatoração:** manter contratos enquanto adapters convertem DTOs e erros; representar contexto do solicitante como entrada explícita da aplicação; autorizar o hospital proprietário no caso de uso, inclusive em chamadas não HTTP; domínio não conhece Spring Web. Não apresentar cabeçalho controlado pelo cliente como identidade autenticada.

**Aceite:** mesmos endpoints, JSON e códigos em contratos de caracterização; hospital diverso/ausente não altera dados; classes novas de domínio não dependem de JPA/Jackson/Spring; apresentação não acessa repositório diretamente; adapters de dados implementam portas internas; chamadas entre contextos por contratos explícitos.

```gherkin
Funcionalidade: Preservar autorização e direção das dependências
  Cenário: Hospital diferente
    Dado uma entrega pertencente ao hospital A
    Quando o contexto declarado do hospital B confirma recebimento
    Então a aplicação nega acesso e nenhum fato muda
  Cenário: Domínio independente
    Dado as classes do novo pacote negocio.dominio
    Quando as regras arquiteturais analisam suas dependências
    Então nenhuma depende de Spring, JPA ou Jackson
```

**Exceções:** perfil/contexto ausente, recurso alheio e credenciais incorretas, com semântica atual. **Testes:** JUnit/MockMvc e ArchUnit em classes concretas, falhando em seleção vazia; regras novas somente para módulos efetivamente introduzidos. **Conclusão:** contratos verdes e fronteiras exercitadas. Migrar a autenticação, remover cabeçalhos ou alterar cadastro de perfis exige contrato e aprovação próprios.

## Rastreabilidade inicial

| Requisito | Especificação | Teste | Implementação | Evidência/estado |
|---|---|---|---|---|
| US10: recebimento único e uso íntegro | SDD-01 | novo `entity/EntregaHospitalarTest`; existentes `impedeDuplicidadeDeRecebimentoEDeUso`, `impedeUsoAntesDeReceberENaoConformidadeBloqueiaUso`, `validaAtestacaoCondicaoObservacaoERequisicaoDaEntrega` | `EntregaHospitalar.receber/utilizar`; orquestração em `AtendimentoHospitalarService` | RED/GREEN desta auditoria serão registrados em `evidencias/` |
| US03: ABO/Rh | SDD-02 | existente `CompatibilidadeServiceTest.tabelaCompletaDeDoadoresPorReceptor` | `CompatibilidadeService` | baseline JaCoCo; commits `9fff19a` e `ae719f3` |
| US03: sugestão FEFO | SDD-02 | existentes `BolsaCompativelRepositoryTest` e `CompatibilidadeAceitacaoTest` | `BolsaService.compativeis`/`BolsaRepository` | consulta existente; reserva não implementada |
| Âncora: reserva exclusiva | SDD-02 | propostos `ReservaFefoTest`, `ReservaConcorrentePostgresIT`, `ReservaRollbackIT`, `ReservaContratoTest` | proposta `alocacao/negocio/aplicacao/ReservarBolsa`; ainda inexistente | aguarda aprovação; nenhuma execução alegada |
| US01/US07: validade, descarte e alertas | SDD-03 | existentes `ValidadeServiceTest`, `EstoqueControllerTest`, `cadastroEdicaoEDescarteGeramEventosComRetrato` | `BolsaHemocomponente`, `BolsaService`, `ValidadeService` | baseline; fortalecimento da raiz proposto |
| US02/US10: parcial/total | SDD-04 | existente `quantidadeParcialNaoFinalizaRequisicaoNemPermiteExcesso`; tabela completa proposta | `AtendimentoHospitalarService.atualizarStatus` | baseline; extração proposta |
| US09: histórico atômico e imutável | SDD-05 | existentes `RastreabilidadeTransacaoTest`, `HistoricoPostgresIT` | `RastreabilidadeService`, `EventoBolsa`, `ProtecaoHistoricoPostgres` | H2 baseline; PostgreSQL separado |
| US10: atraso sem duplicação | SDD-05 | existente `bddPendenciaAposPrazoAlertaUmaVezEPreservaAposConfirmacao` | `AtendimentoHospitalarService.verificarPendencias` | baseline, Clock fixo |
| US08: acesso por perfil/hospital | SDD-06 | existentes `AcessoPorPerfilTest`, `hospitalDiferenteESemContextoNaoLeNemConfirmaNemUsa` | controllers e `exigirHospital` | controle didático; autenticação real pendente |
| Professor: isolamento arquitetural | SDD-06 | ArchUnit proposto, ainda não adicionado | portas/adapters propostos | aguarda aprovação estrutural |
