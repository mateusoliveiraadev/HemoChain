import { startDarkVeil } from './dark-veil.js';
import { initDock } from './dock.js';

// Aperfeicoamentos de apresentacao. As chamadas e regras dos modulos continuam nos HTMLs.
const body = document.body;
if (body.classList.contains('hemo-internal')) init();

function init() {
    let user;
    try { user = JSON.parse(localStorage.getItem('hemochainUsuario') || 'null'); } catch { return; }
    if (!user) return;
    const profiles = {
        GESTOR_HEMOCENTRO: 'Gestor de Hemocentro',
        HOSPITAL_SOLICITANTE: 'Hospital Solicitante',
        OPERADOR_LOGISTICA: 'Operador de Logística'
    };
    document.querySelectorAll('[data-profile-label]').forEach(n => { n.textContent = profiles[user.perfil] || user.perfil; });
    const greeting = document.getElementById('saudacao');
    if (greeting) greeting.textContent = greeting.textContent.replace(/^Ola, /, 'Olá, ');
    document.querySelectorAll('[data-profiles]').forEach(n => { n.hidden = !n.dataset.profiles.split(' ').includes(user.perfil); });
    initDock(document.querySelector('.hemo-dock'));
    document.querySelectorAll('[data-logout]').forEach(n => n.addEventListener('click', () => {
        window.HemoSession.logout();
    }));
    if (body.dataset.module === 'requisicoes' && user.perfil === 'GESTOR_HEMOCENTRO') {
        document.querySelector('.hc-description').textContent = 'Consulte as solicitações registradas pelos hospitais.';
    }
    if (user.perfil === 'OPERADOR_LOGISTICA' && body.dataset.module === 'atendimento') {
        document.querySelector('.hc-description').textContent = 'Consulte os alertas de confirmação de recebimento.';
        document.querySelector('#tituloAtendimento').textContent = 'Acompanhamento de recebimento';
    }

    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)');
    const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)');
    const backdrop = document.querySelector('.hc-veil');
    const canvas = backdrop.querySelector('canvas');
    let veil;
    const staticVeil = () => { backdrop.dataset.renderer = 'static'; canvas.hidden = true; };
    function syncVeil() {
        veil?.destroy(); veil = null; staticVeil();
        const modestDevice = (navigator.deviceMemory && navigator.deviceMemory <= 2) ||
            (navigator.hardwareConcurrency && navigator.hardwareConcurrency <= 2) || navigator.connection?.saveData;
        if (reduce.matches || modestDevice) return;
        canvas.hidden = false;
        veil = startDarkVeil(canvas, staticVeil);
        if (veil) backdrop.dataset.renderer = 'webgl'; else staticVeil();
    }
    reduce.addEventListener('change', syncVeil);
    syncVeil();

    // One delegated, frame-limited light for existing panels and module cards.
    // It only changes presentation; touch and reduced motion never track a pointer.
    const cardSelector = '.hc-panel,.container,.hc-module-card';
    let litCard = null, lightFrame = 0, pointerX = 0, pointerY = 0;
    function clearLight() {
        cancelAnimationFrame(lightFrame); lightFrame = 0;
        if (litCard) {
            litCard.removeAttribute('data-hc-lit');
            litCard.style.removeProperty('--spot-x'); litCard.style.removeProperty('--spot-y');
        }
        litCard = null;
    }
    document.addEventListener('pointermove', event => {
        if (reduce.matches || !finePointer.matches) return;
        const card = event.target.closest(cardSelector);
        if (card !== litCard) { clearLight(); litCard = card; }
        if (!litCard) return;
        pointerX = event.clientX; pointerY = event.clientY;
        if (lightFrame) return;
        lightFrame = requestAnimationFrame(() => {
            lightFrame = 0;
            const rect = litCard.getBoundingClientRect();
            litCard.style.setProperty('--spot-x', `${pointerX - rect.left}px`);
            litCard.style.setProperty('--spot-y', `${pointerY - rect.top}px`);
            litCard.setAttribute('data-hc-lit', '');
        });
    }, { passive: true });
    document.addEventListener('pointerout', event => {
        if (litCard && (!(event.relatedTarget instanceof Node) || !litCard.contains(event.relatedTarget))) clearLight();
    }, { passive: true });
    window.addEventListener('scroll', clearLight, { passive: true, capture: true });
    window.addEventListener('resize', clearLight, { passive: true });
    reduce.addEventListener('change', clearLight);
    finePointer.addEventListener('change', clearLight);

    // A API e o corpo enviado continuam definidos nas funcoes originais.
    document.querySelectorAll('[data-async-action]').forEach(button => {
        const action = window[button.dataset.asyncAction];
        if (typeof action !== 'function') return;
        button.onclick = async () => {
            if (button.disabled) return;
            button.disabled = true;
            try { await action(); }
            catch (error) {
                const status = document.getElementById(button.dataset.statusTarget);
                status.textContent = error.message || 'Não foi possível concluir. Tente novamente.';
                status.style.color = '#e74c3c';
            } finally { button.disabled = false; }
        };
    });

    const snapshots = new WeakMap();
    const renderedText = new WeakMap();
    const eventLabels = {
        CADASTRADA: 'Bolsa cadastrada', ATUALIZADA: 'Bolsa atualizada', DESCARTADA: 'Bolsa descartada',
        ENTREGA_REGISTRADA: 'Entrega registrada', RECEBIDA: 'Recebimento confirmado',
        UTILIZADA: 'Utilização registrada', RECEBIMENTO_PENDENTE: 'Recebimento pendente'
    };
    const stateLabels = {
        DISPONIVEL: 'Disponível', DESCARTADO: 'Descartada', ENTREGUE: 'Entregue', UTILIZADA: 'Utilizada',
        SOLICITADA: 'Solicitada', RECEBIDA: 'Recebida', RECEBIMENTO_PENDENTE: 'Recebimento pendente',
        NAO_CONFORME: 'Não conforme', PARCIALMENTE_ENTREGUE: 'Parcialmente entregue',
        PARCIALMENTE_RECEBIDA: 'Parcialmente recebida', PARCIALMENTE_UTILIZADA: 'Parcialmente utilizada'
    };

    function summary(pairs) {
        const list = document.createElement('dl'); list.className = 'hc-summary-grid';
        for (const [key, content] of pairs) {
            const pair = document.createElement('div'), label = document.createElement('dt'), value = document.createElement('dd');
            label.textContent = key;
            value.textContent = content ?? '';
            pair.append(label, value); list.appendChild(pair);
        }
        return list;
    }

    function decorateSummary(node) {
        if (!node.textContent || renderedText.get(node) === node.textContent) return;
        const payload = node.hemoPresentation;
        if (!payload || payload.source !== node.textContent) return;
        let pairs;
        if (payload.requisicao) {
            const r = payload.requisicao;
            pairs = [['Requisição', `#${r.id}`], ['Hospital', r.hospital.nome], ['Componente', r.tipoComponente], ['Receptor', r.tipoSanguineo], ['Quantidade', r.quantidade], ['Situação', r.status]];
        } else if (payload.entrega) {
            const e = payload.entrega;
            const date = value => value ? new Date(value).toLocaleString('pt-BR') : 'Não registrado';
            pairs = [[`Entrega #${e.id}`, date(e.entregueEm)], ['Prazo de confirmação', date(e.prazoConfirmacao)], ['Recebimento', date(e.recebidaEm)], ['Condição', e.condicao || 'Aguardando'], ['Observação', e.observacao || '—'], ['Utilização', date(e.utilizadaEm)]];
        } else return;
        const list = summary(pairs);
        node.replaceChildren(list); renderedText.set(node, node.textContent);
    }

    function stateBadge(cell) {
        if (cell.querySelector('.hc-state')) return;
        const raw = cell.textContent.trim();
        if (!stateLabels[raw] && !raw.includes('prioritária')) return;
        const span = document.createElement('span');
        span.className = 'hc-state';
        span.textContent = stateLabels[raw] || raw;
        span.dataset.tone = /PENDENTE|prioritária/.test(raw) ? 'warning' : /NAO_CONFORME|DESCARTADO/.test(raw) ? 'error' : /DISPONIVEL|UTILIZADA|RECEBIDA/.test(raw) ? 'success' : 'neutral';
        span.title = raw;
        cell.replaceChildren(span);
    }

    function decorateTable(tbody) {
        const table = tbody.closest('table');
        const headingNodes = [...table.querySelectorAll('thead th')];
        const headers = headingNodes.map((heading, index) => {
            heading.id ||= `${tbody.id}-column-${index}`;
            heading.setAttribute('role', 'columnheader');
            return heading.textContent.trim();
        });
        tbody.setAttribute('role', 'rowgroup');
        const previous = snapshots.get(tbody);
        const current = new Map();
        for (const row of tbody.rows) {
            row.setAttribute('role', 'row');
            if (row.cells.length === 1) { row.cells[0].colSpan = headers.length; row.cells[0].setAttribute('role', 'cell'); continue; }
            if (tbody.id === 'listaRequisicoes' && row.cells.length === 6) {
                const last = row.cells[5];
                const parts = last.textContent.match(/^(\d{4}-\d{2}-\d{2}) \/ (.+)$/);
                if (parts) {
                    last.textContent = parts[1];
                    const status = document.createElement('td'); status.textContent = parts[2]; row.appendChild(status);
                }
            }
            [...row.cells].forEach((cell, index) => {
                cell.dataset.label = headers[index] || '';
                cell.setAttribute('role', 'cell');
                if (headingNodes[index]) cell.setAttribute('headers', headingNodes[index].id);
                if ((tbody.id === 'bolsas' && index === 4) || (tbody.id === 'listaRequisicoes' && index === 6)) stateBadge(cell);
            });
            const key = row.cells[0].textContent.trim();
            const signature = row.textContent;
            current.set(key, signature);
            if (previous && previous.get(key) !== signature && !reduce.matches && !row.dataset.animated) {
                row.classList.add('hc-updated'); row.dataset.animated = 'true';
                row.addEventListener('animationend', () => row.classList.remove('hc-updated'), { once: true });
            }
        }
        snapshots.set(tbody, current);
    }

    function trace(node) {
        const source = node.textContent;
        if (!source || renderedText.get(node) === source) return;
        const payload = node.hemoPresentation;
        if (!payload || payload.source !== source || !payload.eventos.length) return;
        const fragment = document.createDocumentFragment();
        if (payload.bolsa) {
            const b = payload.bolsa;
            const pairs = [['Código', b.codigo], ['Componente / ABO', `${b.tipoComponente} / ${b.tipoSanguineo}`], ['Volume', `${b.volumeMl} mL`], ['Coleta', b.dataColeta], ['Validade calculada', b.validade], ['Status', b.status]];
            if (b.dataDescarte) pairs.push(['Descarte', b.dataDescarte], ['Motivo', b.motivoDescarte], ['Responsável', b.responsavelDescarte]);
            fragment.appendChild(summary(pairs));
        }
        const timeline = document.createElement('div'); timeline.className = 'hc-timeline';
        if (!reduce.matches) timeline.classList.add('hc-updated');
        if (payload.bolsa) { const title = document.createElement('h3'); title.textContent = 'Histórico cronológico (UTC)'; timeline.appendChild(title); }
        const list = document.createElement('ol');
        payload.eventos.forEach(event => {
            const item = document.createElement('li');
            const title = document.createElement('strong'); title.textContent = eventLabels[event.tipo] || event.tipo;
            const date = document.createElement('time'); date.textContent = event.momento; date.dateTime = event.momento;
            const origin = document.createElement('p'); origin.className = 'hc-event-origin'; origin.textContent = `${event.tipo} · Origem: ${event.origem}`;
            const detail = document.createElement('p'); detail.className = 'hc-event-detail'; detail.textContent = event.detalhes;
            item.append(title, date, origin, detail); list.appendChild(item);
        });
        timeline.appendChild(list); fragment.appendChild(timeline);
        node.replaceChildren(fragment); renderedText.set(node, node.textContent);
    }

    function feedback(node) {
        const color = node.style.color.replace(/\s/g, '');
        const tone = ['rgb(183,28,28)', 'rgb(231,76,60)'].includes(color) ? 'error' : ['rgb(38,113,52)', 'rgb(46,125,50)'].includes(color) ? 'success' : 'info';
        if (node.dataset.tone !== tone) node.dataset.tone = tone;
    }
    function busy(button) {
        if (button.disabled) button.setAttribute('aria-busy', 'true'); else button.removeAttribute('aria-busy');
    }
    function decorate() {
        document.querySelectorAll('tbody').forEach(decorateTable);
        document.querySelectorAll('#resumo,.entrega pre').forEach(decorateSummary);
        document.querySelectorAll('#detalhes,#historico').forEach(trace);
        document.querySelectorAll('.status,#mensagem').forEach(feedback);
        document.querySelectorAll('button').forEach(busy);
        document.querySelectorAll('#alertas li,#pendencias li').forEach(n => {
            const empty = /^Nenhum/.test(n.textContent);
            if (n.dataset.empty !== String(empty)) n.dataset.empty = String(empty);
        });
        document.querySelectorAll('.entrega h3').forEach(heading => {
            if (heading.dataset.decorated) return;
            const match = heading.textContent.match(/^(Bolsa .+) — ([A-Z_]+)$/);
            if (!match) return;
            heading.textContent = match[1];
            const code = document.createElement('span'); code.className = 'hc-event-code'; code.textContent = match[2]; heading.appendChild(code);
            heading.dataset.decorated = 'true';
        });
        document.querySelectorAll('.entrega form label').forEach(label => {
            if (label.control) return;
            const field = label.querySelector('select,textarea');
            if (field && !field.id) field.id = `hc-${field.tagName.toLowerCase()}-${crypto.randomUUID()}`;
            if (field) label.htmlFor = field.id;
        });
    }
    let pending = false;
    const observer = new MutationObserver(records => {
        if (records.every(record => record.type === 'attributes' && record.attributeName === 'style' && record.target.matches(cardSelector))) return;
        if (pending) return;
        pending = true;
        queueMicrotask(() => { pending = false; decorate(); });
    });
    observer.observe(document.getElementById('conteudo'), { childList: true, subtree: true, characterData: true, attributes: true, attributeFilter: ['disabled', 'style'] });
    decorate();
}
