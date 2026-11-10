# Evidências — Entrega 04

Capturas reais da aplicação local com dados sintéticos, geradas pelo script versionado `scripts/entrega04-ui.cjs`. Não são mockups, imagens do GitHub ou prova de deploy desta versão no Render.

| Arquivo | Conteúdo |
|---|---|
| `01-estoque-historico.png` | Cadastro/validade e evento inicial |
| `02-requisicao-hospitalar.png` | Requisição criada pelo hospital |
| `03-sugestao-fefo.png` | Bolsa compatível na seleção manual |
| `04-entrega-atestada.png` | Associação explícita e entrega registrada |
| `05-recebimento-integro.png` | Condição e estado RECEBIDA |
| `06-utilizacao.png` | Estado UTILIZADA |
| `07-historico-final.png` | Eventos cronológicos do fluxo completo |
| `08-alerta-pendente.png` | Prazo de 5 segundos ultrapassado e alerta real |
| `fluxo-ui.json` | IDs/código sintéticos e eventos retornados pela API |
| `resultado-ui.json` | Resultado do teste de interface e erros JavaScript |
| `bug-tracker.png` | Captura de tracker **local**, não GitHub Issues |
| `persistencia.json` | Comparação antes/depois de parar e recriar containers, preservando volume |
| `testes-maven-java17.txt` | Resumo extraído do build Docker real, suíte completa |
| `testes-postgresql.txt` | Resumo real de aceitação/rollback e integração PostgreSQL |
| `validacao-final.json` | Verificações finais de build, interface, operação e Git |

O prazo de cinco segundos é exclusivo do ambiente de demonstração; o padrão é 24 horas. O script usa Edge instalado e Playwright. Para reproduzir sem dependências Java novas, instalar Playwright numa pasta local ignorada e definir `PLAYWRIGHT_MODULE` com o caminho do módulo. Usar `HEMOCHAIN_URL` para a API e `HEMOCHAIN_UI_PENDENCIA=true` apenas com o prazo reduzido configurado. Nunca apontar esse script para produção.

Resumo de execução e persistência: ver `validacao-final.json` quando gerado e o [relatório de homologação](../homologacao.md). A captura do tracker representa os problemas descritos em `entrega-04/bug-tracker.md`, com indicação de publicação externa pendente.
