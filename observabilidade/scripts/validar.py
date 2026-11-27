"""Validacao local RSD: consulta servicos reais e salva evidencias sem credenciais.

Execute na raiz: python observabilidade/scripts/validar.py
Requer a stack e a carga ja iniciadas. Nao inicia containers nem tunel.
"""
import base64
import datetime as dt
import json
from pathlib import Path
import re
import subprocess
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
ENV = dict(line.split("=", 1) for line in (ROOT / ".env.observabilidade").read_text(encoding="utf-8-sig").splitlines()
           if "=" in line and not line.lstrip().startswith("#"))
PROM = "http://127.0.0.1:" + ENV.get("OBS_PROMETHEUS_PORT", "19090")
GRAFANA = "http://127.0.0.1:" + ENV.get("OBS_GRAFANA_PORT", "13000")
AUTH = "Basic " + base64.b64encode((ENV.get("GRAFANA_USER", "admin") + ":" + ENV["GRAFANA_PASSWORD"]).encode()).decode()
OUT = ROOT / "docs/observabilidade/evidencias"
OUT.mkdir(parents=True, exist_ok=True)


def get(url):
    headers = {"Accept": "application/json"}
    if url.startswith(GRAFANA + "/"):
        headers["Authorization"] = AUTH
    with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=30) as response:
        return json.load(response)


def save(name, data):
    (OUT / name).write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def prom(query):
    return get(PROM + "/api/v1/query?" + urllib.parse.urlencode({"query": query}))


def main():
    result = {"capturado_em": dt.datetime.now(dt.timezone.utc).isoformat(), "checks": {}}
    targets = get(PROM + "/api/v1/targets")
    save("targets.json", targets)
    active = targets["data"]["activeTargets"]
    result["checks"]["targets"] = {t["labels"]["job"]: t["health"] for t in active}
    assert len(active) == 7 and all(t["health"] == "up" for t in active), "Targets incompletos ou DOWN"
    queries = {}
    for name in ["http_server_requests_seconds_count", "http_server_requests_seconds_bucket", "jvm_memory_used_bytes",
                 "jvm_gc_pause_seconds_count", "jvm_threads_live_threads", "process_cpu_usage", "process_uptime_seconds",
                 "hikaricp_connections_active", "hemochain_entrega_tentativas_total", "hemochain_entrega_registro_seconds_count"]:
        queries[name] = prom(name + '{application="hemochain"}')
        assert queries[name]["data"]["result"], "Sem serie: " + name
    save("metricas.json", queries)
    result["checks"]["metricas_aplicacao"] = len(queries)
    dashboard_queries = []
    for file in (ROOT / "observabilidade/grafana/dashboards").glob("*.json"):
        dashboard = json.loads(file.read_text(encoding="utf-8"))
        for panel in dashboard["panels"]:
            for target in panel.get("targets", []):
                query = target["expr"]
                if target.get("datasource", {}).get("type") == "prometheus":
                    response = prom(query)
                    dashboard_queries.append({"dashboard": dashboard["uid"], "painel": panel["title"],
                                              "query": query, "resposta": response})
    save("consultas-dashboards.json", dashboard_queries)
    result["checks"]["consultas_promql"] = len(dashboard_queries)
    logs = get(GRAFANA + "/api/datasources/proxy/uid/loki/loki/api/v1/query_range?" + urllib.parse.urlencode({
        "query": '{app="hemochain"}', "limit": 1000, "direction": "backward"}))
    save("loki.json", logs)
    lines = [line for stream in logs["data"]["result"] for _, line in stream["values"]]
    assert any("Registro de entrega processado" in line for line in lines), "Falta log de sucesso"
    assert any("Falha simulada" in line for line in lines), "Falta log de erro"
    trace = None
    for line in lines:
        if "Registro de entrega processado" not in line:
            continue
        match = re.search(r"traceId=([0-9a-f]{32})", line)
        if match:
            trace_id = match[1]
            trace = get(GRAFANA + "/api/datasources/proxy/uid/tempo/api/traces/" + trace_id)
            break
    assert trace, "Trace do log nao localizado"
    save("trace.json", trace)
    def spans(value):
        if isinstance(value, dict):
            return ([value] if "spanId" in value and "name" in value else []) + [s for v in value.values() for s in spans(v)]
        return [s for v in value for s in spans(v)] if isinstance(value, list) else []
    trace_spans = spans(trace)
    assert len(trace_spans) >= 3, "Trace sem multiplos spans"
    result["checks"]["trace"] = {"traceId": trace_id, "spans": len(trace_spans), "nomes": [s["name"] for s in trace_spans]}
    compose = ["docker", "compose", "--env-file", str(ROOT / ".env.observabilidade"), "-f", str(ROOT / "compose.observabilidade.yaml")]
    ps = subprocess.check_output(compose + ["ps", "--all"], text=True, encoding="utf-8")
    (OUT / "compose-ps.txt").write_text(result["capturado_em"] + "\n" + ps, encoding="utf-8")
    metrics = subprocess.check_output(compose + ["exec", "-T", "api", "wget", "-qO-", "http://localhost:8081/actuator/prometheus"], text=True, encoding="utf-8")
    (OUT / "actuator-prometheus.txt").write_text(metrics, encoding="utf-8")
    save("validacao.json", result)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
