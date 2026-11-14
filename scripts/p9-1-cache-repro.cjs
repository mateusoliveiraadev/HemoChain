// Isolated reproduction with the pre-P9 runtime and a disposable static directory.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
const out=path.resolve('target/p9-1-evidence'),base='http://127.0.0.1:18087';
const fixed=process.argv.includes('--fixed'),suffix=fixed?'fixed':'legacy';
(async()=>{
    const old=require('node:child_process').execFileSync('git',['show','b10e2d1:src/main/resources/static/login.html']);
    const cachedFile=path.join(out,'legacy-preview/login.html');
    await fs.writeFile(cachedFile,old);const past=new Date(Date.now()-3*86400000);await fs.utimes(cachedFile,past,past);
    await fs.copyFile('src/main/resources/static/js/auth-session.js',path.join(out,'legacy-preview/js/auth-session.js'));
    const browser=await chromium.launch({channel:'msedge',headless:true});
    try {
        const context=await browser.newContext({viewport:{width:1440,height:1000}}),p=await context.newPage();
        const cdp=await context.newCDPSession(p),responses=[];
        await cdp.send('Network.enable');
        cdp.on('Network.responseReceived',e=>{if(e.response.url.includes('login.html'))responses.push({url:e.response.url,fromDiskCache:e.response.fromDiskCache,headers:e.response.headers});});
        await p.goto(base+'/login.html');
        assert.equal(await p.locator('.hemo-login').count(),0);
        await p.screenshot({path:path.join(out,`${suffix}-login.png`)});
        // Simulate updating the preview assets while the unversioned HTML is cached.
        await fs.copyFile('src/main/resources/static/login.html',path.join(out,'legacy-preview/login.html'));
        // Reproduction uses the original logout implementation, not the P9.1 fix.
        const internal=await fs.readFile('src/main/resources/static/js/internal-ui.js','utf8');
        await fs.writeFile(path.join(out,'legacy-preview/js/internal-ui.js'),fixed?internal:internal.replace('window.HemoSession.logout();',"localStorage.removeItem('hemochainUsuario'); window.location.href = 'login.html';"));
        await p.goto(base+'/login.html?preview=p9');
        assert.equal(await p.locator('.hemo-login').count(),1);
        await p.locator('#btnAlternar').click();
        await p.locator('#inputNome').fill('Synthetic cache reproduction');
        await p.locator('#inputEmail').fill('cache-'+Date.now()+'@example.test');
        await p.locator('#inputSenha').fill('Synthetic-P91-2026');
        await p.locator('#btnAcao').click();
        await p.locator('#statusMsg').filter({hasText:'Conta criada'}).waitFor();
        await p.locator('#btnAcao').click();await p.waitForURL('**/index.html');
        await p.locator('[data-logout]').click();await p.waitForURL('**/login.html');
        const legacyReturned=await p.locator('.hemo-login').count()===0;
        await p.evaluate(()=>document.fonts.ready);await p.waitForTimeout(350);
        await p.screenshot({path:path.join(out,`${suffix}-after-logout.png`)});
        await fs.writeFile(path.join(out,fixed?'cache-fixed.json':'cache-repro.json'),JSON.stringify({fixed,legacyReturned,responses,sessionCleared:await p.evaluate(()=>localStorage.getItem('hemochainUsuario')===null)},null,2));
        console.log({legacyReturned,responses:responses.map(r=>({url:r.url,fromDiskCache:r.fromDiskCache}))});
        assert.equal(legacyReturned,!fixed,'Only the legacy logout must return the stale cached login');
    } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
