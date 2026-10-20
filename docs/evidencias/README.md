# Evidencias

Sem evidencia o item nao pontua. Salve aqui os prints (png) ou links. Nada abaixo foi gerado
automaticamente: o grupo precisa executar e registrar.

## E1. Container
- [ ] `docker compose ps` com `api` e `db` como healthy -> `e1-compose-ps.png`
- [ ] API respondendo: `curl http://localhost:8080/actuator/health` -> `e1-health.png`
- [ ] Dados persistem: cadastre uma bolsa, rode `docker compose down` e `docker compose up -d`, consulte de novo -> `e1-volume.png`

## E2. TDD
- [ ] `git log --oneline` mostrando `test: ... (RED)` antes de `feat: ... (GREEN)` -> `e2-historico.png`
- [ ] Execucao vermelha: faca push do commit `test:` sozinho e capture a falha na aba Actions -> `e2-vermelho.png`
- [ ] Execucao verde apos o `feat:` -> `e2-verde.png`

Como obter o vermelho de verdade (a branch local ja tem os commits em ordem):

```bash
# 1) envia ate o commit "test:" (esteira vermelha)
git push origin <sha-do-commit-test>:refs/heads/feature/pi-container-tdd-deploy
# 2) depois envia o restante (esteira verde)
git push origin feature/pi-container-tdd-deploy
```

## E3. Deploy
- [ ] Execucao vermelha que barrou o deploy (jobs `publish` e `deploy` pulados) -> `e3-vermelho.png`
- [ ] Execucao verde que publicou (4 jobs verdes) -> `e3-verde.png`
- [ ] URL publica no ar -> `e3-url.png`
- [ ] Rollback executado (workflow Rollback verde + versao anterior no ar) -> `e3-rollback.png`
- [ ] Protecao da main ativada -> `e3-branch-protection.png`
