# Cobertura e inventário de testes medidos

Gerado a partir dos XMLs preservados em `evidencias/`, com JaCoCo 0.8.14. As duas medições comparáveis usam a suíte padrão completa em JVM 21, release 17, H2/MockMvc. As execuções PostgreSQL são registradas separadamente.

## Execuções JUnit

| Etapa | Executados | Aprovados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|---:|
| baseline | 84 | 84 | 0 | 0 | 0 |
| final | 98 | 98 | 0 | 0 | 0 |
| tdd-red | 12 | 3 | 9 | 0 | 0 |
| tdd-green | 12 | 12 | 0 | 0 | 0 |
| tdd-red-2 | 14 | 12 | 2 | 0 | 0 |
| tdd-green-2 | 14 | 14 | 0 | 0 | 0 |
| postgres-surefire | 17 | 17 | 0 | 0 | 0 |
| postgres-failsafe | 4 | 4 | 0 | 0 | 0 |

Contam-se invocações JUnit, inclusive parâmetros. A execução PostgreSQL repete 17 cenários existentes e acrescenta 4 IT já existentes; não são 21 testes novos.

## Total comparável

| Etapa | Linhas | Branches | Métodos |
|---|---:|---:|---:|
| baseline | 705/833 (84.63%) | 207/273 (75.82%) | 247/277 (89.17%) |
| final | 719/847 (84.89%) | 227/293 (77.47%) | 247/277 (89.17%) |

## Pacotes reais

| Grupo | Linhas antes → depois | Branches antes → depois | Métodos antes → depois |
|---|---:|---:|---:|
| com/hemochain | 1/3 (33.33%) → 1/3 (33.33%) | N/A → N/A | 1/2 (50.00%) → 1/2 (50.00%) |
| com/hemochain/config | 32/44 (72.73%) → 32/44 (72.73%) | 12/18 (66.67%) → 12/18 (66.67%) | 14/14 (100.00%) → 14/14 (100.00%) |
| com/hemochain/controller | 76/95 (80.00%) → 76/95 (80.00%) | 18/34 (52.94%) → 18/34 (52.94%) | 33/41 (80.49%) → 33/41 (80.49%) |
| com/hemochain/dto | 2/4 (50.00%) → 2/4 (50.00%) | N/A → N/A | 2/4 (50.00%) → 2/4 (50.00%) |
| com/hemochain/entity | 195/200 (97.50%) → 209/214 (97.66%) | 2/4 (50.00%) → 22/24 (91.67%) | 102/105 (97.14%) → 102/105 (97.14%) |
| com/hemochain/estrutura | 105/118 (88.98%) → 105/118 (88.98%) | 25/28 (89.29%) → 25/28 (89.29%) | 29/38 (76.32%) → 29/38 (76.32%) |
| com/hemochain/repository | N/A → N/A | N/A → N/A | N/A → N/A |
| com/hemochain/service | 294/369 (79.67%) → 294/369 (79.67%) | 150/189 (79.37%) → 150/189 (79.37%) | 66/73 (90.41%) → 66/73 (90.41%) |

## Contextos analíticos

| Grupo | Linhas antes → depois | Branches antes → depois | Métodos antes → depois |
|---|---:|---:|---:|
| Acesso | 42/46 (91.30%) → 42/46 (91.30%) | 6/6 (100.00%) → 6/6 (100.00%) | 17/20 (85.00%) → 17/20 (85.00%) |
| Alocacao (somente compatibilidade existente) | 15/15 (100.00%) → 15/15 (100.00%) | 16/16 (100.00%) → 16/16 (100.00%) | 7/7 (100.00%) → 7/7 (100.00%) |
| Apoio de dados sinteticos | 7/57 (12.28%) → 7/57 (12.28%) | 0/16 (0.00%) → 0/16 (0.00%) | 2/7 (28.57%) → 2/7 (28.57%) |
| Compartilhado e inicializacao | 23/25 (92.00%) → 23/25 (92.00%) | N/A → N/A | 8/9 (88.89%) → 8/9 (88.89%) |
| Estoque | 170/178 (95.51%) → 170/178 (95.51%) | 44/59 (74.58%) → 44/59 (74.58%) | 56/58 (96.55%) → 56/58 (96.55%) |
| Exercicio U1 de estruturas de dados | 105/118 (88.98%) → 105/118 (88.98%) | 25/28 (89.29%) → 25/28 (89.29%) | 29/38 (76.32%) → 29/38 (76.32%) |
| Expedicao e recebimento | 143/154 (92.86%) → 157/168 (93.45%) | 80/96 (83.33%) → 100/116 (86.21%) | 52/55 (94.55%) → 52/55 (94.55%) |
| Interfaces de repositorio sem bytecode executavel | N/A → N/A | N/A → N/A | N/A → N/A |
| Rastreabilidade | 33/46 (71.74%) → 33/46 (71.74%) | 3/6 (50.00%) → 3/6 (50.00%) | 15/16 (93.75%) → 15/16 (93.75%) |
| Requisicao e cadastro hospitalar | 167/194 (86.08%) → 167/194 (86.08%) | 33/46 (71.74%) → 33/46 (71.74%) | 61/67 (91.04%) → 61/67 (91.04%) |

Contextos são uma classificação por responsabilidade principal, não pacotes isolados já implementados. Cada classe pertence a exatamente um grupo; classes internas acompanham a externa. O mapeamento completo está em `metricas.json` e neste script. N/A significa ausência de bytecode/decisões no denominador, não cobertura de 100%. Os repositórios Spring Data são exercitados por integração, mas suas interfaces não têm linhas executáveis mensuráveis. Alocação mede só compatibilidade; reserva ainda não existe.

## Inventário por classe de teste

| Classe | Antes | Depois |
|---|---:|---:|
| com.hemochain.HemochainApplicationTests | 1 | 1 |
| com.hemochain.config.EvolucaoStatusBolsaH2Test | 2 | 2 |
| com.hemochain.controller.AcessoPorPerfilTest | 5 | 5 |
| com.hemochain.controller.AtendimentoHospitalarAceitacaoTest | 14 | 14 |
| com.hemochain.controller.CompatibilidadeAceitacaoTest | 4 | 4 |
| com.hemochain.controller.EstoqueControllerTest | 11 | 11 |
| com.hemochain.entity.EntregaHospitalarTest | 0 | 14 |
| com.hemochain.estrutura.EstoqueBolsasTest | 4 | 4 |
| com.hemochain.estrutura.FilaRequisicoesTest | 3 | 3 |
| com.hemochain.estrutura.HistoricoOperacoesTest | 3 | 3 |
| com.hemochain.repository.BolsaCompativelRepositoryTest | 1 | 1 |
| com.hemochain.service.AtendimentoHospitalarServiceTest | 3 | 3 |
| com.hemochain.service.CompatibilidadeServiceTest | 6 | 6 |
| com.hemochain.service.HospitalServiceTest | 2 | 2 |
| com.hemochain.service.RastreabilidadeTransacaoTest | 3 | 3 |
| com.hemochain.service.RequisicaoIndicadoresServiceTest | 9 | 9 |
| com.hemochain.service.RequisicaoServiceTest | 4 | 4 |
| com.hemochain.service.UsuarioServiceTest | 5 | 5 |
| com.hemochain.service.ValidadeServiceTest | 4 | 4 |

## Limites da métrica

JaCoCo mede execução de bytecode: getters e construtores elevam cobertura de entidades sem demonstrar invariantes. Na baseline, EntregaHospitalar tinha 100% de linhas e ainda aceitava todas as nove violações observadas no RED. Cobertura não prova TDD, exclusividade, corretude clínica ou ausência de regressões. As contagens Java não incluem asserts C nem scripts Playwright. Não foram excluídas classes de produção para melhorar percentuais.
