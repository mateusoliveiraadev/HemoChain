"""Carga local de leitura para a janela de correcao; nao cria registros de negocio.

Execute da raiz: python observabilidade/scripts/manter-leituras.py
Para encerrar, crie .codex/observabilidade/parar-leituras (ou use Ctrl+C).
O estado e sobrescrito em um unico JSON ignorado pelo Git. Sem credenciais.
"""
import datetime as dt
import json
import os
from pathlib import Path
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / '.codex/observabilidade'
OUT.mkdir(parents=True, exist_ok=True)
STOP = OUT / 'parar-leituras'
STATE = OUT / 'carga-leitura-estado.json'
LOCK = OUT / 'carga-leitura.lock'


def get(path):
    request = urllib.request.Request('http://127.0.0.1:18086' + path,
                                     headers={'X-Perfil': 'GESTOR_HEMOCENTRO'})
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            response.read()
            return response.status
    except urllib.error.HTTPError as error:
        return error.code
    except (OSError, TimeoutError):
        return 0


def main():
    import msvcrt
    # Windows: o lock e liberado pelo sistema se o processo terminar.
    with LOCK.open('a+b') as lock:
        if lock.tell() == 0:
            lock.write(b'0')
            lock.flush()
        lock.seek(0)
        try:
            msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        except OSError:
            raise SystemExit('Ja existe uma carga de leitura ativa.')
        state = {'pid': os.getpid(), 'iniciado_em': dt.datetime.now(dt.timezone.utc).isoformat(),
                 'intervalo_segundos': 10, 'somente_get': True, 'iteracoes': 0,
                 'leituras_200': 0, 'erros_controlados_500': 0, 'falhas': 0}
        while not STOP.exists():
            status = get('/hospitais')
            state['iteracoes'] += 1
            state['leituras_200'] += status == 200
            state['falhas'] += status != 200
            if state['iteracoes'] % 6 == 1:
                error = get('/observabilidade/erro-simulado')
                state['erros_controlados_500'] += error == 500
                state['falhas'] += error != 500
            state['atualizado_em'] = dt.datetime.now(dt.timezone.utc).isoformat()
            state['ultimo_http'] = status
            STATE.write_text(json.dumps(state, indent=2) + '\n', encoding='utf-8')
            time.sleep(10 if status == 200 else 30)
        state['encerrado_em'] = dt.datetime.now(dt.timezone.utc).isoformat()
        STATE.write_text(json.dumps(state, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
