// Local browser regression. Reuses Playwright outside the project; no npm install.
// Run --before before changing the background, then run without it to compare.
// Requires HEMOCHAIN_DISPOSABLE=1; seeds synthetic data in an isolated local DB.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const base = process.env.HEMOCHAIN_URL || 'http://127.0.0.1:18089';
assert(['127.0.0.1', 'localhost'].includes(new URL(base).hostname));
const output = path.resolve(process.env.HEMOCHAIN_UI_OUTPUT || 'target/micro-slats-evidence');
const before = process.argv.includes('--before');
const phase = before ? 'antes' : 'depois';
const sizes = [[1440,1000], [1920,1080], [834,1112], [390,844], [320,780], [844,390]];
const roles = { GESTOR_HEMOCENTRO: ['index','estoque','requisicoes','atendimento'], HOSPITAL_SOLICITANTE: ['index','requisicoes','atendimento'], OPERADOR_LOGISTICA: ['index','atendimento'] };
const report = { phase, cases: [], checks: [], errors: [] };

async function geometry(page) {
    return page.evaluate(() => [...document.querySelectorAll('.hemo-navigation,.hemo-dock,.hc-main,.hc-page-header,.hc-panel,.container,.hc-module-card,input,select,textarea,button')]
        .filter(n => n.getClientRects().length).map(n => {
            const r = n.getBoundingClientRect(), s = getComputedStyle(n);
            return { tag:n.tagName, id:n.id, class:n.className, rect:[r.x,r.y,r.width,r.height].map(v=>Math.round(v*10)/10), font:s.font, color:s.color, background:s.backgroundColor };
        }));
}
async function main() {
    await fs.mkdir(output,{recursive:true});
    assert.equal(process.env.HEMOCHAIN_DISPOSABLE,'1','Use an isolated, disposable local database.');
    async function api(route, data, role='GESTOR_HEMOCENTRO', hospitalId) {
        const r=await fetch(base+route,{method:data?'POST':'GET',headers:{'Content-Type':'application/json','X-Perfil':role,...(hospitalId?{'X-Hospital-Id':String(hospitalId)}:{})},body:data?JSON.stringify(data):undefined});
        assert(r.ok,`${route}: ${r.status} ${await r.clone().text()}`);return r.json();
    }
    const hospitals=await api('/hospitais');
    if(!hospitals.length) {
        const h=await api('/hospitais',{nome:'Hospital Sintético Micro Slats',cnpj:'00000000000000',telefone:'000000000'});
        const date=new Date().toISOString().slice(0,10);
        const b=await api('/bolsas',{codigo:'SINTETICA-MICRO-SLATS',tipoComponente:'CONCENTRADO_HEMACIAS',tipoSanguineo:'O_NEGATIVO',volumeMl:450,dataColeta:date});
        const r=await api('/requisicoes',{hospitalId:h.id,tipoComponente:'CONCENTRADO_HEMACIAS',tipoSanguineo:'O_NEGATIVO',quantidade:2,urgencia:'ROTINA',prazoLimite:date},'HOSPITAL_SOLICITANTE',h.id);
        await api(`/requisicoes/${r.id}/entregas`,{bolsaId:b.id,entregaRealizada:true});
    }
    const baseline = before ? null : JSON.parse(await fs.readFile(path.join(output,'antes.json'),'utf8'));
    const browser = await chromium.launch({channel:process.env.PLAYWRIGHT_CHANNEL || 'msedge',headless:true});
    async function context(role, size, extra={}) {
        const ctx=await browser.newContext({viewport:{width:size[0],height:size[1]}, isMobile:size[0]<600, hasTouch:size[0]<600, locale:'pt-BR', ...extra});
        if(role) await ctx.addInitScript(role=>{
            if(!sessionStorage.getItem('microSlatsTest')) {
                localStorage.setItem('hemochainUsuario',JSON.stringify({nome:'Teste visual',perfil:role}));
                sessionStorage.setItem('microSlatsTest','true');
            }
        },role);
        return ctx;
    }
    function watch(p) {
        p.on('pageerror',e=>report.errors.push(e.message));
        p.on('console',m=>{if(m.type()==='error')report.errors.push(m.text());});
        p.on('response',r=>{if(r.status()>=400)report.errors.push(`${r.status()} ${r.url()}`);});
    }
    try {
        for(const size of sizes) for(const [role,routes] of Object.entries(roles)) {
            const ctx=await context(role,size), p=await ctx.newPage(); watch(p);
            for(const route of routes) {
                await p.goto(`${base}/${route}.html`,{waitUntil:'networkidle'});
                await p.evaluate(()=>document.fonts.ready);
                if(role==='GESTOR_HEMOCENTRO' && route==='estoque') {
                    await p.locator('#bolsas button').first().click();
                    await p.locator('#detalhes .hc-timeline li').first().waitFor();
                    await p.evaluate(()=>scrollTo(0,0));
                }
                if(role==='GESTOR_HEMOCENTRO' && route==='atendimento') {
                    await p.getByRole('button',{name:'Consultar histórico'}).click();
                    await p.locator('#historico .hc-timeline li').first().waitFor();
                    await p.evaluate(()=>scrollTo(0,0));
                }
                await p.waitForTimeout(before ? 500 : 1100);
                const name=`${size.join('x')}-${role}-${route}`;
                assert(p.url().endsWith(`/${route}.html`));
                assert.equal(await p.locator('.hc-veil').count(),1);
                const info=await p.locator('.hc-veil').evaluate(n=>{
                    const r=n.getBoundingClientRect(),s=getComputedStyle(n);
                    return {renderer:n.dataset.renderer,position:s.position,pointer:s.pointerEvents,rect:[r.x,r.y,r.width,r.height],viewport:[innerWidth,innerHeight],overflow:document.documentElement.scrollWidth>innerWidth};
                });
                assert.equal(info.position,'fixed'); assert.equal(info.pointer,'none'); assert.equal(info.overflow,false);
                assert.deepEqual(info.rect,[0,0,...info.viewport]);
                assert.equal(await p.locator('.hc-nav [aria-current="page"]:visible').count(),1);
                if(!before) {
                    assert.equal(await p.locator('[data-micro-slats]').count(),1);
                    assert.equal(info.renderer,'webgl');
                    assert.equal(await p.locator('[data-micro-slats]').evaluate(n=>n.getContext('webgl2').getError()),0);
                }
                const layout=await geometry(p);
                if(baseline) assert.deepEqual(layout,baseline.cases.find(c=>c.name===name).layout,`${name}: layout/style changed`);
                if(role==='GESTOR_HEMOCENTRO' || role==='HOSPITAL_SOLICITANTE' && route==='requisicoes') {
                    await p.screenshot({path:path.join(output,`${phase}-${name}.png`)});
                    await p.evaluate(()=>scrollTo(0,document.documentElement.scrollHeight));
                    assert.deepEqual(await p.locator('.hc-veil').evaluate(n=>{const r=n.getBoundingClientRect();return [r.x,r.y,r.width,r.height];}),[0,0,...info.viewport]);
                    await p.screenshot({path:path.join(output,`${phase}-${name}-fim.png`)});
                }
                report.cases.push({name,...info,layout});
            }
            await ctx.close();
            console.log(`${phase}: ${size.join('x')} ${role}`);
        }
        for(const size of sizes.filter(s=>[1440,834,390].includes(s[0]))) {
            const ctx=await context(null,size),p=await ctx.newPage();watch(p);
            const loaded=[];p.on('request',r=>loaded.push(r.url()));
            await p.goto(base+'/login.html',{waitUntil:'networkidle'});
            await p.screenshot({path:path.join(output,`${phase}-login-${size[0]}.png`),fullPage:true});
            await p.locator('#btnAlternar').click();
            await p.screenshot({path:path.join(output,`${phase}-cadastro-${size[0]}.png`),fullPage:true});
            assert.equal(await p.locator('.hc-veil,[data-micro-slats]').count(),0);
            assert(!loaded.some(u=>/micro-slats|internal-ui|dark-veil/.test(u)));
            assert.equal(await p.locator('.login-liquid').getAttribute('data-renderer'),'webgl');
            await ctx.close();
        }
        report.checks.push('54 role/route/viewport combinations; fixed coverage at top/bottom; no horizontal overflow.', 'Login/register in three sizes load only authentication effects.');
        if(!before) await extraChecks(browser,context,watch,report,output);
        assert.deepEqual(report.errors,[]);
        report.status='PASS';
    } finally {
        await browser.close();
        await fs.writeFile(path.join(output,`${phase}.json`),JSON.stringify(report,null,2));
    }
    console.log(JSON.stringify({status:report.status,cases:report.cases.length,checks:report.checks,errors:report.errors},null,2));
}

async function extraChecks(browser,context,watch,report,output) {
    const ctx=await context('GESTOR_HEMOCENTRO',sizes[0]);
    await ctx.addInitScript(()=>{
        window.slatsProbe={frames:0,fluid:0,capture:false,hash:0,clock:0};
        const original=HTMLCanvasElement.prototype.getContext;
        HTMLCanvasElement.prototype.getContext=function(type,...args){
            const gl=original.call(this,type,...args); if(type!=='webgl2'||!gl||gl.probed)return gl;
            gl.probed=true; const names=new Map(); let screen=true,fluid=false;
            const location=gl.getUniformLocation.bind(gl),uniform=gl.uniform1f.bind(gl),draw=gl.drawArrays.bind(gl),bind=gl.bindFramebuffer.bind(gl);
            gl.getUniformLocation=(p,n)=>{const l=location(p,n);names.set(l,n);return l;};
            gl.uniform1f=(l,v)=>{const n=names.get(l);if(n==='uTime'){v=2;slatsProbe.clock++;}if(n==='uIntro')v=1;if(n==='uFluid')fluid=v>0;return uniform(l,v);};
            gl.bindFramebuffer=(t,f)=>{screen=!f;return bind(t,f);};
            gl.drawArrays=(...a)=>{
                draw(...a);if(!screen)return;slatsProbe.frames++;if(fluid)slatsProbe.fluid++;
                if(slatsProbe.capture){const data=new Uint8Array(this.width*this.height*4);gl.readPixels(0,0,this.width,this.height,gl.RGBA,gl.UNSIGNED_BYTE,data);let h=2166136261;for(let i=0;i<data.length;i++)h=Math.imul(h^data[i],16777619);slatsProbe.hash=h>>>0;slatsProbe.capture=false;}
            };return gl;
        };
    });
    const p=await ctx.newPage();watch(p);
    await p.goto(base+'/index.html',{waitUntil:'networkidle'});
    await p.waitForFunction(()=>document.querySelector('.hc-veil').dataset.renderer==='webgl');
    const contrast=await p.evaluate(()=>{
        const luminance=c=>c.map(v=>v<=.04045?v/12.92:((v+.055)/1.055)**2.4).reduce((sum,v,i)=>sum+v*[.2126,.7152,.0722][i],0);
        const opacity=Number(getComputedStyle(document.querySelector('canvas')).opacity);
        const base=[8,10,16].map(v=>v/255),peak=[.65,.118,.235];
        return ['.hc-eyebrow','.hc-description','.hc-profile','.hc-footer'].map(selector=>{
            const n=document.querySelector(selector),color=getComputedStyle(n).color.match(/[\d.]+/g).slice(0,3).map(v=>Number(v)/255);
            // The header accent is within the first opaque 208px of the contrast layer.
            const header=selector==='.hc-eyebrow';
            if(header&&n.getBoundingClientRect().bottom>208)throw Error('Header outside contrast layer');
            const alpha=opacity*(header ? .5 : 1);
            const bg=peak.map((v,i)=>v*alpha+base[i]*(1-alpha));
            return {selector,minimumRatio:(luminance(color)+.05)/(luminance(bg)+.05)};
        });
    });
    assert(contrast.every(c=>c.minimumRatio>=4.5),JSON.stringify(contrast));
    report.checks.push({contrast:'Conservative maximum slat color, including intro/cursor peaks; minimum 4.5:1',values:contrast});
    async function sample() {
        await p.evaluate(()=>{slatsProbe.capture=true;});await p.waitForFunction(()=>!slatsProbe.capture);
        return p.evaluate(()=>slatsProbe.hash);
    }
    const neutral=await sample();
    await p.mouse.move(700,720);await p.mouse.move(1000,820,{steps:18});
    const moved=await sample();assert.notEqual(moved,neutral,'Cursor must alter actual rendered pixels at a fixed wave time');
    assert((await p.evaluate(()=>slatsProbe.fluid))>0);
    await p.mouse.click(1000,820);const clicked=await sample();assert.notEqual(clicked,moved);
    report.checks.push('Original fluid trail and click splash change actual GPU pixels with wave time held fixed.');
    const card=p.locator('#btnEstoque'),r=await card.boundingBox();
    await p.mouse.move(r.x+r.width*.8,r.y+r.height*.65);await p.waitForTimeout(200);
    assert.equal(await card.getAttribute('data-hc-lit'),'');
    assert(await card.evaluate(n=>n.getAnimations().some(a=>a.effect.getKeyframes().some(k=>k.translate&&k.translate!=='0px 0px'))));
    await card.click();await p.waitForURL('**/estoque.html');await p.waitForLoadState('networkidle');
    await p.locator('#codigo').fill('CAMPO-PRESERVADO');
    for(const width of [600,1280,390,1440]) {
        await p.setViewportSize({width,height:1000});await p.waitForTimeout(120);
        assert.equal(await p.locator('#codigo').inputValue(),'CAMPO-PRESERVADO');
        assert.equal(await p.locator('canvas').evaluate(n=>n.getContext('webgl2').getError()),0);
    }
    for(const route of ['requisicoes','atendimento','index']) {
        await p.locator(`.hc-nav a[href="${route}.html"]`).click();await p.waitForURL(`**/${route}.html`);await p.waitForLoadState('networkidle');
        assert.equal(await p.locator('[data-micro-slats]').count(),1);
    }
    report.checks.push('Magic Bento glow, Magnetic translation, card click and all Dock routes; resize preserves entered form data.');
    await p.mouse.move(1430,990);
    const start=await p.evaluate(()=>slatsProbe.frames);await p.waitForTimeout(1100);
    const frames=(await p.evaluate(()=>slatsProbe.frames))-start;assert(frames>0&&frames<=35,`Frame cap: ${frames}`);
    await p.evaluate(()=>dispatchEvent(new PageTransitionEvent('pagehide',{persisted:true})));
    const paused=await p.evaluate(()=>slatsProbe.frames);await p.waitForTimeout(200);assert.equal(await p.evaluate(()=>slatsProbe.frames),paused);
    await p.evaluate(()=>dispatchEvent(new PageTransitionEvent('pageshow',{persisted:true})));await p.waitForTimeout(150);assert((await p.evaluate(()=>slatsProbe.frames))>paused);
    await p.evaluate(()=>{Object.defineProperty(document,'hidden',{configurable:true,value:true});document.dispatchEvent(new Event('visibilitychange'));});
    const hidden=await p.evaluate(()=>slatsProbe.frames);await p.waitForTimeout(200);assert.equal(await p.evaluate(()=>slatsProbe.frames),hidden);
    await p.evaluate(()=>{delete document.hidden;document.dispatchEvent(new Event('visibilitychange'));});
    await p.emulateMedia({reducedMotion:'reduce'});
    await p.waitForFunction(()=>document.querySelector('.hc-veil').dataset.renderer==='static');
    assert.equal(await p.locator('[data-micro-slats]').isVisible(),false);
    const reduced=await p.evaluate(()=>slatsProbe.frames);await p.mouse.move(600,700);await p.waitForTimeout(200);assert.equal(await p.evaluate(()=>slatsProbe.frames),reduced);
    await p.screenshot({path:path.join(output,'depois-movimento-reduzido.png')});
    await p.reload({waitUntil:'networkidle'});
    await p.keyboard.press('Tab');assert.equal(await p.locator(':focus').getAttribute('class'),'hc-skip-link');
    await p.keyboard.press('Enter');assert.equal(await p.locator(':focus').getAttribute('id'),'conteudo');
    await p.keyboard.press('Tab');assert.equal(await p.locator(':focus').evaluate(n=>getComputedStyle(n).outlineWidth),'2px');
    await p.emulateMedia({reducedMotion:'no-preference'});
    await p.waitForFunction(()=>document.querySelector('.hc-veil').dataset.renderer==='webgl');
    await p.locator('canvas').evaluate(n=>n.getContext('webgl2').getExtension('WEBGL_lose_context').loseContext());
    await p.waitForFunction(()=>document.querySelector('.hc-veil').dataset.renderer==='static');
    await p.locator('[data-logout]').click();await p.waitForURL('**/login.html');
    assert.equal(await p.locator('[data-micro-slats],.hc-veil,.hemo-dock').count(),0);
    await ctx.close();
    report.checks.push(`Frame cap (${frames} frames / 1.1s), hidden tab, BFCache lifecycle, live reduced-motion changes, keyboard focus, context loss and logout.`);
    for(const failure of ['no-webgl','modest-device','module-unavailable','shader-failure']) {
        const c=await context('GESTOR_HEMOCENTRO',sizes[0]);
        if(failure==='module-unavailable') await c.route('**/js/micro-slats.js',r=>r.abort());
        else await c.addInitScript(failure=>{
            if(failure==='modest-device')Object.defineProperty(navigator,'hardwareConcurrency',{get:()=>2});
            else {const original=HTMLCanvasElement.prototype.getContext;HTMLCanvasElement.prototype.getContext=function(type,...args){if(type==='webgl2'&&failure==='no-webgl')return null;const gl=original.call(this,type,...args);if(type==='webgl2'&&gl&&failure==='shader-failure')gl.getShaderParameter=()=>false;return gl;};}
        },failure);
        const page=await c.newPage();
        // Deliberate network/compiler failures are expected here; check page errors separately.
        page.on('pageerror',e=>report.errors.push(e.message));
        await page.goto(base+'/estoque.html',{waitUntil:'networkidle'});
        await page.waitForFunction(()=>document.querySelector('.hc-veil').dataset.renderer==='static');
        await page.locator('#codigo').fill('FALLBACK-UTILIZAVEL');
        assert.equal(await page.locator('#codigo').inputValue(),'FALLBACK-UTILIZAVEL');
        assert.equal(await page.locator('.hc-veil').evaluate(n=>getComputedStyle(n,'::before').backgroundImage.includes('micro-slats-static.png')),true);
        await page.screenshot({path:path.join(output,`depois-${failure}.png`)});
        await c.close();
    }
    report.checks.push('Static slat image with WebGL unavailable, modest hardware, failed module download and shader compilation failure; form remains usable.');
}
main().catch(e=>{console.error(e);process.exitCode=1;});
