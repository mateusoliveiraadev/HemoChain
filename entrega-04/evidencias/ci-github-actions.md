# GitHub Actions — Entrega 04

Execuções remotas concluídas em 10/11/2026 para o commit `9b379eb421008bcd92bca1234be781fd2afa3f06`:

- [Push da branch — aprovado](https://github.com/mateusoliveiraadev/HemoChain/actions/runs/42755213594).
- [Pull request — aprovado](https://github.com/mateusoliveiraadev/HemoChain/actions/runs/42755395418).
- [Checks atuais do PR Draft #8](https://github.com/mateusoliveiraadev/HemoChain/pull/8/checks), incluindo novas execuções após atualizações documentais.

| Job | Resultado remoto |
|---|---|
| Build e testes (Maven) | Sucesso; Java 17, `mvn -B verify`, 83 testes, 0 falhas/erros/ignorados |
| US09 e US10 (PostgreSQL real) | Sucesso; PostgreSQL 16, 17 testes de aceitação/rollback e 4 de integridade/concorrência/migração, 0 falhas/erros/ignorados |
| Container sobe do zero (compose + healthcheck) | Sucesso; Docker build com 83 testes, API e banco saudáveis, `/actuator/health` retornou `{"status":"UP"}` |
| Publica imagem no GHCR | Ignorado, conforme condição restrita a push na `main` |
| Deploy no Render | Ignorado; nenhum deploy de produção executado |

Os resultados foram conferidos no estado dos jobs e nos logs remotos, separadamente dos testes locais. Não houve divergência funcional ou falha introduzida pela Entrega 04 nessas execuções. Não foi necessário alterar código ou repetir a suíte local durante a preparação da publicação.

Os artefatos `surefire-reports` e `postgres-reports` ficam disponíveis nas respectivas execuções do GitHub Actions, sujeitos à retenção do serviço. A captura `github-actions.png` registra a execução de PR acima. Os logs temporários usados para conferência não são versionados.
