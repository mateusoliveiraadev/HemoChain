# Issue/Bug Tracker — Entrega 04

Tracker local de defeitos reais. As evidências de execução e a captura desta tabela são locais; publicação em GitHub Issues depende de autorização. Não representa uma captura do GitHub.

| ID | Problema / causa | Severidade | Correção | Validação | Status |
|---|---|---|---|---|---|
| E04-B01 | Hospital via `GET /requisicoes` recebia todos os registros; controller usava `listarTodas()` para qualquer perfil de leitura | Alta | Exigir contexto `X-Hospital-Id`, filtrar por hospital e conferir o mesmo contexto no cadastro e nas operações de US10 | `listaHospitalarFiltraHospitalEImpedeCadastroComOutroContexto`, `hospitalDiferenteESemContextoNaoLeNemConfirmaNemUsa` | CORRIGIDO e validado localmente; publicação pendente |
| E04-B02 | Atualizar requisição recarregada falhava no commit JPA: `@NotNull` no `hospitalId` transitório, não preenchido ao ler do banco | Média | Retirar validação persistente do campo transitório; manter validação explícita de entrada no serviço e no controller | Falha reproduzida no desenvolvimento; aceitação sem transação externa e rollback no PostgreSQL | CORRIGIDO e validado localmente; publicação pendente |
| E04-B03 | Bancos anteriores mantinham CHECK de status contendo somente DISPONIVEL/DESCARTADO; `ddl-auto=update` não amplia essa restrição | Alta | Evolução idempotente do CHECK preservando linhas, em PostgreSQL e H2 | `EvolucaoStatusBolsaH2Test`, `migraCheckLegadoEmSchemaIsoladoSemApagarBolsa` | CORRIGIDO e validado localmente; publicação pendente |
| E04-B04 | Nova definição de coluna com `columnDefinition` contendo DEFAULT causava DDL inválido em reinicialização PostgreSQL (`ALTER ... TYPE ... DEFAULT`) | Média | Mapear tamanho/tipo e `@ColumnDefault` separadamente; revisar logs de novo startup | Segunda execução PostgreSQL e reinício Compose sem esse erro | CORRIGIDO e revalidado localmente; publicação pendente |

## Limitação mantida, sem alegar correção de autenticação

O cabeçalho didático de contexto pode ser informado pelo cliente. A correção B01 impede acesso fora do contexto declarado, mas não substitui sessão/token, vínculo individual usuário-hospital nem autenticação de produção. Esse mecanismo veio das entregas anteriores e está documentado. Use apenas dados sintéticos.

## Reprodução resumida

- B01: criar requisições de hospitais A e B; consultar como hospital A. A resposta deve conter apenas A; informar B para entrega de A retorna 403.
- B02: criar requisição, encerrar transação, recarregar e registrar entrega/recebimento. O commit deve funcionar sem depender do campo transient de entrada.
- B03: banco com bolsa disponível e CHECK antigo; executar inicialização de evolução; linha permanece e estado ENTREGUE passa a ser aceito; estado desconhecido continua bloqueado.
- B04: iniciar duas vezes contra o mesmo PostgreSQL e examinar os logs; a coluna status mantém default SOLICITADA e não há comando de alteração de tipo com DEFAULT.

As correções estão na branch da Entrega 04. As minutas completas para issues estão em [issues-preparadas.md](issues-preparadas.md); o PR está em [pull-request.md](pull-request.md).
