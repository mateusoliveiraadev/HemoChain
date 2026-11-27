> **Atualização após aprovação da equipe:** os sete grupos foram autorizados e separados na branch `entrega/rsd-final`, em checkout isolado. A edição remota da Entrega 04 no README foi preservada. Consulte [publicação e testes](publicacao.md). O texto abaixo registra a preparação anterior à aprovação.

# Revisão final e separação para GitHub — RSD

Repositório origin: https://github.com/mateusoliveiraadev/HemoChain. Visibilidade **pública**, confirmada pelo conector GitHub em 27/11/2026. Branch local em uso: `feature/internal-micro-slats`; a equipe deve confirmar qual branch/commit será a entrega. Nenhuma publicação foi executada.

## Seleção da observabilidade

Arquivos exclusivos de RSD: `compose.observabilidade.yaml`, `.env.observabilidade.example`, `observabilidade/`, `docs/observabilidade/` (exceto os itens de apoio local abaixo), `src/main/java/com/hemochain/config/ObservabilidadeConfig.java`, `src/main/java/com/hemochain/controller/ErroSimuladoController.java`, `src/main/resources/application-observabilidade.properties`, `src/main/resources/logback-spring.xml` e `src/test/java/com/hemochain/observabilidade/ObservabilidadeTest.java`.

Nos arquivos existentes, pertencem a RSD: proteção dos arquivos locais em `.dockerignore`; exceção para o exemplo em `.gitignore`; bloco final “Observabilidade” no `README.md`; dependências de observabilidade no `pom.xml`; anotações Micrometer e logs em `AtendimentoHospitalarService.java`; desligamento padrão de tracing/JDBC proxy em `application.properties`.

O README e o POM são arquivos compartilhados: selecionar somente os hunks de RSD na futura preparação do commit. O JaCoCo 0.8.14 e suas execuções padrão/PostgreSQL no POM já existiam antes da integração e pertencem à auditoria anterior. O conteúdo anterior do README foi preservado. A pasta final em Downloads contém `ARQUIVOS-RSD.txt`, `MANIFESTO-ARQUIVOS-RSD.json` e `PATCH-CODIGO-RSD.patch`; o patch usa o baseline anterior para README/POM e não inclui os trechos das outras entregas. Os arquivos novos e evidências estão listados individualmente no manifesto. O patch não inclui binários nem os documentos/evidências completos: estes são identificados pela lista e pelos hashes.

## Preservar e deixar fora da seleção RSD

- `.github/workflows/ci-cd.yml`, `entrega-03/bug-tracker.png`, `src/main/java/com/hemochain/config/EvolucaoStatusBolsaH2.java`, `src/main/java/com/hemochain/entity/EntregaHospitalar.java` e `src/test/java/com/hemochain/config/EvolucaoStatusBolsaH2Test.java`: alterações anteriores dos integrantes, preservadas por comparação de hashes com o baseline da integração.
- `docs/auditoria/`, `scripts/auditoria-metricas.py` e `src/test/java/com/hemochain/entity/`: auditoria/entregas anteriores, sem inclusão automática nesta entrega RSD.
- `compose.yaml` e `Dockerfile`: preservados; a demonstração usa seu próprio Compose e reutiliza o build existente.
- `docs/observabilidade/evidencias/regressao-ui/`, `targets-encerramento.json` e `.gitkeep`: apoio local redundante, dispensado do conjunto mínimo RSD. Os sumários relevantes dos testes ficam em `testes.json`.

Não usar `git add .` em uma árvore com entregas misturadas. Após a autorização futura para commit/push, usar a lista nominal e seleção de hunks nos arquivos compartilhados. Não foi alterado o índice Git nesta preparação.

## Segredos, temporários e evidências

A varredura de arquivos candidatos não encontrou as senhas reais do `.env.observabilidade` nem padrões reconhecidos de tokens GitHub/Slack/AWS e chaves privadas. A busca não equivale a uma auditoria de todo o histórico Git. Credenciais fictícias dos testes não foram tratadas como senhas reais. `.env.observabilidade`, `.codex/`, `target/`, logs e estados de autenticação permanecem ignorados. Apenas o exemplo com placeholders foi selecionado; a senha do Viewer não aparece em nenhum arquivo da pasta final.

As 12 imagens originais foram inspecionadas visualmente, sem senhas/tokens visíveis, e copiadas com igualdade de SHA-256. A captura de Targets e a consulta PromQL não mostram relógio próprio na página; o manifesto registra o horário de captura. Na montagem do PDF, inserir esse horário na legenda e manter os arquivos originais; não apresentar uma legenda como se fosse relógio nativo da tela. As demais figuras mostram intervalos, logs ou trace datados. As saídas de Compose/Actuator são respostas reais de terminal/TXT, permitidas pelo enunciado.

Os links locais do README e da documentação RSD foram verificados. O link do repositório foi confirmado pelo GitHub. Também foram feitas verificações HTTP dos links externos do README, registradas em `links-readme-final.json`, com 26 endereços, incluindo exemplos locais. Os endereços Render e o protótipo histórico Figma não tiveram confirmação automática (timeout/404); pertencem a outras entregas e foram preservados. Resposta HTTP 200 não comprova o conteúdo dos vídeos/designs. Esses serviços externos não são dependências da validação RSD. Os parâmetros temporários de redirecionamento foram omitidos do relatório. Após publicar, conferir no GitHub o README, o Compose, os JSONs dos dashboards e as evidências da branch/commit informado no PDF.

## Resultado

Nenhuma regra de negócio foi refatorada nesta finalização. Foram feitas somente validação do túnel autorizado, geração de dados sintéticos limitados, atualização das afirmações de estado na documentação e organização do material da entrega. Os testes Maven/PostgreSQL/UI da integração anterior foram preservados como evidências; não houve mudança de código funcional que exigisse repetição dessas suítes.

Sem commit, push, merge ou deploy. E8 depende do PDF e da publicação autorizada. E7 depende da permanência do ambiente e da carga na janela de correção a confirmar. Evidência técnica do acesso remoto: [acesso remoto](acesso-remoto.md). Matriz: [atendimento final](matriz-final.md).
