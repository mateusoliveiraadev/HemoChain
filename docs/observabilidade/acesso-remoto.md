# Evidência adicional — acesso remoto como Viewer

Registro técnico real de autenticação e consultas por HTTPS, iniciado em **27/11/2026 17:13:50 (America/Fortaleza, UTC−3)**. Respostas integrais resumidas, sem cookies, senhas ou tokens: `evidencias/acesso-remoto.json`. Este documento é uma evidência de protocolo HTTP; não é uma captura de tela.

URL: https://cached-former-attempts-think.trycloudflare.com

| Verificação | Resultado real |
| --- | --- |
| Health do Grafana na URL pública | HTTP 200 |
| POST /login com credencial do professor | HTTP 200, sessão autenticada |
| GET /api/user | login professor; isGrafanaAdmin=false |
| GET /api/user/orgs | Papel Viewer na organização |
| GET /api/search anônimo | HTTP 401 |
| GET /api/search como Viewer | HTTP 200; três dashboards |
| GET /api/users, /api/org/users, /api/admin/settings | HTTP 403 para o Viewer |
| Dashboards Aplicação, Infraestrutura e Logs/traces | HTTP 200; canEdit=false, canAdmin=false, canSave=false |
| Targets pelo datasource Prometheus | Sete valores UP=1 |
| Consultas dos dashboards | 53 com séries e valores finitos |
| Idade das amostras verificadas | API: 8,45 s; Node Exporter: 0,99 s; cAdvisor/API: 27,73 s |
| /actuator, /actuator/health e /actuator/prometheus | HTTP 404 autenticado; nenhum encaminhamento à API |
| /api/v1/targets, /targets e /-/ready | HTTP 404 autenticado; nenhum encaminhamento ao servidor Prometheus |
| /bolsas, /hospitais e /requisicoes (rotas reais da API) | HTTP 404 autenticado na URL do túnel |
| PostgreSQL | Sem porta publicada; ausente como destino do túnel |
| Destino do cloudflared inspecionado | tunnel --no-autoupdate --url http://grafana:3000 |

O endpoint público `/metrics` é do próprio Grafana, identificado por `grafana_build_info`; não contém o contador de negócio da aplicação. O Grafana permite ao Viewer consultar os datasources por seu proxy autenticado para montar os painéis. Essa consulta não equivale a expor API, banco, Actuator ou porta do Prometheus ao público.

Limite da evidência: as requisições foram enviadas pela máquina anfitriã para o endereço HTTPS público da Cloudflare. Um teste humano em rede independente, como celular em dados móveis, ainda é recomendado e permanece a confirmar. O navegador integrado estava indisponível, e o Chrome estava minimizado sem restauração pela ferramenta; foi solicitada a restauração para coletar uma captura adicional. As 12 capturas locais originais permanecem preservadas. Não há disponibilidade futura comprovada sem a definição e acompanhamento da janela de correção.


## Conferência posterior de logs e traces remotos

Em 27/11/2026 às 17:23:17 (Fortaleza), o Viewer consultou o Loki e o Tempo pela URL pública: 10 linhas de log nos últimos cinco minutos, último registro com 19,1 segundos e trace correspondente disponível (HTTP 200). O trace atual tem um span de leitura/erro controlado; o trace de entrega com 19 spans continua documentado nas figuras 05/06 e no JSON original. Resultado sem credenciais em `logs-remotos-atuais.json`.
