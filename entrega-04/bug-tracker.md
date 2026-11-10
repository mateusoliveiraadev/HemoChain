# Issue/Bug Tracker — Entrega 04

Defeitos publicados em [GitHub Issues](https://github.com/mateusoliveiraadev/HemoChain/issues?q=is%3Aissue+E04). Correções disponíveis no [PR Draft #8](https://github.com/mateusoliveiraadev/HemoChain/pull/8), aguardando revisão e merge. As issues permanecem abertas.

| ID | Problema / causa | Severidade | Correção | Validação | Status |
|---|---|---|---|---|---|
| [E04-B01 / #4](https://github.com/mateusoliveiraadev/HemoChain/issues/4) | Hospital via `GET /requisicoes` recebia todos os registros; controller usava `listarTodas()` para qualquer perfil de leitura | Alta | Exigir contexto `X-Hospital-Id`, filtrar por hospital e conferir o mesmo contexto no cadastro e nas operações de US10 | `listaHospitalarFiltraHospitalEImpedeCadastroComOutroContexto`, `hospitalDiferenteESemContextoNaoLeNemConfirmaNemUsa` | Corrigido e validado; aguardando revisão/merge |
| [E04-B02 / #5](https://github.com/mateusoliveiraadev/HemoChain/issues/5) | Atualizar requisição recarregada falhava no commit JPA: `@NotNull` no `hospitalId` transitório, não preenchido ao ler do banco | Média | Retirar validação persistente do campo transitório; manter validação explícita de entrada no serviço e no controller | Aceitação sem transação externa e rollback no PostgreSQL | Corrigido e validado; aguardando revisão/merge |
| [E04-B03 / #6](https://github.com/mateusoliveiraadev/HemoChain/issues/6) | Bancos anteriores mantinham CHECK de status contendo somente DISPONIVEL/DESCARTADO; `ddl-auto=update` não amplia essa restrição | Alta | Evolução idempotente do CHECK preservando linhas, em PostgreSQL e H2 | `EvolucaoStatusBolsaH2Test`, `migraCheckLegadoEmSchemaIsoladoSemApagarBolsa` | Corrigido e validado; aguardando revisão/merge |
| [E04-B04 / #7](https://github.com/mateusoliveiraadev/HemoChain/issues/7) | Nova definição de coluna com `columnDefinition` contendo DEFAULT causava DDL inválido em reinicialização PostgreSQL (`ALTER ... TYPE ... DEFAULT`) | Média | Mapear tamanho/tipo e `@ColumnDefault` separadamente; revisar logs de novo startup | Segunda execução PostgreSQL e reinício Compose sem esse erro | Corrigido e revalidado; aguardando revisão/merge |

## Limitação mantida, sem alegar correção de autenticação

O cabeçalho didático de contexto pode ser informado pelo cliente. A correção B01 impede acesso fora do contexto declarado, mas não substitui sessão/token, vínculo individual usuário-hospital nem autenticação de produção. Esse mecanismo veio das entregas anteriores e está documentado. Use apenas dados sintéticos.

## Reprodução resumida

- B01: criar requisições de hospitais A e B; consultar como hospital A. A resposta deve conter apenas A; informar B para entrega de A retorna 403.
- B02: criar requisição, encerrar transação, recarregar e registrar entrega/recebimento. O commit deve funcionar sem depender do campo transient de entrada.
- B03: banco com bolsa disponível e CHECK antigo; executar inicialização de evolução; linha permanece e estado ENTREGUE passa a ser aceito; estado desconhecido continua bloqueado.
- B04: iniciar duas vezes contra o mesmo PostgreSQL e examinar os logs; a coluna status mantém default SOLICITADA e não há comando de alteração de tipo com DEFAULT.

As issues registram causa, reprodução, severidade, correção, validação e os commits de referência. O PR #8 associa as quatro por `Closes`, sem fechá-las antes de eventual merge.
