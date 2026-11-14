# P9.1 — logout corrigido; branding aguardando asset oficial

## Estado da entrega

P0 implementado e validado na aplicação Spring real e no Docker local em `http://localhost:18084`. P1 foi inspecionado na aplicação real, mas **não está concluído**: a remoção tentada do fundo alterou o acabamento da tipografia. O resultado foi rejeitado, conforme a exigência de preservar integralmente a identidade aprovada. Nenhuma imagem gerada foi integrada.

## Diagnóstico e causa do retorno antigo

Antes da alteração, `/login.html` já servia o HTML Premium Dark em uma navegação limpa. `/` e `/index.html` serviam o início interno; `/login` e `/logout` não eram endpoints existentes (404). Todas as quatro páginas internas tinham uma única opção visível Sair no Dock, com listener compartilhado em `internal-ui.js`. Existia também a função pública legada `sair()` no início. Não havia um segundo destino legítimo de logout nem filtro de Spring Security gerando outro login.

Os recursos eram entregues sem `Cache-Control`. O JAR efetivamente presente no container continha o login branco/vermelho antigo; o conteúdo novo era fornecido por um bind mount de preview com prioridade sobre o classpath. Essa divergência tornava a atualização dependente do preview, sem atualizar o artefato executável.

A reprodução controlada utilizou o JAR antigo, uma cópia descartável dos arquivos estáticos e H2 em memória em `127.0.0.1:18087`. Primeiro o navegador recebeu o login antigo, com Last-Modified anterior, sem política de cache. Depois o arquivo foi atualizado e o login novo foi aberto com uma query de preview. O cadastro e login usaram a API real. Ao sair para o caminho canônico, o navegador exibiu novamente o login antigo, **do cache de disco**, mesmo com o arquivo novo no servidor e a sessão local apagada. `cache-repro.json` registra `legacyReturned: true`, `fromDiskCache: true` e `sessionCleared: true`.

Essa reprodução demonstra o mecanismo; não é uma inspeção do cache do navegador pessoal do usuário. As capturas [antes](legacy-after-logout.png) e [depois](fixed-after-logout.png) mostram o mesmo fluxo com e sem a correção.

## Correção implementada

- `src/main/resources/application.properties`: `Cache-Control: no-store` para os recursos estáticos; APIs e modelo de autenticação preservados.
- `src/main/resources/static/js/auth-session.js`: saída única para `/login.html`, remoção da chave `hemochainUsuario`, atualização explícita do HTML canônico com `fetch`/`cache: reload` para migrar caches antigos, timeout de cinco segundos e navegação com `location.replace`.
- `index.html`, `estoque.html`, `requisicoes.html`, `atendimento.html`: guarda inicial compartilhada, revalidação no `pageshow` para histórico/BFCache e reação à saída em outra aba. As guardas já existentes usam o mesmo mecanismo.
- `js/internal-ui.js` e a função legada `sair()`: delegação à mesma saída.
- `scripts/p9-1-cache-repro.cjs`, `scripts/p9-1-ui.cjs`, `scripts/p9-1-docker-smoke.cjs`: reprodução e regressões direcionadas.

O Docker local foi recompilado e somente o serviço `api` foi recriado. O bind mount de preview foi removido da instância local: agora o próprio JAR inclui o login novo e os recursos atuais. O banco e seu volume foram preservados. O arquivo de autenticação empacotado foi comparado com o fonte. Não houve push, merge nem deploy remoto.

A aplicação acadêmica usa usuário em localStorage, não uma sessão de servidor ou token revogável. Encerrar a sessão, nesta entrega, significa apagar essa chave e impedir o retorno às telas internas pelo mecanismo de navegação existente. A correção não introduz autenticação de servidor nem modifica permissões da API.

## Validação real

- `docker build -t hemochain-entrega04-api .`: Maven/Java 17, **83 testes, zero falhas, zero erros**, BUILD SUCCESS. Inclui testes de usuário e autenticação.
- Cadastro, senha incorreta, login por Enter, navegação e logout com os três perfis, em desktop 1440×1000, tablet 834×1112 e mobile 390×844: 27 combinações de viewport/perfil/página de saída, com API real e H2 descartável. Nenhum cadastro foi feito no PostgreSQL existente.
- Saída pelos quatro Docks, função `sair()`, botão Voltar, acesso direto às quatro páginas após sair e sincronização entre abas.
- Reprodução com cache antigo já preenchido: com a correção, `legacyReturned: false` e a mesma URL canônica passa a renderizar o login atualizado (`cache-fixed.json`). Não se considerou apenas a mudança de URL.
- Docker/PostgreSQL real em 18084: login com conta sintética preexistente, navegação pelos quatro módulos, logout e tentativa de entrada pela raiz nas três resoluções. Resultado PASS, sem erros JavaScript (`resultado-docker.json`).
- Liquid Ether: movimento real do mouse altera os dados de velocidade enviados à GPU; erro WebGL igual a zero. Magnetic dos dois botões e interrupção das atualizações com movimento reduzido também são verificados. Os arquivos desses efeitos não foram alterados.

Capturas do runtime Docker:

- [Login desktop](docker-login-desktop.png).
- Módulos: [início](docker-modulo-index.png), [estoque](docker-modulo-estoque.png), [requisições](docker-modulo-requisicoes.png), [atendimento](docker-modulo-atendimento.png).
- Retorno após Sair: [desktop](docker-logout-desktop.png), [tablet](docker-logout-tablet.png), [mobile](docker-logout-mobile.png).

## Branding: limitação encontrada

Foram examinados os dois arquivos oficiais em Downloads e as cópias do projeto. Ambos os originais têm canal alfa. Isso, porém, não torna a composição do nome um wordmark isolado: o PNG inclui fundo vinho, ondas externas, brilho separador e sublinhado. A cópia integrada conserva esses elementos, formando o banner observado no card. Dimensões, contagem de transparência e hashes estão em `branding-inspection.json`.

Foi tentada uma extração com a ferramenta ImageGen, usando o arquivo oficial como referência e solicitando apenas remoção de fundo, sem redesenho. A comparação visual detectou alterações no contorno/acabamento das letras; a saída foi rejeitada e não faz parte do projeto. Não foram usados blend modes, filtros, máscaras CSS ou substituição da tipografia por texto aproximado.

Para concluir P1 é necessário o arquivo oficial do nome HemoChain, **com transparência real e sem as ondas/fundo decorativo**, ou o arquivo vetorial/em camadas original. O caminho local desse asset foi solicitado ao usuário. O símbolo isolado já possui transparência aproveitável. Os assets aprovados e a aparência atual foram mantidos até que seja possível fazer a integração fiel.

## Execução dos scripts

Os scripts aceitam `PLAYWRIGHT_MODULE` apontando para uma instalação externa do Playwright e usam Microsoft Edge headless. `p9-1-ui.cjs` aceita `HEMOCHAIN_URL`/`HEMOCHAIN_UI_OUTPUT` e exige banco descartável: ele cria usuários sintéticos. O padrão é `127.0.0.1:18086` com H2 em memória. `p9-1-docker-smoke.cjs` usa somente uma conta sintética preexistente em 18084 e não cadastra nem altera dados. A reprodução de cache requer o runtime legado e a cópia descartável de estáticos preparados em `target/p9-1-evidence/legacy-preview`, servidos em 18087; `--fixed` seleciona o logout corrigido. Capturas, vídeo e logs completos intermediários permanecem em `target/p9-1-evidence`.
