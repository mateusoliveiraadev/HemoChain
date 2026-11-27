# Carga leve e disponibilidade — atualização final

Grafana e Quick Tunnel permanecem ativos. URL: https://cached-former-attempts-think.trycloudflare.com — usuário professor, papel Viewer. A senha permanece fora desta pasta, no arquivo local ignorado do projeto. Comunicar somente no canal privado da disciplina.

O k6 finito foi parado, preservando os dados já coletados. A partir de 27/11/2026 às 17:32 (Fortaleza), um processo Python local mantém somente GET /hospitais a cada dez segundos e um GET de erro controlado a cada minuto. Não executa POST, PUT ou DELETE e não cria hospitais, bolsas, requisições, entregas ou histórico. As contagens foram conferidas antes/depois; o estado da validação fica em carga-leitura-validacao.json. A retenção já configurada continua limitando métricas/logs/traces; as leituras não eliminam o consumo normal da telemetria.

## Durante a correção

Deixar computador ligado, conectado à energia, Docker Desktop e internet ativos. A carga de leitura não tem o limite anterior de 60 minutos: permanece até parada explícita ou encerramento do processo/computador. Ela não reinicia automaticamente após reboot. A janela de correção e o responsável continuam a confirmar. A suspensão na tomada estava desativada; na bateria, dez minutos.

Estado local (sem credenciais): `.codex/observabilidade/carga-leitura-estado.json`. O arquivo é sobrescrito, sem acúmulo de logs. Um lock impede dois trabalhadores simultâneos.

## Reiniciar após reboot (PowerShell, raiz do repositório)

```powershell
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml up -d
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml --profile remote up -d tunel
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml logs --tail 30 tunel | Select-String 'https://.*trycloudflare.com'
python observabilidade/scripts/manter-leituras.py
```

O último comando ocupa o terminal até Ctrl+C. Para mantê-lo em segundo plano no Windows:

```powershell
Start-Process -FilePath (Get-Command python.exe).Source -ArgumentList 'observabilidade/scripts/manter-leituras.py' -WorkingDirectory (Get-Location).Path -WindowStyle Hidden
```

Não reiniciar lotes k6 com escrita para manutenção rotineira. O script k6 original permanece disponível para demonstrações limitadas e intencionais. Após reiniciar o túnel, conferir se a URL mudou e avisar o professor no canal autorizado.

## Encerrar a carga após a correção

```powershell
New-Item -ItemType File -Path .codex/observabilidade/parar-leituras
docker compose --env-file .env.observabilidade -f compose.observabilidade.yaml --profile remote stop tunel
```

O marcador encerra o trabalhador após o ciclo atual. Para nova execução deliberada, remova somente esse marcador de parada, se existir. Não usar down -v, não remover volumes e não expor API, banco, Prometheus ou Actuator. Confirmar também o login do professor em rede independente.
