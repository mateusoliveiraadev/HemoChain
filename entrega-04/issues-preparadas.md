# Issues preparadas para publicação

Publicar somente com autorização da equipe. Manter abertas até o merge do PR corretivo; relacionar o PR por `Closes #numero` depois que existirem números reais.

## E04-B01 — Hospital acessa requisições fora do contexto declarado

**Tipo:** bug de autorização. **Severidade:** alta. **Estado:** corrigido na branch; aguarda revisão/merge.

**Causa:** `RequisicaoController.listar` encaminhava gestor e hospital para `listarTodas()`, apesar do comentário indicar que hospital consultava somente suas requisições.

**Reprodução:** cadastrar hospitais A/B e suas requisições; consultar `/requisicoes` como `HOSPITAL_SOLICITANTE`. A implementação anterior devolvia todos os registros.

**Esperado:** hospital só consulta o contexto hospitalar declarado e não confirma bolsa de outro hospital.

**Correção:** exigir `X-Hospital-Id`, filtrar por `hospital.id` e comparar o contexto com o vínculo persistido em recebimento, uso e consulta de entregas. Cadastro também exige correspondência do contexto.

**Validação:** testes de listagem filtrada, falta de contexto, contexto divergente e escrita proibida no PostgreSQL. Limitação de autenticação por cabeçalho continua explícita.

## E04-B02 — Campo transitório impede atualização de requisição persistida

**Tipo:** bug de persistência/validação. **Severidade:** média. **Estado:** corrigido na branch; aguarda revisão/merge.

**Causa:** `hospitalId` é `@Transient`, mas sua anotação `@NotNull` era aplicada no pre-update JPA. O campo é nulo depois de recarregar uma entidade do banco.

**Reprodução:** criar requisição em uma transação e registrar entrega em outra; o commit falhava com `ConstraintViolationException`, ocultado em testes que mantivessem a entidade de entrada na mesma transação.

**Correção:** validar o ID na entrada/serviço e conferir o hospital persistido para operações posteriores, sem exigir transient no ciclo JPA.

**Validação:** fluxo HTTP completo sem `@Transactional` externo; testes PostgreSQL de entrega/recebimento/uso e rollback.

## E04-B03 — Restrição legada bloqueia novos estados da bolsa

**Tipo:** migração de esquema. **Severidade:** alta. **Estado:** corrigido na branch; aguarda revisão/merge.

**Causa:** o CHECK gerado anteriormente para `StatusBolsa` não é substituído pelo Hibernate update quando o enum ganha estados.

**Reprodução:** base com CHECK `DISPONIVEL/DESCARTADO`; tentar persistir `ENTREGUE` viola a restrição.

**Correção:** evolução explícita e idempotente de CHECK; preservar as linhas e rejeitar estados fora do enum. PostgreSQL executa evolução e instalação do gatilho em transação.

**Validação:** testes de migração em H2 e schema PostgreSQL isolado com linha legada, duas execuções da evolução e tentativa de status inválido.

## E04-B04 — Default no tipo da coluna produz DDL inválido no PostgreSQL

**Tipo:** defeito identificado na implementação da Entrega 04. **Severidade:** média. **Estado:** corrigido na branch; aguarda revisão/merge.

**Causa:** `columnDefinition="varchar(40) default 'SOLICITADA'"` era incorporado ao `ALTER ... SET DATA TYPE` na segunda inicialização.

**Correção:** `@Column(length=40)` e `@ColumnDefault` separados.

**Validação:** reexecução dos testes PostgreSQL e revisão dos logs de startup/reinício; nenhum DDL inválido remanescente. O teste de FK do histórico emite erro esperado e capturado por `assertThrows`, sem falha da suíte.
