// Browser regression for P1. Reuses an external Playwright installation.
// Read-only against localhost; screenshots/reports default to ignored target/.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const base = process.env.HEMOCHAIN_URL || 'http://localhost:18084';
assert(['localhost','127.0.0.1'].includes(new URL(base).hostname));
const output = path.resolve(process.env.HEMOCHAIN_UI_OUTPUT || 'target/dock-evidence');
const profiles = {
    GESTOR_HEMOCENTRO: ['index','estoque','requisicoes','atendimento'],
    HOSPITAL_SOLICITANTE: ['index','requisicoes','atendimento'],
    OPERADOR_LOGISTICA: ['index','atendimento']
};
const sizes = [{width:1440,height:1000},{width:1920,height:1080},{width:834,height:1112},{width:390,height:844},{width:320,height:780},{width:844,height:390}];
const report = { routes: [], checks: [], errors: [] };
async function main() {
    await fs.mkdir(output,{recursive:true});
    const browser = await chromium.launch({channel:process.env.PLAYWRIGHT_CHANNEL || 'msedge',headless:true});
    async function context(profile,size) {
        const ctx = await browser.newContext({viewport:size,isMobile:size.width<600,hasTouch:size.width<600,locale:'pt-BR'});
        await ctx.addInitScript(profile => {
            if (!sessionStorage.getItem('dockTest')) {
                localStorage.setItem('hemochainUsuario',JSON.stringify({nome:'Teste Dock',perfil:profile}));
                sessionStorage.setItem('dockTest','true');
            }
        },profile);
        return ctx;
    }
    function watch(p) {
        p.on('pageerror',e=>report.errors.push(e.message));
        p.on('response',r=>{if(r.status()>=400&&!r.url().endsWith('/favicon.ico'))report.errors.push(`${r.status()} ${r.url()}`);});
    }
    try {
        for (const size of sizes) for (const [profile,routes] of Object.entries(profiles)) {
            const ctx=await context(profile,size),p=await ctx.newPage();watch(p);
            await p.goto(base+'/index.html',{waitUntil:'networkidle'});
            for (const route of routes) {
                await p.locator(`.hc-nav a[href="${route}.html"]`).click();
                await p.waitForURL(`**/${route}.html`);await p.waitForLoadState('networkidle');
                const nav=p.locator('.hemo-dock');assert.equal(await nav.count(),1);
                assert.deepEqual(await p.locator('.hc-nav a:visible').evaluateAll(ns=>ns.map(n=>n.getAttribute('href'))),routes.map(r=>r+'.html'));
                assert.equal(await p.locator('.hc-nav a[aria-current="page"]:visible').getAttribute('href'),route+'.html');
                assert.equal(await p.locator('main [data-profile-label]:visible').count(),1);
                const box=await nav.boundingBox();assert(box.x>=0&&box.y>=0&&box.x+box.width<=size.width+1&&box.y+box.height<=size.height+1);
                assert(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
                for(const item of await nav.locator('.hemo-dock__item:visible').all()) {
                    const r=await item.boundingBox();assert(r.width>=44&&r.height>=44);assert(await item.getAttribute('aria-label'));
                }
                const horizontal=size.width<=760||size.height<=480;
                assert.equal(await nav.evaluate(n=>getComputedStyle(n).flexDirection),horizontal?'row':'column');
                if(!horizontal) assert(await p.locator('main .hc-panel,main .container,main .hc-module-card').evaluateAll((ns,right)=>ns.every(n=>!n.getClientRects().length||n.getBoundingClientRect().left>right),box.x+box.width));
                else {
                    const first=await p.locator('main').boundingBox();assert(first.y>=box.y+box.height);
                    assert(await nav.locator('.hemo-dock__compact-label').evaluateAll(ns=>ns.every(n=>!n.getClientRects().length||n.getBoundingClientRect().height<=17)));
                }
                report.routes.push({viewport:size,profile,route,orientation:horizontal?'horizontal':'vertical'});
            }
            await p.evaluate(()=>window.scrollTo(0,document.documentElement.scrollHeight));
            assert(await p.locator('[data-logout]').isVisible());
            await p.locator('[data-logout]').click();await p.waitForURL('**/login.html');
            assert.equal(await p.locator('.hemo-dock,link[href*="dock"],script[src*="dock"]').count(),0);
            await ctx.close();
        }
        report.checks.push('54 real route navigations; role masks, active section, hit areas, mobile labels and logout.');
        const ctx=await context('GESTOR_HEMOCENTRO',sizes[0]),p=await ctx.newPage();watch(p);
        await p.goto(base+'/index.html',{waitUntil:'networkidle'});
        const items=p.locator('.hemo-dock__item:visible'),stock=items.nth(1),rect=await stock.boundingBox();
        await p.mouse.move(rect.x+rect.width/2,rect.y+rect.height/2);await p.waitForTimeout(350);
        const sizesNow=await items.locator('.hemo-dock__face').evaluateAll(ns=>ns.map(n=>n.getBoundingClientRect().width));
        assert(sizesNow[1]>55.5&&sizesNow[1]<=56.1);assert(sizesNow[0]>46&&sizesNow[0]<sizesNow[1]);assert(sizesNow[2]>46&&sizesNow[2]<sizesNow[1]);assert(sizesNow[3]<44.1);
        assert.deepEqual(await stock.boundingBox(),rect,'Magnification cannot move hit targets.');
        const visualRects=await items.locator('.hemo-dock__face').evaluateAll(ns=>ns.map(n=>({top:n.getBoundingClientRect().top,bottom:n.getBoundingClientRect().bottom})));
        assert(visualRects.every((r,i)=>!i||r.top>=visualRects[i-1].bottom));
        assert.equal(await stock.locator('.hemo-dock__label').evaluate(n=>getComputedStyle(n).opacity),'1');
        assert.equal(await stock.locator('.hemo-dock__label').evaluate(n=>getComputedStyle(n).pointerEvents),'none');
        await p.screenshot({path:path.join(output,'dock-proximidade.png')});
        await p.mouse.move(800,100);await p.waitForTimeout(400);
        assert((await items.locator('.hemo-dock__face').evaluateAll(ns=>ns.map(n=>n.getBoundingClientRect().width))).every(w=>Math.abs(w-44)<.1));
        await p.keyboard.press('Tab');assert.equal(await p.locator(':focus').getAttribute('class'),'hc-skip-link');
        await p.keyboard.press('Tab');await p.keyboard.press('Tab');
        assert.equal(await p.locator(':focus').getAttribute('href'),'index.html');
        await p.keyboard.press('ArrowDown');assert.equal(await p.locator(':focus').getAttribute('href'),'estoque.html');await p.waitForTimeout(180);
        assert.equal(await stock.evaluate(n=>getComputedStyle(n).outlineWidth),'2px');
        assert.equal(await stock.locator('.hemo-dock__label').evaluate(n=>getComputedStyle(n).visibility),'visible');
        await p.keyboard.press('Escape');assert.equal(await stock.locator('.hemo-dock__label').evaluate(n=>getComputedStyle(n).visibility),'hidden');
        await p.keyboard.press('Enter');await p.waitForURL('**/estoque.html');await p.waitForLoadState('networkidle');
        await p.locator('#codigo').fill('P1-VALOR-PRESERVADO');
        const card=p.locator('.hc-stock-grid .hc-panel').first(),cardBox=await card.boundingBox();
        await p.mouse.move(cardBox.x+30,cardBox.y+30);await p.waitForTimeout(80);assert(await card.getAttribute('data-hc-lit')!==null);
        assert.equal(await p.locator('.hc-veil').getAttribute('data-renderer'),'webgl');
        await p.emulateMedia({reducedMotion:'reduce'});await p.waitForTimeout(40);
        const focusFace=p.locator('.hemo-dock__face').first();assert.equal(await focusFace.evaluate(n=>getComputedStyle(n).transform),'none');
        assert.equal(await p.evaluate(()=>document.getAnimations().length),0);
        for(const size of [sizes[2],sizes[3],sizes[4]]) {
            await p.setViewportSize(size);assert.equal(await p.locator('#codigo').inputValue(),'P1-VALOR-PRESERVADO');
            await p.locator('#salvar').evaluate(n=>n.scrollIntoView({block:'center'}));
            const visible=await p.locator('#salvar').evaluate(n=>{const r=n.getBoundingClientRect(),nav=document.querySelector('.hemo-dock').getBoundingClientRect();return r.right<nav.left||r.left>nav.right||r.bottom<nav.top||r.top>nav.bottom;});assert(visible,'Form submit must remain reachable.');
        }
        report.checks.push('Mouse proximity affects neighbours, peak 56px, stable targets, no collisions, tooltip and reset.');
        report.checks.push('Keyboard Tab/arrows/Enter/Escape; visible focus; P0 light and WebGL remain operational.');
        report.checks.push('Reduced motion stops magnification; resizing preserves fields and submit remains reachable.');
        await ctx.close();
        const mobile=await context('GESTOR_HEMOCENTRO',sizes[4]),mp=await mobile.newPage();watch(mp);
        await mp.goto(base+'/index.html',{waitUntil:'networkidle'});
        await mp.locator('.hc-nav a[href="estoque.html"]').tap();await mp.waitForURL('**/estoque.html');
        assert((await mp.locator('.hemo-dock__face').evaluateAll(ns=>ns.map(n=>getComputedStyle(n).transform))).every(t=>t==='none'));
        await mp.locator('.hc-nav a').first().focus();await mp.keyboard.press('ArrowRight');assert.equal(await mp.locator(':focus').getAttribute('href'),'estoque.html');
        await mp.keyboard.press('End');assert.equal(await mp.locator(':focus').getAttribute('data-logout'),'');
        await mp.keyboard.press('Space');await mp.waitForURL('**/login.html');await mobile.close();
        report.checks.push('Touch, horizontal keyboard arrows and Space logout; no hover magnification on phones.');
        assert.deepEqual(report.errors,[]);report.status='APROVADO';
    } finally {await browser.close();await fs.writeFile(path.join(output,'resultado-dock.json'),JSON.stringify(report,null,2));}
    console.log(JSON.stringify({status:report.status,routes:report.routes.length,checks:report.checks,errors:report.errors},null,2));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
