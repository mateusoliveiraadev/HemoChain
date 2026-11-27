# Branch final, commits e validações

Publicação autorizada pela equipe em 27/11/2026. Branch de entrega: [entrega/rsd-final](https://github.com/mateusoliveiraadev/HemoChain/tree/entrega/rsd-final), preparada em checkout isolado a partir da main `e3ce174`. A edição remota do título da Entrega 04 no README foi preservada; a árvore local anterior e as alterações dos integrantes permaneceram intactas.

## Sete grupos aprovados

1. Reaproveitamento do commit Micro Slats, originalmente `3c3e506`, por cherry-pick.
2. Correções anteriores de enum H2 e invariantes de entrega, com seus testes.
3. Auditoria de 24/11, JaCoCo e cobertura no CI.
4. Atualização anterior do bug tracker da Entrega 03.
5. Instrumentação Actuator/Micrometer, logs e tracing.
6. Stack isolada, exporters, dashboards e scripts operacionais.
7. Documentação e evidências de RSD, incluindo os resultados abaixo.

README e POM foram separados por finalidade. Não foram incluídos o arquivo local de credenciais, estados de autenticação, caches, logs operacionais, ZIP ou target/. Os XMLs arquivados em docs/auditoria/ são evidências históricas da auditoria aprovada, separadas dos artefatos temporários desta validação.

## Validação real da branch

| Verificação | Resultado |
| --- | --- |
| Maven, JVM 21 / release 17 | 100 testes; zero falhas, erros ou ignorados; BUILD SUCCESS |
| PostgreSQL 16 real e isolado | 17 Surefire + 4 Failsafe; zero falhas/erros; BUILD SUCCESS |
| Build Docker / Java 17 | 100 testes aprovados; imagem local construída |
| Container prod | Health 200; /actuator/prometheus 404; erro simulado 404 |
| Interface com Micro Slats | Cadastro, histórico, requisição, FEFO, entrega, recebimento, utilização e alerta após prazo real aprovados; zero erros JavaScript |

Dados completos, comandos e data/hora em [publicacao-validacoes.json](evidencias/publicacao-validacoes.json). Os testes de escrita usaram somente um banco sintético temporário, fora da stack da demonstração. Os relatórios completos e capturas auxiliares desta rodada permanecem em diretórios locais ignorados.

## Disponibilidade e limites da entrega

Grafana e túnel continuam ativos, exclusivamente com destino ao Grafana. O usuário professor é Viewer, sem Grafana Admin. A manutenção de dados recentes usa GET /hospitais a cada dez segundos e um erro controlado por minuto; não cria novos registros de negócio. [Operação e parada](disponibilidade.md).

O fluxo autorizado termina em push da branch e PR Draft para main. Não inclui merge, publicação de imagem nem deploy. A esteira existente só publica imagem em push na main. O PDF final, dados da capa, janela de correção e submissão no Classroom continuam pendentes da equipe. E8 não deve ser apresentado como concluído antes do PDF e link serem entregues.

Revisão de segredos, arquivos, links, preservação e disponibilidade: [publicacao-revisao.json](evidencias/publicacao-revisao.json). As saídas TXT reais conservam os espaços produzidos pelas ferramentas; a verificação de whitespace de código, configuração e documentação editável passou.
