// Read-only smoke on the running Docker app using an existing synthetic account.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
const base='http://localhost:18084',out=path.resolve('target/p9-1-evidence');
(async()=>{
    const browser=await chromium.launch({channel:'msedge',headless:true}),report={base,flows:[],errors:[]};
    try{
        for(const [size,width,height]of [['desktop',1440,1000],['tablet',834,1112],['mobile',390,844]]){
            const ctx=await browser.newContext({viewport:{width,height},isMobile:width<600,hasTouch:width<600});
            const p=await ctx.newPage();p.on('pageerror',e=>report.errors.push(e.message));
            const response=await p.goto(base+'/login.html',{waitUntil:'networkidle'});assert.match(response.headers()['cache-control'],/no-store/);
            await p.locator('.login-card').waitFor();await p.screenshot({path:path.join(out,`docker-login-${size}.png`),fullPage:true});
            await p.locator('#inputEmail').fill('gestor_hemocentro.1794281605703@example.test');await p.locator('#inputSenha').fill('DemoSintetica-E04-2026');await p.locator('#btnAcao').click();await p.waitForURL('**/index.html');
            for(const module of ['estoque','requisicoes','atendimento','index']){await p.locator(`.hc-nav a[href="${module}.html"]`).click();await p.waitForURL(`**/${module}.html`);await p.waitForLoadState('networkidle');if(size==='desktop')await p.screenshot({path:path.join(out,`docker-modulo-${module}.png`),fullPage:true});}
            await p.locator('[data-logout]').click();await p.waitForURL('**/login.html');await p.locator('.login-card').waitFor();
            assert.equal(await p.evaluate(()=>localStorage.getItem('hemochainUsuario')),null);
            await p.waitForFunction(()=>document.querySelector('.login-liquid')?.dataset.renderer==='webgl');
            await p.evaluate(()=>document.fonts.ready);
            await p.locator('#titulo').evaluate(n=>Promise.all(n.getAnimations().map(a=>a.finished)));
            await p.screenshot({path:path.join(out,`docker-logout-${size}.png`),fullPage:true});
            await p.goto(base+'/');await p.waitForURL('**/login.html');await p.locator('.login-card').waitFor();
            report.flows.push({size,realLogin:true,modules:4,logoutPremium:true,sessionCleared:true,rootGuarded:true});await ctx.close();
        }
        assert.deepEqual(report.errors,[]);report.status='PASS';console.log(report);
    }finally{await browser.close();await fs.writeFile(path.join(out,'resultado-docker.json'),JSON.stringify(report,null,2));}
})().catch(e=>{console.error(e);process.exitCode=1;});
