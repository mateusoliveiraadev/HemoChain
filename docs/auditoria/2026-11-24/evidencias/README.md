# Evidências executadas em 24 de novembro de 2026

Estas evidências pertencem ao checkout local `3c3e506` com as três alterações anteriores do squad identificadas em `git-inicial.txt`/`hash-preexistentes.txt`. Não representam um commit ou workflow remoto novo. Todas as massas de teste utilizadas são sintéticas.

## Arquivos e origem

| Arquivo ou diretório | Origem e finalidade |
|---|---|
| `baseline-jacoco.xml` | JaCoCo antes de alterar código/POM; denominador real 833 linhas/273 branches/277 métodos |
| `final-jacoco.xml` | Suíte completa final; denominador real 847 linhas/293 branches/277 métodos |
| `baseline-surefire/` | XMLs JUnit das 18 suites padrão iniciais, 84 invocações |
| `final-surefire/` | XMLs JUnit das 19 suites padrão finais, 98 invocações |
| `tdd-red.xml`, `tdd-red.txt` | Primeiro RED: 12 casos, 9 falhas por exceção esperada não lançada |
| `tdd-green.xml`, `tdd-green.txt` | Primeiro GREEN: 12 casos aprovados |
| `tdd-red-2.xml`, `tdd-red-2.txt` | Segundo RED: 14 casos, 2 falhas por instante nulo aceito |
| `tdd-green-2.xml`, `tdd-green-2.txt` | Segundo GREEN: 14 casos aprovados |
| `tdd-red-hashes.txt`, `tdd-red-2-hashes.txt` | Hashes da especificação, teste e produção antes de cada GREEN; a especificação recebeu emendas posteriores identificadas no texto |
| `EntregaHospitalar-antes.java.txt` | Snapshot de produção anterior aos dois ciclos, correspondente à entidade no HEAD inicial |
| `postgres-surefire/`, `postgres-failsafe/` | Última execução de 17 cenários selecionados e 4 IT em PostgreSQL 16 |
| `postgres-selecao-jacoco.xml`, `postgres-it-jacoco.xml` | Cobertura separada dos dois grupos PostgreSQL; não é agregada ao resultado da suíte padrão |
| `postgres-schema.txt` | Versão 16.15, índices, constraints e gatilhos consultados no banco sintético isolado |
| `metricas.json` | Totais, contadores por pacote/classe e mapeamento dos contextos, gerados por `scripts/auditoria-metricas.py` |
| `baseline-maven.txt`, `final-maven.txt`, `postgres-maven.txt` | Logs completos das execuções locais correspondentes |
| `docker-final.txt` | Build final em Linux/Java 17 com 98 testes e Compose saudável |
| `resultado-ui.json`, `fluxo-ui.json`, `ui-test.txt` | Roteiro existente `scripts/entrega04-ui.cjs` no projeto isolado, sem modo PT5S |
| `persistencia.json`, `historico-antes.json`, `historico-depois.json`, `entrega-antes.json`, `entrega-depois.json` | Comparação real após down sem `-v` e up --wait, preservando o volume |
| `compose-restart.txt`, `compose-status.txt`, `health.json` | Operação de recriação, estado saudável observado e resposta de saúde |
| `yaml-validacao.txt` | Parse de YAML com chaves únicas para CI, rollback e Compose; não substitui execução no GitHub |
| `manifesto.json` | Hashes dos arquivos de evidência e fontes alteradas, identificação do checkout e preservação dos arquivos anteriores |

Os arquivos XML incluem propriedades do ambiente Java e saída dos testes. Não há dados reais de pacientes. Logs extensos, HTML JaCoCo, imagens UI e ferramentas temporárias também permanecem em `target/auditoria/`/`target/site/`, ignorados pelo Git. Os arquivos copiados para esta pasta são novos e ainda não foram commitados.

## Comandos efetivamente usados

Baseline funcional anterior às mudanças:

```powershell
.\mvnw.cmd -B verify
# exit 0; 84 testes, 0 falhas/erros/ignorados

.\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.14:report
# exit 0; mesmos 84 testes, sem modificar POM ou producao
```

Cada ciclo TDD usou o comando abaixo primeiro antes e depois da guarda correspondente. A primeira versão do teste tinha 12 invocações; a segunda, 14.

```powershell
.\mvnw.cmd -B '-Dtest=EntregaHospitalarTest' test
# RED 1 exit 1: 9 falhas de assercao; GREEN 1 exit 0
# RED 2 exit 1: 2 falhas de assercao; GREEN 2 exit 0

.\mvnw.cmd -B verify
# ultima execucao exit 0: 98 testes e relatorio JaCoCo
```

Perfil PostgreSQL, com variáveis locais apenas para o banco sintético isolado:

```powershell
$env:SPRING_PROFILES_ACTIVE='postgres-test'
$env:HEMOCHAIN_TEST_DB_URL='jdbc:postgresql://127.0.0.1:15495/hemochain_auditoria'
$env:HEMOCHAIN_TEST_DB_USER='hemochain_auditoria'
# HEMOCHAIN_TEST_DB_PASSWORD foi definido somente no processo de teste.
.\mvnw.cmd -B -Ppostgres-it '-Dtest=AtendimentoHospitalarAceitacaoTest,RastreabilidadeTransacaoTest' verify
# exit 0: 17 Surefire + 4 Failsafe
```

Compose: `target/auditoria/compose.env` contém exclusivamente configuração sintética da auditoria; o override publica PostgreSQL somente em `127.0.0.1:15495`. API em 18095. Não foi sobrescrito `.env` do projeto nem alterado o Compose versionado.

```powershell
docker compose --env-file target/auditoria/compose.env -p hemochain-auditoria-20261124 -f compose.yaml -f target/auditoria/compose.override.yaml config --quiet
docker compose --env-file target/auditoria/compose.env -p hemochain-auditoria-20261124 -f compose.yaml -f target/auditoria/compose.override.yaml up -d --build --wait --wait-timeout 240
# exit 0; Dockerfile executou 98 testes com JDK 17

# Exclusivamente no projeto isolado, depois de confirmar o label com.docker.compose.project:
docker compose --env-file target/auditoria/compose.env -p hemochain-auditoria-20261124 -f compose.yaml -f target/auditoria/compose.override.yaml down
docker compose --env-file target/auditoria/compose.env -p hemochain-auditoria-20261124 -f compose.yaml -f target/auditoria/compose.override.yaml up -d --wait --wait-timeout 120
# sem -v; historico e entrega identicos, bolsa UTILIZADA e health UP
```

Interface: Playwright e parser YAML foram instalados apenas em `target/auditoria/ui-tools`, sem alterar dependências do projeto. A versão de cada ferramenta consta no manifesto.

```powershell
$env:PLAYWRIGHT_MODULE=(Resolve-Path target/auditoria/ui-tools/node_modules/playwright).Path
$env:HEMOCHAIN_URL='http://localhost:18095'
$env:HEMOCHAIN_UI_OUTPUT=(Join-Path (Get-Location) 'target/auditoria/ui')
$env:HEMOCHAIN_UI_PENDENCIA='false'
node scripts/entrega04-ui.cjs
# exit 0; fluxo principal aprovado, zero erros JavaScript
```

Regerar as tabelas preservadas:

```powershell
python scripts/auditoria-metricas.py docs/auditoria/2026-11-24/evidencias
```

O `verify` PostgreSQL sobrescreve o relatório Surefire com sua seleção de testes. Para manter acessível o HTML da suíte completa final, ele foi regenerado sem reexecutar testes:

```powershell
.\mvnw.cmd -B org.jacoco:jacoco-maven-plugin:0.8.14:report '-Djacoco.dataFile=target/auditoria/final-jacoco.exec'
```

## Limites e ocorrências de execução

- Os primeiros logs finais foram interrompidos pela reinicialização do servidor de ferramentas; houve nova execução completa verde. O log parcial não é contabilizado como teste concluído.
- Uma tentativa de validar o label com Go template falhou no quoting do PowerShell e foi abortada antes do down. A validação foi feita por JSON de `docker inspect`. Uma tentativa posterior sofreu tratamento indevido do stderr de progresso do Docker pelo PowerShell; o ambiente isolado foi restaurado e a comparação completa repetida com códigos de saída verificados. `persistencia.json` e os snapshots pertencem à operação concluída.
- A mensagem SQL de violação de FK no log PostgreSQL é esperada por `assertThrows` no IT de imutabilidade; resultados finais têm zero erros.
- A baseline PostgreSQL antes da alteração não foi executada. Há execução real posterior dos testes existentes; não se atribui a ela uma comparação antes/depois inexistente.
- YAML válido, comandos Maven/Docker locais aprovados e evidências antigas do GitHub não provam que o novo diff passou no workflow remoto. Não houve push, merge ou deploy.
- O compilador C não está no PATH; o código C foi inspecionado, mas seus asserts não compõem os números Java.
- Ao finalizar, os serviços do projeto isolado foram parados com `docker compose ... stop`, sem remover containers ou volume. `compose-status.txt`/`health.json` registram o estado saudável anterior à parada; `compose-stop.txt` registra o encerramento. Os containers do squad continuaram ativos.
