# Observabilidade do HemoChain ? RSD

Integracao adaptada do patch da equipe, validada localmente em 27/11/2026. Spring Boot 3.3.4, Java 17 e PostgreSQL 16. Os arquivos originais do integrante foram aproveitados, especialmente Compose, dashboards, provisionamento, Loki, Tempo e instrumentacao JDBC.

## Preparar o ambiente (PowerShell)

Requisitos: Docker Desktop com containers Linux e WSL2, Compose v2 ou posterior e internet para baixar imagens/dependencias no primeiro build. Python 3 e necessario somente para o validador opcional; os testes Maven locais usam JDK 17 ou posterior. A VM desta maquina tem aproximadamente 7,7 GiB disponiveis; os limites dos servicos da demonstracao somam cerca de 3 GiB. O consumo medido esta em `evidencias/consumo.json`. Nao ha mudancas globais no Docker/WSL.

Execute na raiz do repositorio:

```powershell
Set-Location D:\Desenvolvimento\Projetos\HemoChain
# Somente se o arquivo ainda nao existir; nao sobrescreva credenciais existentes:
Copy-Item .env.observabilidade.example .env.observabilidade
notepad .env.observabilidade
```

Substitua os dois placeholders por senhas fortes diferentes. `.env.observabilidade` e ignorado pelo Git; somente o exemplo pode ser publicado. Nesta integracao, o arquivo local ja foi criado com senhas aleatorias. Nao copie seu conteudo para prints, issues ou GitHub. O Grafana armazena a senha no volume: mudar apenas a variavel posteriormente nao equivale a trocar a senha existente.

Confirme o gateway Linux da VM:

```powershell
docker network inspect bridge --format '{{range .IPAM.Config}}{{.Gateway}}{{end}}'
```

O resultado validado foi `172.17.0.1`. Se for diferente, ajuste `OBS_VM_GATEWAY` no arquivo local. O Node Exporter usa a rede da VM e escuta somente nesse endereco; nao usa `host.docker.internal`, que nesta instalacao aponta para o lado Windows.

## Subir os oito componentes obrigatorios

```powershell
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml up -d --build
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml ps
```

Este e um unico comando Compose para a stack obrigatoria. Aguarde a API ficar healthy e o Prometheus realizar duas ou mais coletas (intervalo de 15s). Loki pode levar aproximadamente um minuto para mostrar o primeiro lote de logs.

O projeto Compose chama-se `hemochain-observabilidade`. Seus volumes e banco sao independentes do `compose.yaml` original. O Compose de trabalho, CI/CD e configuracao Render permanecem preservados. Nao altere `name`, nem aponte o datasource para um banco de trabalho/producao.

| Servico | Acesso local padrao | Interno |
| --- | --- | --- |
| HemoChain | http://127.0.0.1:18086 | api:8080 |
| Grafana | http://127.0.0.1:13000 | grafana:3000 |
| Prometheus | http://127.0.0.1:19090/targets | prometheus:9090 |
| Actuator | sem porta publicada | api:8081 |
| PostgreSQL | sem porta publicada | db:5432 |
| Loki | sem porta publicada | loki:3100 |
| Tempo | sem porta publicada | tempo:3200; OTLP HTTP:4318 |
| Node Exporter | endereco Linux interno da VM | docker-vm:9100 |
| cAdvisor | sem porta publicada | cadvisor:8080 |

As portas locais podem ser ajustadas por `OBS_API_PORT`, `OBS_GRAFANA_PORT` e `OBS_PROMETHEUS_PORT`. O Actuator esta acessivel aos servicos da rede Docker de demonstracao e aos administradores do Docker, nao exclusivamente ao Prometheus. Nao ha encaminhamento do Actuator pelo tunel.

## Gerar dados sinteticos e trafego leve

```powershell
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml --profile carga up -d carga
```

Padroes: uma VU, 30 minutos, 12 fluxos de escrita e uma leitura a cada 5s. Cada fluxo cria uma bolsa, uma requisicao e uma entrega, confirma recebimento e uso e testa uma recusa 409. Depois do teto, continuam apenas leituras e um erro simulado 500 a cada 12 iteracoes. O gerador termina e nao reinicia automaticamente.

Configuracoes locais: `OBS_CARGA_DURACAO` (1m..60m), `OBS_CARGA_ESCRITAS` (1..30 fluxos) e `OBS_CARGA_INTERVALO` (2..60s). Reiniciar a carga cria outro lote limitado. Nao e necessario reiniciar para manter as metricas de negocio visiveis: os paineis mostram contagem e media acumuladas desde o inicio da API.

O script rejeita destinos externos: somente `api:8080` e os enderecos locais de demonstracao previstos sao aceitos. Todas as operacoes devem usar dados sinteticos. O endpoint `/observabilidade/erro-simulado` existe somente no perfil adicional `observabilidade-demo`, ativado pelo Compose isolado, cuja API fica no loopback.

Para repetir deliberadamente um lote apos o gerador terminar:

```powershell
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml --profile carga up -d --force-recreate carga
```

## Duas metricas de negocio (+1,0)

| Nome Prometheus | Tipo | Descricao e painel |
| --- | --- | --- |
| `hemochain_entrega_tentativas_total` | counter | Execucoes de `registrarEntrega`, com `result=success/failure`, `exception`, `class`, `method` e `application`. Painel proprio: tentativas acumuladas |
| `hemochain_entrega_registro_seconds_count/sum/max` | timer | Duracao das tentativas de registrar entrega, incluindo recusas; painel proprio de media acumulada |

Sao tentativas do metodo, nao fatos confirmados por commit. Erros de validacao/perfil rejeitados antes de entrar no service nao sao contados. Nao se soma o count do timer ao counter como eventos adicionais. O gauge de estoque e os outros contadores opcionais foram dispensados. Nenhum cache, agendador ou listener apos commit foi acrescentado.

```promql
sum by (result) (hemochain_entrega_tentativas_total{application="hemochain"})
sum(hemochain_entrega_registro_seconds_sum{application="hemochain"}) / sum(hemochain_entrega_registro_seconds_count{application="hemochain"})
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{application="hemochain"}[5m])))
```

## Dashboards, logs e traces

A pasta HemoChain recebe automaticamente os dashboards Aplicacao, Infraestrutura e Logs e traces. Os JSONs ficam em `observabilidade/grafana/dashboards`; os tres datasources sao provisionados. Se editar um dashboard pela UI, exporte o JSON para preservar a mudanca no repositorio.

A API envia logs pelo Loki4j e traces OTLP para o Tempo. As tentativas de entrega emitem logs com `traceId` e `spanId`. O fluxo validado tem span HTTP, `registrar-entrega` e spans JDBC (19 spans no exemplo capturado). Nem todo endpoint executa esse service.

No Explore, selecione Loki:

```logql
{app="hemochain"}
{app="hemochain", level="ERROR"}
{app="hemochain"} |= "Registro de entrega processado"
```

Abra uma linha de entrega e clique em **Abrir trace no Tempo**. O campo derivado deve abrir o trace correspondente. A consulta inversa Tempo -> Loki tambem esta configurada. Se a carga terminou ha mais de 30 minutos, amplie a janela para incluir o horario em que o lote foi gerado. Logs de startup podem nao ter IDs; isso e esperado fora de um trace ativo.

## Docker Desktop e armazenamento

Node Exporter mede a VM Linux do Docker Desktop, nao o Windows fisico. Isso e expressamente aceito pelo enunciado e deve constar do PDF. O painel de disco usa os mounts Linux dos dados Docker; a capacidade virtual do VHDX nao equivale ao espaco livre fisico da unidade C:.

O cAdvisor original 0.49.1 ficava UP, mas falhava ao identificar containers com o armazenamento overlayfs/containerd desta instalacao. Foi adaptado para 0.55.1. Apenas labels de projeto/servico sao coletadas, e o Prometheus conserva as series dos containers de `hemochain-observabilidade`. O cAdvisor exige mounts sensiveis e privilegios de leitura do host; nao exponha sua porta nem use seus dados como prova de isolamento do daemon.

Retencao: Prometheus 3 dias/512 MB de blocos (WAL/head podem ocupar espaco adicional), Loki 72h com Compactor habilitado, Tempo 48h. Logs Docker: 5 MB x 2 arquivos por container. Retencao temporal nao e quota rigida de disco. O banco cresce somente com lotes explicitamente iniciados e validacoes, sem limpeza automatica de historico.

## Viewer e acesso remoto

No Grafana, entre como administrador usando as credenciais do arquivo local. Em **Administration -> Users and access -> Users**, crie o usuario do professor e confirme o papel **Viewer** na organizacao e ausencia de privilegio Grafana Admin. E suficiente fazer isso manualmente. Um Viewer local foi criado nesta validacao; suas credenciais ficam somente no arquivo local ignorado.

O acesso anonimo e o cadastro publico estao desabilitados. O acesso externo foi autorizado e validado em 27/11/2026. O comando abaixo mantém o Quick Tunnel com destino exclusivo ao Grafana:

```powershell
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml --profile remote up -d tunel
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml logs tunel | Select-String trycloudflare
```

O Quick Tunnel encaminha exclusivamente para `grafana:3000`. Nao encaminhe API, Actuator, Prometheus, Loki ou Tempo. A URL pública e o Viewer foram validados por HTTPS; confira [os resultados reais](acesso-remoto.md). Confirme também o acesso em outra rede antes da correção. Envie URL, credenciais e periodo de disponibilidade somente no canal autorizado da disciplina. Nao publique credenciais no GitHub.

A maquina precisa permanecer ligada, sem suspensao, com Docker Desktop e internet ativos durante a correcao. A URL pode mudar apos reinicio/recriacao do tunel. Apos reboot: inicie o Docker Desktop, execute o comando de subida, confira targets, inicie um lote limitado se necessario suba o tunel e confira a nova URL. A autorização deste ambiente já foi concedida. Nao ha garantia de disponibilidade permanente.

## Validar e parar com seguranca

```powershell
python observabilidade/scripts/validar.py
# Endpoint administrativo lido de dentro do container:
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml exec -T api wget -qO- http://localhost:8081/actuator/prometheus | Select-Object -First 30
# Logs de diagnostico:
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml logs --tail 50 api loki tempo
# Parar somente esta demonstracao, preservando volumes:
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml --profile carga --profile remote stop
```

O validador consulta os sete targets, metricas, consultas PromQL, logs e um trace real; grava JSON/TXT sem credenciais. Execute apos a carga e aguarde o primeiro lote de logs. Ele falha se os requisitos consultados nao estiverem presentes. Capturas reais estao indexadas em `evidencias/README.md` com horarios no manifesto.

Regressao: `./mvnw.cmd -B verify` (100 testes na integracao) e Dockerfile Java 17. O perfil `postgres-it` exige banco de teste isolado e as variaveis ja documentadas nas entregas anteriores; nao aponte para o banco de trabalho. Os 21 cenarios PostgreSQL e a regressao de interface foram executados nesta integracao.

O perfil padrao/prod continua expondo apenas health. O Compose de demonstracao usa 8081 internamente; o `compose.yaml`, o workflow CI/CD e o Dockerfile originais conservam health em 8080/PORT para o Render. As alterações foram separadas na branch `entrega/rsd-final`, com a edição remota do README preservada. A validação da branch está em [publicação e testes](publicacao.md). Merge e deploy não fazem parte desta entrega autorizada.

Consulte [o relatorio da integracao](relatorio-integracao.md) e [o material para o PDF](material-pdf.md) para resultados e pendencias. O PDF final continua pendente; a validação técnica do acesso externo está em [acesso remoto](acesso-remoto.md). A [matriz final](matriz-final.md) e a [revisão GitHub](revisao-github.md) registram as pendências.


## Manter a janela de correção sem novos cadastros

Para disponibilidade prolongada, o k6 foi substituído por um processo Python que executa somente GET a cada dez segundos, mais um erro controlado por minuto. Ele não cria registros de negócio. Rode `python observabilidade/scripts/manter-leituras.py` na raiz do projeto; [operação e parada](disponibilidade.md). O script k6 anterior permanece somente para lotes deliberados. O trabalhador local foi deixado ativo nesta validação.
