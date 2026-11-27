# Matriz final de atendimento — RSD

Fonte: enunciado oficial de 28/10/2026, atribuído a Luiz Novaes, cópia e hash na pasta final de Downloads. A nota máxima é 7,0; esta matriz registra evidências, não atribui nota.

| Etapa | Valor | Atendimento demonstrado | Evidências para o PDF / repositório | Pendência |
| --- | ---: | --- | --- | --- |
| E1 — Instrumentação com Actuator (R1/R2) | 0,75 | Uma aplicação Java; dependências, health/info/metrics/prometheus internos e tag application; HTTP/JVM/processo/Hikari | actuator-endpoints.json, actuator-prometheus.txt; figuras 01 e 09; pom.xml e perfil observabilidade | Inserir excerto/figuras no PDF; confirmar que é a única aplicação Java |
| E2 — Exporters de infraestrutura (R3) | 0,75 | Node Exporter e cAdvisor ativos, séries presentes e targets UP | Figuras 02, 07 e 11; targets.json; acesso-remoto.json | Explicar no PDF que o host medido é a VM Linux |
| E3 — Stack via Docker Compose (R4) | 0,75 | Comando único para oito componentes obrigatórios; sete jobs UP; datasources provisionados | compose.observabilidade.yaml, compose-ps-final.txt, figura 07, arquivos de provisioning | Arquivos selecionados na branch entrega/rsd-final |
| E4 — Dashboards (R4) | 0,75 | Aplicação, Infraestrutura e Logs/traces; JSONs exportados no repositório local; consultas com dados | Figuras 01–03, 09–11; três JSONs; acesso-remoto.json com consultas atuais | Inserir prints legíveis; JSONs incluídos na branch |
| E5 — Logs com Loki (R6) | 0,75 | Logs da aplicação filtrados por app, sucesso e erro controlado | Figuras 03–05; loki.json; LogQL no material consolidado | Incluir consulta e legenda no PDF |
| E6 — Traces e correlação (R6) | 0,75 | Trace real de 19 spans, HTTP/serviço/JDBC; navegação log → trace pelo mesmo traceId | Figuras 05 e 06; trace.json; provisioning Loki/Tempo | Inserir sequência log → trace no PDF |
| E7 — Acesso remoto (R7) | 0,75 | URL pública HTTPS validada; login professor, Viewer, não Grafana Admin, três dashboards sem edição; APIs administrativas 403 | acesso-remoto.json; registro HTTP autenticado; guia de disponibilidade | Confirmar janela e teste humano em outra rede; manter ambiente e carga durante correção |
| E8 — Repositório e PDF | 0,75 | Organização, README, diagrama, material e revisão local preparados | Pasta final; revisão GitHub e lista dos arquivos RSD; links locais válidos | PDF final e submissão PDF/link no Classroom pendentes; código organizado em branch para PR Draft |
| Adicional — duas métricas de negócio | 1,00 | Counter e Timer Micrometer descritos; Prometheus e dois painéis próprios | Figuras 10 e 12; excerto real do serviço; ObservabilidadeConfig; consultas e TXT de métricas | Incluir tabela, código e figuras no PDF |
| Total possível | **7,00** | Critérios preparados para avaliação | A nota depende do professor | E7 condicionado à disponibilidade; E8 ainda não concluído |

Segurança transversal R5: Actuator e banco sem porta publicada; portas locais em loopback; túnel exclusivo Grafana; admin com senha não padrão; acesso anônimo e cadastro desativados; senhas locais ignoradas. Consultas aos datasources via proxy autenticado do Grafana permanecem disponíveis para leitura dos painéis.

O período futuro de disponibilidade não pode ser comprovado por uma captura de hoje. O PDF e o link do repositório precisam ser entregues na atividade da disciplina para concluir E8.
