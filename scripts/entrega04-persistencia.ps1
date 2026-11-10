param(
    [string]$EnvFile = '.git/entrega04.env',
    [string]$OverrideFile = '.git/entrega04-compose.yaml',
    [string]$ProjectName = 'hemochain-entrega04',
    [string]$BaseUrl = 'http://localhost:18084'
)
$ErrorActionPreference = 'Stop'
$fluxoE04 = Get-Content entrega-04/evidencias/fluxo-ui.json -Raw | ConvertFrom-Json
$headersE04 = @{ 'X-Perfil' = 'GESTOR_HEMOCENTRO' }
$urlE04 = "$BaseUrl/bolsas/$($fluxoE04.bolsaId)/historico"
$antesE04 = Invoke-RestMethod -Uri $urlE04 -Headers $headersE04
$entregasAntesE04 = Invoke-RestMethod -Uri "$BaseUrl/requisicoes/$($fluxoE04.requisicaoId)/entregas" -Headers $headersE04
$jsonAntesE04 = ConvertTo-Json -InputObject $antesE04 -Depth 15 -Compress
docker compose -p $ProjectName --env-file $EnvFile -f compose.yaml -f $OverrideFile down
if ($LASTEXITCODE -ne 0) { throw 'Falha ao parar Compose.' }
# Sem -v: os dados e o volume de homologacao sao preservados.
docker compose -p $ProjectName --env-file $EnvFile -f compose.yaml -f $OverrideFile up -d --wait --wait-timeout 120
if ($LASTEXITCODE -ne 0) { throw 'Falha ao reiniciar Compose.' }
$depoisE04 = Invoke-RestMethod -Uri $urlE04 -Headers $headersE04
$entregasDepoisE04 = Invoke-RestMethod -Uri "$BaseUrl/requisicoes/$($fluxoE04.requisicaoId)/entregas" -Headers $headersE04
$jsonDepoisE04 = ConvertTo-Json -InputObject $depoisE04 -Depth 15 -Compress
if ($jsonAntesE04 -cne $jsonDepoisE04) { throw 'Historico divergiu apos reiniciar.' }
if ((ConvertTo-Json -InputObject $entregasAntesE04 -Depth 15 -Compress) -cne
        (ConvertTo-Json -InputObject $entregasDepoisE04 -Depth 15 -Compress)) { throw 'Entrega divergiu apos reiniciar.' }
if ($entregasDepoisE04[0].bolsa.status -ne 'UTILIZADA') { throw 'Estado final nao persistiu.' }
$healthE04 = Invoke-RestMethod "$BaseUrl/actuator/health"
if ($healthE04.status -ne 'UP') { throw 'Health nao esta UP.' }
$resultadoE04 = [ordered]@{
    data = [DateTime]::UtcNow.ToString('o')
    resultado = 'APROVADO'
    operacao = 'docker compose down (sem -v) e up -d --wait'
    projeto = $ProjectName
    health = $healthE04.status
    bolsaId = $fluxoE04.bolsaId
    requisicaoId = $fluxoE04.requisicaoId
    eventosAntes = @($antesE04).Count
    eventosDepois = @($depoisE04).Count
    historicoIdentico = $true
    entregaIdentica = $true
    statusBolsa = $entregasDepoisE04[0].bolsa.status
}
$resultadoE04 | ConvertTo-Json -Depth 5 | Set-Content entrega-04/evidencias/persistencia.json -Encoding UTF8
$resultadoE04 | ConvertTo-Json -Depth 5
docker compose -p $ProjectName --env-file $EnvFile -f compose.yaml -f $OverrideFile ps
