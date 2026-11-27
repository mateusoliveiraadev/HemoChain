# Evidencias reais da integracao RSD

Coleta em 27/11/2026. Capturas de navegacao real no Grafana/Prometheus, sem montagem ou dados simulados. Horarios abaixo em America/Fortaleza (UTC-3); o manifesto preserva UTC e a URL capturada.

| Captura | Data/hora local |
| --- | --- |
| [hemochain-aplicacao.png](hemochain-aplicacao.png) | 27/11/2026 15:04:02 |
| [hemochain-infra.png](hemochain-infra.png) | 27/11/2026 15:04:07 |
| [hemochain-logs.png](hemochain-logs.png) | 27/11/2026 15:04:12 |
| [loki-explore.png](loki-explore.png) | 27/11/2026 15:04:18 |
| [loki-trace-link.png](loki-trace-link.png) | 27/11/2026 15:04:19 |
| [tempo-correlacao.png](tempo-correlacao.png) | 27/11/2026 15:04:24 |
| [prometheus-targets.png](prometheus-targets.png) | 27/11/2026 15:04:26 |
| [viewer-local.png](viewer-local.png) | 27/11/2026 15:04:33 |
| [jvm-hikari.png](jvm-hikari.png) | 27/11/2026 15:04:35 |
| [metricas-negocio.png](metricas-negocio.png) | 27/11/2026 15:04:38 |
| [infra-containers.png](infra-containers.png) | 27/11/2026 15:04:44 |
| [promql-negocio.png](promql-negocio.png) | 27/11/2026 15:04:46 |

## Dados e verificacoes

- `validacao.json`: sete targets UP, familias de metricas e trace com multiplos spans.
- `actuator-prometheus.txt` e `actuator-endpoints.json`: respostas reais dos endpoints internos.
- `targets.json` e `consultas-dashboards.json`: targets e resultados das consultas.
- `loki.json` e `trace.json`: log e trace reais; dados exclusivamente sinteticos.
- `checagens-finais.json`: 53 consultas com dados/valores finitos e verificacao da carga limitada.
- `testes.json`: sumarios das suites Maven, Docker Java 17 e PostgreSQL.
- `testes.json`: inclui o sumário da regressão de interface anterior; as capturas auxiliares dessa regressão não integram o conjunto mínimo publicado.
- `viewer-local.json`: papel Viewer, sem Grafana Admin; dashboards acessiveis e administracao recusada.
- `prod-smoke.json`: perfil prod conserva health e nao expoe Prometheus/erro simulado.
- `consumo.json`: tres amostras de CPU/RAM e tamanho real dos cinco volumes.
- `indisponibilidade-sinks.json`: startup e leituras com destinos de telemetria indisponiveis em container temporario separado.
- `preservacao.json`: comparacao com as alteracoes locais preexistentes.
- `compose-ps.txt`: estado dos containers da demonstracao.

A consulta de logs pode precisar de ate um minuto para o primeiro lote. As figuras dos dashboards possuem intervalo absoluto com data/hora visivel; conserve as legendas e os horarios do manifesto ao montar o PDF.

**Atualização final:** o Quick Tunnel foi autorizado e validado por HTTPS com o Viewer. Consulte [a evidência remota](../acesso-remoto.md), `acesso-remoto.json`, `disponibilidade-final.json` e `compose-ps-final.txt`. As 12 capturas acima continuam sendo as capturas locais originais. Retenção configurada e aceita pelos serviços; não se afirma que foi observado o expurgo após 72 horas.

A validação da branch de publicação está em `publicacao-validacoes.json`; comandos, isolamento e limites em [publicação e testes](../publicacao.md).
