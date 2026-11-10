"""Revisao local de links, integridade Git e padroes comuns de segredos, sem imprimir valores."""
import hashlib
import json
import re
import subprocess
from datetime import datetime, timezone
from pathlib import Path

root = Path(__file__).resolve().parents[1]

def git(*args):
    return subprocess.run(["git", *args], cwd=root, capture_output=True, text=True, check=True).stdout.strip()

patterns = {
    "chave_privada": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "github_token": re.compile(r"\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{50,})\b"),
    "aws_access_key": re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
    "jwt": re.compile(r"\beyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}\b"),
    "senha_datasource_literal": re.compile(r"(?m)^spring\.datasource\.password[ \t]*=[ \t]*(?![ \t]*\$\{)[^ \t\r\n]+"),
}
findings = []
tracked = git("ls-files").splitlines()
for name in tracked:
    p = root / name
    if not p.is_file() or p.suffix.lower() in {".png", ".jpg", ".mp4", ".jar"}:
        continue
    try:
        text = p.read_text(encoding="utf-8-sig")
    except (UnicodeError, OSError):
        continue
    for kind, pattern in patterns.items():
        for match in pattern.finditer(text):
            findings.append({"arquivo": name, "linha": text[:match.start()].count("\n")+1, "tipo": kind})

bad_links = []
for md in [root / "README.md", *(root / "entrega-04").rglob("*.md")]:
    for target in re.findall(r"\]\(([^)]+)\)", md.read_text(encoding="utf-8-sig")):
        if target.startswith(("https://", "http://", "#")):
            continue
        local = target.split("#", 1)[0]
        if local and not (md.parent / local).exists():
            bad_links.append({"arquivo": str(md.relative_to(root)), "link": target})

git("diff", "--check")
git("diff", "--cached", "--check")
git("fsck", "--no-progress")
preserved_hash = hashlib.sha256((root / "entrega-03/bug-tracker.png").read_bytes()).hexdigest().upper()
assert preserved_hash == "C47F7AE1DFA436ADBB07DCFD9313137F793018151209A2B4A17C2127C0B823BC"
assert not findings, findings
assert not bad_links, bad_links
assert not any(n == ".env" or n.endswith(".env") for n in tracked)
result = {
    "data": datetime.now(timezone.utc).isoformat(), "resultado": "APROVADO",
    "branch": git("branch", "--show-current"), "headAuditado": git("rev-parse", "HEAD"),
    "gitFsck": "APROVADO", "diffCheck": "APROVADO", "linksLocais": "APROVADO",
    "padroesSegredosEncontrados": findings, "envRealVersionado": False,
    "limiteDaRevisao": "Busca de padroes comuns e revisao dos arquivos; nao constitui auditoria de seguranca completa.",
    "imagemPreexistentePreservadaSha256": preserved_hash,
    "publicacaoExterna": "PENDENTE de autorizacao; sem merge ou deploy",
}
(root / "entrega-04/evidencias/auditoria-final.json").write_text(json.dumps(result, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")

build = (root / "entrega-04/evidencias/testes-maven-java17.txt").read_text(encoding="utf-8-sig")
postgres = (root / "entrega-04/evidencias/testes-postgresql.txt").read_text(encoding="utf-8-sig")
assert "Tests run: 83, Failures: 0, Errors: 0, Skipped: 0" in build
assert "Tests run: 17, Failures: 0, Errors: 0, Skipped: 0" in postgres
assert "Tests run: 4, Failures: 0, Errors: 0, Skipped: 0" in postgres
ui = json.loads((root / "entrega-04/evidencias/resultado-ui.json").read_text(encoding="utf-8-sig"))
persistencia = json.loads((root / "entrega-04/evidencias/persistencia.json").read_text(encoding="utf-8-sig"))
assert ui["status"] == "APROVADO" and not ui["errosJavaScript"]
assert persistencia["resultado"] == "APROVADO" and persistencia["historicoIdentico"] and persistencia["entregaIdentica"]
resumo = {
    "data": result["data"], "resultado": "APROVADO",
    "mavenDockerJava17": {"testes": 83, "falhas": 0, "erros": 0, "ignorados": 0},
    "postgresql16": {"aceitacaoRollback": 17, "integridadeConcorrenciaMigracao": 4, "falhas": 0, "erros": 0, "ignorados": 0},
    "interface": ui, "persistencia": persistencia,
    "revisaoGitCredenciaisLinks": "APROVADO — detalhes e limites em auditoria-final.json",
    "githubActionsRemoto": "PENDENTE de publicacao autorizada",
    "youtube": "PENDENTE da equipe — roteiros entregues",
    "mergeDeployProducao": "NAO EXECUTADO",
}
(root / "entrega-04/evidencias/validacao-final.json").write_text(json.dumps(resumo, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
print(json.dumps(result, ensure_ascii=False, indent=2))
