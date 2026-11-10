# Auditoria — Entrega 04

Data de execução: 10/11/2026. A seção acadêmica mantém a data prevista **09/11**.

## Repositório e preservação

- Utilizada a cópia local existente; nenhum clone necessário.
- Branch inicial: `main`; HEAD inicial `71f9f4cebb1b17f0e7d50f7a4e2fbd1efb30a4f3`.
- Remote: `https://github.com/mateusoliveiraadev/HemoChain.git`.
- Após `git fetch origin`: local 0 commits à frente e 21 atrás; atualização por `git merge --ff-only origin/main` para `924d580`.
- Branch de trabalho: `feature/entrega-04-rastreabilidade-recebimento`.
- Alteração preexistente preservada e excluída dos commits desta entrega: `entrega-03/bug-tracker.png`, SHA-256 `C47F7AE1DFA436ADBB07DCFD9313137F793018151209A2B4A17C2127C0B823BC`.
- Nenhum `reset --hard`, `clean`, rebase destrutivo, merge na main ou deploy de produção.
- Histórico remoto consultado: PRs #1 (container/TDD/deploy), #2 e #3 (documentação). Sem issues existentes na consulta inicial. Branches remotas examinadas com fetch/listagem e histórico; nenhuma implementação de US09/US10 encontrada na main atualizada.

## Fontes lidas

README; `User Stories.md`; `User Stories 2° Entrega.md`; `entrega-03/post-its.md`; fontes e testes Java; páginas estáticas; propriedades e POM; Dockerfile/Compose; workflows CI/CD e rollback; `docs/DEPLOY.md`; BDD de compatibilidade e checklist de evidências; histórico Git e tracker remoto.

## Contratos encontrados

| História | Implementação anterior | Consequência para esta entrega |
|---|---|---|
| US01 | Bolsa, código único, validade calculada, edição e consultas; bloqueio pessimista | Registrar criação e antes/depois da edição sem mudar campos de entrada |
| US02 | Requisição com hospital, quantidade, urgência e prazo; sem bolsa nem status | Adicionar status e associação explícita por entrega, aceitando quantidades parciais |
| US03 | Sugestão de **hemácias** ABO/Rh em FEFO; sem reserva | Reutilizar exatamente essa regra; seleção manual; reserva automática segue pendente |
| US07 | Alertas 5/2 dias e descarte automático de disponíveis; data/motivo/SISTEMA na bolsa | Preservar esse contrato e acrescentar evento de descarte na mesma transação |
| US08 | Login BCrypt e contexto didático `X-Perfil`; sem sessão/token nem vínculo usuário-hospital | Usar origem declarada, acrescentar `X-Hospital-Id` e validar propriedade; documentar limitação |

Há duas representações de bolsa: entidade JPA de negócio e estruturas didáticas em `estrutura/`. As estruturas e seus testes não foram substituídos nem usados como persistência de auditoria.

## Decisões de escopo

1. Nenhuma implementação de US04/US05/US06. O indicador preexistente foi preservado.
2. `EntregaHospitalar` registra uma entrega **já ocorrida**, atestada explicitamente pelo gestor no formulário. O servidor registra o momento da atestação; não deduz nem fabrica transporte, expedição, localização ou utilização.
3. Uma bolsa atende uma requisição; uma requisição pode receber até a quantidade solicitada de bolsas. Não há substituição automática de bolsa não conforme.
4. Associação limitada a concentrado de hemácias. Aplicar a tabela existente a plasma/plaquetas seria incorreto. Cadastro e requisição dos demais componentes continuam disponíveis como antes.
5. Histórico legado permanece vazio até a primeira nova operação; data antiga de descarte continua na bolsa, sem evento retroativo fabricado.
6. Eventos novos usam `Instant` UTC. A validade continua uma data; US10 avalia a data em UTC. A origem usa perfil e identificador hospitalar declarado, sem afirmar identidade individual autenticada.
7. Interface mantém gradiente vermelho, cartões brancos, menu e páginas anteriores. Nova página de atendimento complementa a navegação.
8. O problema de listagem entre hospitais e a validação de campo transitório precisaram de ajustes nos contratos anteriores. Justificativas, correção e testes estão no tracker.

## Estratégia de homologação

Testes afetados durante desenvolvimento; suíte Maven completa no fechamento; integração PostgreSQL para gatilho, integridade e concorrência; build Java 17 no Docker; Compose isolado e reinício preservando volume; navegação real com dados sintéticos. O relatório final distingue execução local de verificações ainda dependentes do GitHub, Render ou YouTube.
