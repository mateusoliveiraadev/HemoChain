// Verificacao visual/acessibilidade do front-end. Reutiliza Playwright instalado fora do projeto.
// HEMOCHAIN_URL aponta para uma instancia local; capturas vao para target/ por padrao.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');

const base = process.env.HEMOCHAIN_URL || 'http://localhost:18084';
assert(['localhost', '127.0.0.1'].includes(new URL(base).hostname), 'Use uma instancia local.');
const output = path.resolve(process.env.HEMOCHAIN_UI_OUTPUT || 'target/premium-dark-evidence');
const sizes = [{ name: 'desktop', width: 1440, height: 1000 }, { name: 'tablet', width: 834, height: 1112 }, { name: 'mobile', width: 390, height: 844 }, { name: 'mobile-small', width: 320, height: 780 }];
const profiles = { GESTOR_HEMOCENTRO: ['index', 'estoque', 'requisicoes', 'atendimento'], HOSPITAL_SOLICITANTE: ['index', 'requisicoes', 'atendimento'], OPERADOR_LOGISTICA: ['index', 'atendimento'] };
const report = { cases: [], errors: [], renderers: {}, checks: [] };

async function main() {
    await fs.mkdir(output, { recursive: true });
    const browser = await chromium.launch({ channel: process.env.PLAYWRIGHT_CHANNEL || 'msedge', headless: true });
    async function context(profile, size, extra = {}) {
        const ctx = await browser.newContext({ viewport: { width: size.width, height: size.height }, locale: 'pt-BR', timezoneId: 'America/Fortaleza', isMobile: size.width < 600, hasTouch: size.width < 600, ...extra });
        if (profile) await ctx.addInitScript(profile => {
            if (!sessionStorage.getItem('hcTestInitialized')) {
                localStorage.setItem('hemochainUsuario', JSON.stringify({ nome: 'Validação visual', perfil: profile }));
                sessionStorage.setItem('hcTestInitialized', 'true');
            }
        }, profile);
        return ctx;
    }
    function watch(page) {
        page.on('pageerror', e => report.errors.push(e.message));
        page.on('response', r => { if (r.status() >= 400 && !r.url().endsWith('/favicon.ico')) report.errors.push(`${r.status()} ${r.url()}`); });
    }
    try {
        for (const size of sizes) {
            for (const [profile, files] of Object.entries(profiles)) {
                const ctx = await context(profile, size);
                const p = await ctx.newPage(); watch(p);
                for (const file of files) {
                    await p.goto(`${base}/${file}.html`, { waitUntil: 'networkidle' });
                    await p.evaluate(() => document.fonts.ready);
                    assert(p.url().endsWith(file + '.html'));
                    assert.equal(await p.locator('.hc-nav [aria-current="page"]:visible').count(), 1);
                    assert.equal(await p.locator('.hc-nav a[href="estoque.html"]:visible').count(), profile === 'GESTOR_HEMOCENTRO' ? 1 : 0);
                    const geometry = await p.evaluate(() => ({ width: document.documentElement.clientWidth, scrollWidth: document.documentElement.scrollWidth }));
                    assert(geometry.scrollWidth <= geometry.width + 1, `${size.name}/${profile}/${file}: overflow horizontal`);
                    const unlabelled = await p.locator('input,select,textarea').evaluateAll(fields => fields.filter(f => f.getClientRects().length && !f.labels?.length && !f.getAttribute('aria-label') && !f.getAttribute('aria-labelledby')).map(f => f.id));
                    assert.deepEqual(unlabelled, [], `${file}: campos sem rotulo`);
                    if (file === 'index') assert.equal(await p.locator('.hc-module-card').count(), profile === 'GESTOR_HEMOCENTRO' ? 3 : profile === 'HOSPITAL_SOLICITANTE' ? 2 : 1);
                    if (file === 'requisicoes') assert.equal(await p.locator('.hc-request-grid').isVisible(), profile === 'HOSPITAL_SOLICITANTE');
                    const renderer = await p.locator('.hc-veil').getAttribute('data-renderer');
                    report.renderers[renderer] = (report.renderers[renderer] || 0) + 1;
                    const name = `${size.name}-${profile}-${file}`;
                    await p.screenshot({ path: path.join(output, `${name}-topo.png`) });
                    if (profile === 'GESTOR_HEMOCENTRO' && file === 'estoque' && await p.locator('#bolsas button').count()) {
                        await p.locator('#bolsas button').first().click();
                        await p.locator('#painelDetalhes').waitFor();
                        await p.waitForFunction(() => document.querySelector('#detalhes .hc-timeline li') || /Sem eventos/.test(document.getElementById('detalhes').textContent));
                        assert(await p.locator('#detalhes').innerText());
                    }
                    await p.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
                    await p.screenshot({ path: path.join(output, `${name}-fim.png`) });
                    report.cases.push({ name, geometry, renderer });
                }
                await ctx.close();
            }
        }
        const ctx = await context('GESTOR_HEMOCENTRO', sizes[0]);
        const p = await ctx.newPage(); watch(p);
        await p.goto(base + '/index.html', { waitUntil: 'networkidle' });
        await p.keyboard.press('Tab');
        assert.equal(await p.locator(':focus').getAttribute('class'), 'hc-skip-link');
        await p.keyboard.press('Enter');
        assert.equal(await p.locator(':focus').getAttribute('id'), 'conteudo');
        await p.keyboard.press('Tab');
        assert.equal(await p.locator(':focus').evaluate(n => getComputedStyle(n).outlineWidth), '2px');
        await p.emulateMedia({ reducedMotion: 'reduce' });
        await p.waitForFunction(() => document.querySelector('.hc-veil').dataset.renderer === 'static');
        assert.equal(await p.locator('[data-dark-veil]').isVisible(), false);
        assert.equal(await p.evaluate(() => document.getAnimations().length), 0);
        report.checks.push('Teclado, link de salto, foco e prefers-reduced-motion.');
        await p.emulateMedia({ reducedMotion: 'no-preference' });
        await p.goto(base + '/estoque.html', { waitUntil: 'networkidle' });
        await p.locator('#codigo').fill('PREENCHIMENTO-PRESERVADO');
        for (const width of [1280, 600, 1440]) {
            await p.setViewportSize({ width, height: 1000 });
            await p.waitForTimeout(80);
            assert.equal(await p.locator('#codigo').inputValue(), 'PREENCHIMENTO-PRESERVADO');
            if (await p.locator('.hc-veil').getAttribute('data-renderer') === 'webgl') assert.equal(await p.locator('canvas').evaluate(n => n.getContext('webgl2').getError()), 0);
        }
        report.checks.push('Redimensionamento preserva campos e nao gera erros WebGL.');
        if (await p.locator('.hc-veil').getAttribute('data-renderer') === 'webgl') {
            await p.locator('canvas').evaluate(n => n.getContext('webgl2').getExtension('WEBGL_lose_context')?.loseContext());
            await p.waitForFunction(() => document.querySelector('.hc-veil').dataset.renderer === 'static');
            assert.equal(await p.locator('#codigo').inputValue(), 'PREENCHIMENTO-PRESERVADO');
            report.checks.push('Perda do contexto GPU retorna ao fundo estatico sem interromper o formulario.');
        }
        await p.locator('[data-logout]').click();
        await p.waitForURL('**/login.html');
        assert.equal(await p.locator('link[rel=stylesheet],canvas,.hc-sidebar').count(), 0);
        report.checks.push('Sair retorna ao login sem carregar estilos ou scripts internos.');
        await ctx.close();

        const low = await context('GESTOR_HEMOCENTRO', sizes[0]);
        await low.addInitScript(() => Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 2 }));
        const lowPage = await low.newPage(); watch(lowPage);
        await lowPage.goto(base + '/index.html', { waitUntil: 'networkidle' });
        assert.equal(await lowPage.locator('.hc-veil').getAttribute('data-renderer'), 'static');
        await low.close();
        report.checks.push('Dispositivo modesto usa fase estatica.');

        for (const size of sizes.slice(0, 3)) {
            const anon = await browser.newContext({ viewport: { width: size.width, height: size.height } });
            const lp = await anon.newPage();
            await lp.goto(base + '/login.html', { waitUntil: 'networkidle' });
            await lp.screenshot({ path: path.join(output, `login-depois-${size.name}.png`) });
            assert.equal(await lp.locator('link[rel=stylesheet],canvas').count(), 0);
            await anon.close();
        }
        const login = await fs.readFile('src/main/resources/static/login.html');
        assert.equal(crypto.createHash('sha256').update(login).digest('hex'), '9ad4d02f7f88863dd141d6bd93c29d5ceec5be77cea732f32d961beb0c9aead1');
        report.checks.push('Arquivo do login identico ao SHA256 anterior.');
        assert.deepEqual(report.errors, []);
        report.status = 'APROVADO';
    } finally {
        await browser.close();
        await fs.writeFile(path.join(output, 'resultado-premium-dark.json'), JSON.stringify(report, null, 2));
    }
    console.log(JSON.stringify({ status: report.status, cases: report.cases.length, renderers: report.renderers, checks: report.checks, errors: report.errors }, null, 2));
}
main().catch(error => { console.error(error); process.exitCode = 1; });
