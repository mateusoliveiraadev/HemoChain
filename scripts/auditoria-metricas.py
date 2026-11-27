"""Resume XMLs JaCoCo/JUnit desta auditoria sem estimar cobertura ausente.

Uso: python scripts/auditoria-metricas.py docs/auditoria/2026-11-24/evidencias
Cada contexto e uma atribuicao analitica por classe, nao um modulo existente.
Nao soma percentuais nem linhas de metodos para calcular linhas do pacote.
"""

import json
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path


CONTEXTOS = {
    "Estoque": "BolsaHemocomponente StatusBolsa AlertaValidade BolsaService ValidadeService "
    "BolsaController EvolucaoStatusBolsaH2 VerificacaoValidadeAgendada",
    "Alocacao (somente compatibilidade existente)": "CompatibilidadeService",
    "Requisicao e cadastro hospitalar": "RequisicaoHospitalar StatusRequisicao UrgenciaRequisicao "
    "Hospital HospitalService HospitalController RequisicaoService RequisicaoController "
    "RequisicaoIndicadoresService DemandaAccumulator IndicadoresController "
    "IndicadoresDemandaResponse RequisicaoProjecao EstatisticaTipoSanguineo",
    "Expedicao e recebimento": "AtendimentoHospitalarService AtendimentoHospitalarController "
    "EntregaHospitalar CondicaoRecebimento VerificacaoRecebimentoAgendada",
    "Rastreabilidade": "EventoBolsa TipoEventoBolsa RastreabilidadeService ProtecaoHistoricoPostgres",
    "Acesso": "Usuario PerfilUsuario UsuarioService UsuarioController PasswordEncoderConfig",
    "Compartilhado e inicializacao": "TipoComponente TipoSanguineo RelogioConfig HemochainApplication",
    "Apoio de dados sinteticos": "DadosSinteticosService DadosSinteticosController ResultadoGeracaoDados",
}
DONO = {nome: contexto for contexto, nomes in CONTEXTOS.items() for nome in nomes.split()}
TIPOS = ("LINE", "BRANCH", "METHOD")


def contadores(node):
    return {c.get("type"): {k: int(c.get(k)) for k in ("covered", "missed")}
            for c in node.findall("counter") if c.get("type") in TIPOS}


def formato(metricas, tipo):
    c = metricas.get(tipo, {})
    cobertas, perdidas = c.get("covered", 0), c.get("missed", 0)
    total = cobertas + perdidas
    return f"{cobertas}/{total} ({100 * cobertas / total:.2f}%)" if total else "N/A"


def contexto(classe):
    if classe.startswith("com/hemochain/estrutura/"):
        return "Exercicio U1 de estruturas de dados"
    nome = classe.rsplit("/", 1)[-1].split("$", 1)[0]
    if nome in DONO:
        return DONO[nome]
    if classe.startswith("com/hemochain/repository/"):
        return "Interfaces de repositorio sem bytecode executavel"
    raise ValueError(f"Classe sem mapeamento de contexto: {classe}")


def cobertura(path):
    root = ET.parse(path).getroot()
    pacotes = {p.get("name"): contadores(p) for p in root.findall("package")}
    classes = {c.get("name"): contadores(c) for p in root.findall("package") for c in p.findall("class")}
    grupos = defaultdict(lambda: {t: {"covered": 0, "missed": 0} for t in TIPOS})
    propriedade = {}
    for nome, metricas in classes.items():
        grupo = contexto(nome)
        propriedade[nome] = grupo
        for tipo in TIPOS:
            for chave in ("covered", "missed"):
                grupos[grupo][tipo][chave] += metricas.get(tipo, {}).get(chave, 0)
    # Grupos usam contadores de classes; pacotes usam contadores de pacotes.
    # Esta verificacao detecta omissoes no mapeamento e dupla contagem de classes.
    for tipo, esperado in contadores(root).items():
        for chave in ("covered", "missed"):
            assert sum(m[tipo][chave] for m in grupos.values()) == esperado[chave]
    return dict(total=contadores(root), pacotes=pacotes, classes=classes,
                contextos=dict(grupos), propriedade=propriedade)


def junit(paths):
    suites = []
    for path in sorted(paths):
        root = ET.parse(path).getroot()
        for suite in ([root] if root.tag == "testsuite" else root.findall("testsuite")):
            valores = {k: int(suite.get(k, "0")) for k in ("tests", "failures", "errors", "skipped")}
            valores["passed"] = valores["tests"] - sum(valores[k] for k in ("failures", "errors", "skipped"))
            suites.append(dict(classe=suite.get("name"), **valores))
    return dict(totais={k: sum(s[k] for s in suites)
                       for k in ("tests", "passed", "failures", "errors", "skipped")}, suites=suites)


def gerar(pasta):
    dados = {}
    for etapa in ("baseline", "final"):
        dados[etapa] = dict(cobertura=cobertura(pasta / f"{etapa}-jacoco.xml"),
                            testes=junit((pasta / f"{etapa}-surefire").glob("TEST-*.xml")))
    for etapa in ("tdd-red", "tdd-green", "tdd-red-2", "tdd-green-2"):
        if (pasta / f"{etapa}.xml").exists():
            dados[etapa] = junit([pasta / f"{etapa}.xml"])
    for etapa in ("postgres-surefire", "postgres-failsafe"):
        if (pasta / etapa).exists():
            dados[etapa] = junit((pasta / etapa).glob("TEST-*.xml"))
    (pasta / "metricas.json").write_text(json.dumps(dados, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    linhas = ["# Cobertura e inventário de testes medidos", "",
              "Gerado a partir dos XMLs preservados em `evidencias/`, com JaCoCo 0.8.14. "
              "As duas medições comparáveis usam a suíte padrão completa em JVM 21, release 17, H2/MockMvc. "
              "As execuções PostgreSQL são registradas separadamente.", "",
              "## Execuções JUnit", "",
              "| Etapa | Executados | Aprovados | Falhas | Erros | Ignorados |",
              "|---|---:|---:|---:|---:|---:|"]
    for etapa in ("baseline", "final", "tdd-red", "tdd-green", "tdd-red-2", "tdd-green-2",
                  "postgres-surefire", "postgres-failsafe"):
        d = dados.get(etapa)
        if d:
            t = d["testes"]["totais"] if etapa in ("baseline", "final") else d["totais"]
            linhas.append(f"| {etapa} | " + " | ".join(str(t[k]) for k in
                          ("tests", "passed", "failures", "errors", "skipped")) + " |")
    linhas += ["", "Contam-se invocações JUnit, inclusive parâmetros. A execução PostgreSQL "
               "repete 17 cenários existentes e acrescenta 4 IT já existentes; não são 21 testes novos.", "",
               "## Total comparável", "", "| Etapa | Linhas | Branches | Métodos |",
               "|---|---:|---:|---:|"]
    for etapa in ("baseline", "final"):
        linhas.append(f"| {etapa} | " + " | ".join(formato(dados[etapa]["cobertura"]["total"], t) for t in TIPOS) + " |")
    for titulo, chave in (("Pacotes reais", "pacotes"), ("Contextos analíticos", "contextos")):
        linhas += ["", f"## {titulo}", "",
                   "| Grupo | Linhas antes → depois | Branches antes → depois | Métodos antes → depois |",
                   "|---|---:|---:|---:|"]
        for nome in sorted(dados["baseline"]["cobertura"][chave]):
            a = dados["baseline"]["cobertura"][chave][nome]
            d = dados["final"]["cobertura"][chave][nome]
            linhas.append(f"| {nome} | " + " | ".join(f"{formato(a,t)} → {formato(d,t)}" for t in TIPOS) + " |")
    linhas += ["", "Contextos são uma classificação por responsabilidade principal, não pacotes isolados "
               "já implementados. Cada classe pertence a exatamente um grupo; classes internas acompanham "
               "a externa. O mapeamento completo está em `metricas.json` e neste script. "
               "N/A significa ausência de bytecode/decisões no denominador, não cobertura de 100%. "
               "Os repositórios Spring Data são exercitados por integração, mas suas interfaces não "
               "têm linhas executáveis mensuráveis. Alocação mede só compatibilidade; reserva ainda não existe.", "",
               "## Inventário por classe de teste", "",
               "| Classe | Antes | Depois |", "|---|---:|---:|"]
    antes = {s["classe"]: s["tests"] for s in dados["baseline"]["testes"]["suites"]}
    depois = {s["classe"]: s["tests"] for s in dados["final"]["testes"]["suites"]}
    for nome in sorted(antes.keys() | depois.keys()):
        linhas.append(f"| {nome} | {antes.get(nome, 0)} | {depois.get(nome, 0)} |")
    linhas += ["", "## Limites da métrica", "",
               "JaCoCo mede execução de bytecode: getters e construtores elevam cobertura de entidades "
               "sem demonstrar invariantes. Na baseline, EntregaHospitalar tinha 100% de linhas e ainda "
               "aceitava todas as nove violações observadas no RED. Cobertura não prova TDD, exclusividade, "
               "corretude clínica ou ausência de regressões. As contagens Java não incluem asserts C nem "
               "scripts Playwright. Não foram excluídas classes de produção para melhorar percentuais.", ""]
    (pasta.parent / "cobertura.md").write_text("\n".join(linhas), encoding="utf-8")
    print(json.dumps({k: v["testes"]["totais"] for k, v in dados.items() if k in ("baseline", "final")}, indent=2))


if __name__ == "__main__":
    gerar(Path(sys.argv[1]))
