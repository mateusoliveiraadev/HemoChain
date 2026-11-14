// Real Spring/API regression. Run against a disposable local database, never production.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
const base=process.env.HEMOCHAIN_URL || 'http://127.0.0.1:18086';
assert(['localhost','127.0.0.1'].includes(new URL(base).hostname));
const out=path.resolve(process.env.HEMOCHAIN_UI_OUTPUT || 'target/p9-1-evidence');
const sizes=[['desktop',1440,1000],['tablet',834,1112],['mobile',390,844]];
const roles={GESTOR_HEMOCENTRO:['index','estoque','requisicoes','atendimento'],HOSPITAL_SOLICITANTE:['index','requisicoes','atendimento'],OPERADOR_LOGISTICA:['index','atendimento']};
const report={base,database:'isolated H2 in memory; real register/login endpoints; no API mocks',flows:[],errors:[],checks:[]};
async function premium(p){
    await p.locator('.hemo-login .login-card').waitFor();
    await p.waitForFunction(()=>document.querySelector('.login-liquid')?.dataset.renderer);
    await p.evaluate(()=>document.fonts.ready);
    await p.locator('#titulo').evaluate(n=>Promise.all(n.getAnimations().map(a=>a.finished)));
    assert.equal(await p.locator('.login-brand img').evaluate(n=>n.complete&&n.naturalWidth>0),true);
    assert.equal(await p.locator('.hc-veil,.hemo-dock').count(),0);
    assert(await p.locator('.login-card').evaluate(n=>getComputedStyle(n).backdropFilter.includes('blur')));
    assert(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
}
(async()=>{
    await fs.mkdir(out,{recursive:true});
    const browser=await chromium.launch({channel:'msedge',headless:true});
    try {
        for(const [size,width,height] of sizes)for(const [role,routes] of Object.entries(roles)){
            const ctx=await browser.newContext({viewport:{width,height},isMobile:width<600,hasTouch:width<600,locale:'pt-BR',...(size==='desktop'&&role==='GESTOR_HEMOCENTRO'?{recordVideo:{dir:path.join(out,'video'),size:{width:1440,height:1000}}}:{})});
            const p=await ctx.newPage();p.on('pageerror',e=>report.errors.push(e.message));
            const email=`p91-${role.toLowerCase()}-${Date.now()}@example.test`,password='Synthetic-P91-2026';
            await p.goto(base+'/login.html',{waitUntil:'networkidle'});await premium(p);
            if(role==='GESTOR_HEMOCENTRO')await p.screenshot({path:path.join(out,`login-${size}.png`),fullPage:true});
            await p.locator('#btnAlternar').click();
            await p.locator('#inputNome').fill('Synthetic P9.1');await p.locator('#inputEmail').fill(email);await p.locator('#inputSenha').fill(password);await p.locator('#inputPerfil').selectOption(role);
            if(role==='GESTOR_HEMOCENTRO')await p.screenshot({path:path.join(out,`cadastro-${size}.png`),fullPage:true});
            await p.locator('#btnAcao').click();await p.locator('#statusMsg').filter({hasText:'Conta criada'}).waitFor();
            assert.equal(await p.evaluate(()=>localStorage.getItem('hemochainUsuario')),null);
            await p.locator('#inputSenha').fill('invalid-password');await p.locator('#btnAcao').click();
            await p.waitForFunction(()=>document.querySelector('#statusMsg').dataset.tone==='error');
            assert.equal(await p.evaluate(()=>localStorage.getItem('hemochainUsuario')),null);
            for(const route of routes){
                await p.locator('#inputEmail').fill(email);await p.locator('#inputSenha').fill(password);await p.locator('#inputSenha').press('Enter');await p.waitForURL('**/index.html');
                assert.equal(await p.evaluate(()=>JSON.parse(localStorage.getItem('hemochainUsuario')).perfil),role);
                // Navigate through every module allowed by this role before each logout.
                for(const module of routes){await p.locator(`.hc-nav a[href="${module}.html"]`).click();await p.waitForURL(`**/${module}.html`);await p.waitForLoadState('networkidle');}
                await p.locator(`.hc-nav a[href="${route}.html"]`).click();await p.waitForURL(`**/${route}.html`);await p.waitForLoadState('networkidle');
                assert.equal(await p.locator('[data-logout]').count(),1);
                if(size==='desktop'&&role==='GESTOR_HEMOCENTRO')await p.screenshot({path:path.join(out,`modulo-${route}.png`),fullPage:true});
                await p.locator('[data-logout]').click();await p.waitForURL('**/login.html');await premium(p);
                assert.equal(await p.evaluate(()=>localStorage.getItem('hemochainUsuario')),null);
                if(role==='GESTOR_HEMOCENTRO')await p.screenshot({path:path.join(out,`logout-${size}-${route}.png`),fullPage:true});
                // A restored authenticated page must be checked again, as must direct entry.
                await p.goBack();await p.waitForURL('**/login.html');await premium(p);
                for(const module of ['index','estoque','requisicoes','atendimento']){await p.goto(base+`/${module}.html`);await p.waitForURL('**/login.html');await premium(p);}
                assert.equal(await p.evaluate(()=>localStorage.getItem('hemochainUsuario')),null);
                report.flows.push({size,role,logoutFrom:route,registered:true,realLogin:true,newLoginVisible:true,sessionCleared:true,backAndDirectEntryGuarded:true});
            }
            if(size==='desktop'&&role==='GESTOR_HEMOCENTRO'){
                // The legacy public function remains supported and uses the same implementation.
                await p.locator('#inputEmail').fill(email);await p.locator('#inputSenha').fill(password);await p.locator('#btnAcao').click();await p.waitForURL('**/index.html');
                const second=await ctx.newPage();await second.goto(base+'/index.html',{waitUntil:'networkidle'});
                await p.evaluate(()=>{sair();});await p.waitForURL('**/login.html');await premium(p);
                await second.waitForURL('**/login.html');await premium(second);await second.close();
                report.checks.push('Legacy sair() delegates to canonical logout; another authenticated tab also returns to login.');
            }
            await ctx.close();
        }
        // Measure actual uploaded fluid velocity, rather than only checking a canvas exists.
        const ctx=await browser.newContext({viewport:{width:1440,height:1000}});
        await ctx.addInitScript(()=>{window.p91Uploads=0;const original=HTMLCanvasElement.prototype.getContext;HTMLCanvasElement.prototype.getContext=function(...args){const gl=original.apply(this,args);if(gl&&args[0]==='webgl'){const upload=gl.texSubImage2D.bind(gl);gl.texSubImage2D=(...a)=>{window.p91Uploads++;window.p91Flow=Array.from(a.at(-1));return upload(...a);};}return gl;};});
        const p=await ctx.newPage();await p.goto(base+'/login.html',{waitUntil:'networkidle'});await premium(p);
        assert.equal(await p.locator('.login-liquid').getAttribute('data-renderer'),'webgl');
        await p.waitForTimeout(150);const neutral=await p.evaluate(()=>p91Flow);
        await p.mouse.move(60,300);for(let i=0;i<24;i++){await p.mouse.move(60+i*15,300+Math.sin(i/4)*80);await p.waitForTimeout(16);}
        await p.waitForTimeout(80);assert.notDeepEqual(await p.evaluate(()=>p91Flow),neutral);
        assert.equal(await p.evaluate(()=>document.querySelector('canvas').getContext('webgl').getError()),0);
        await p.screenshot({path:path.join(out,'liquid-interativo.png')});
        for(const id of ['btnAcao','btnAlternar']){const button=p.locator('#'+id),r=await button.boundingBox();await p.mouse.move(r.x+r.width/2+14,r.y+r.height/2-8);await p.waitForTimeout(250);assert(await button.evaluate(e=>e.getAnimations().some(a=>a.effect.getKeyframes().some(k=>'translate' in k))));}
        await p.emulateMedia({reducedMotion:'reduce'});await p.waitForTimeout(250);
        const stopped=await p.evaluate(()=>p91Uploads);await p.waitForTimeout(250);assert.equal(await p.evaluate(()=>p91Uploads),stopped,'Reduced motion stops fluid updates');
        report.checks.push('Liquid Ether reacts to real mouse movement; WebGL error=0; Magnetic on both buttons; reduced-motion supported.');
        await ctx.close();
        const cache={};for(const route of ['/','/index.html','/login.html','/css/login.css','/js/auth-session.js']){const response=await fetch(base+route);cache[route]={status:response.status,cacheControl:response.headers.get('cache-control')};assert.match(cache[route].cacheControl,/no-store/);}
        for(const route of ['/login','/logout']){const response=await fetch(base+route);assert.equal(response.status,404);cache[route]={status:response.status};}
        report.cache=cache;assert.deepEqual(report.errors,[]);report.status='PASS';
    }finally{await browser.close();await fs.writeFile(path.join(out,'resultado-ui.json'),JSON.stringify(report,null,2));}
    console.log(JSON.stringify({status:report.status,flows:report.flows.length,checks:report.checks,errors:report.errors},null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
