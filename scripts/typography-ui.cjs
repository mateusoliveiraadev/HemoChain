// P2 regression: actual fonts, accessible text, bounded motion and graceful fallback.
// Reuses externally installed Playwright. Only read-only requests to localhost.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert=require('node:assert/strict'),fs=require('node:fs/promises'),path=require('node:path');
const base=process.env.HEMOCHAIN_URL || 'http://localhost:18084';
assert(['localhost','127.0.0.1'].includes(new URL(base).hostname));
const output=path.resolve(process.env.HEMOCHAIN_UI_OUTPUT || 'target/typography-evidence');
const report={pages:[],checks:[],errors:[]};
async function main(){
 await fs.mkdir(output,{recursive:true});const browser=await chromium.launch({channel:process.env.PLAYWRIGHT_CHANNEL || 'msedge',headless:true});
 async function context(options={}){
  const ctx=await browser.newContext({viewport:{width:1440,height:1000},locale:'pt-BR',...options});
  await ctx.addInitScript(()=>{
   localStorage.setItem('hemochainUsuario',JSON.stringify({nome:'Validação P2',perfil:'GESTOR_HEMOCENTRO'}));
   window.__textTest={words:[],css:[]};
   const original=Element.prototype.animate;
   Element.prototype.animate=function(frames,options){if(frames.some(f=>'filter' in f))window.__textTest.words.push({text:this.textContent,heading:!!this.closest('h1'),frames,options});return original.call(this,frames,options);};
   document.addEventListener('animationstart',event=>{const style=getComputedStyle(event.target);window.__textTest.css.push({name:event.animationName,duration:style.animationDuration,delay:style.animationDelay,header:!!event.target.closest('.hc-page-header')});});
  });return ctx;
 }
 function watch(p){p.on('pageerror',e=>report.errors.push(e.message));p.on('response',r=>{if(r.status()>=400&&!r.url().endsWith('/favicon.ico'))report.errors.push(`${r.status()} ${r.url()}`);});}
 async function fonts(ctx,p,selector){const cdp=await ctx.newCDPSession(p);try{await cdp.send('DOM.enable');await cdp.send('CSS.enable');const {root}=await cdp.send('DOM.getDocument');const {nodeId}=await cdp.send('DOM.querySelector',{nodeId:root.nodeId,selector});return (await cdp.send('CSS.getPlatformFontsForNode',{nodeId})).fonts;}finally{await cdp.detach();}}
 try{
  const ctx=await context(),p=await ctx.newPage();watch(p);
  for(const page of ['index','estoque','requisicoes','atendimento']){
   await p.goto(`${base}/${page}.html`,{waitUntil:'domcontentloaded'});
   await p.evaluate(()=>document.fonts.ready);await p.waitForFunction(()=>document.querySelector('.hc-page-header').dataset.textEntry==='complete');
   const result=await p.evaluate(()=>{
    const h=document.querySelector('h1'),s=getComputedStyle(h),range=document.createRange();range.selectNodeContents(h);const selection=getSelection();selection.removeAllRanges();selection.addRange(range);const selected=selection.toString();selection.removeAllRanges();
    const brand=document.querySelector('.hemo-navigation__brand').getBoundingClientRect(),dock=document.querySelector('.hemo-dock').getBoundingClientRect();
    return {text:h.textContent,name:h.getAttribute('aria-label'),selected,size:parseFloat(s.fontSize),weight:s.fontWeight,words:window.__textTest.words,css:window.__textTest.css,animations:document.getAnimations().length,gap:dock.top-brand.bottom,fontFaces:[...document.fonts].map(f=>({family:f.family,display:f.display,status:f.status})),source:h.innerHTML};
   });
   assert.equal(result.name,result.text);assert.equal(result.selected,result.text);assert(result.size>=28&&result.size<=32);assert.equal(result.weight,'700');assert.equal(result.gap,24);
   assert.equal(result.words.length,result.text.trim().split(/\s+/u).length);assert(result.words.every(x=>x.heading));
   assert.equal(Math.max(...result.words.map(x=>x.options.duration+x.options.delay)),560);
   assert(result.words[0].frames[0].opacity>0);assert(result.words.every(x=>x.frames[0].transform==='translateY(4px)'));
   const fade=result.css.find(x=>x.name==='hc-subtitle-reveal');assert(fade&&fade.header);assert.equal(fade.duration,'0.38s');assert.equal(fade.delay,'0.1s');
   assert(result.css.every(x=>x.header));assert.equal(result.animations,0);assert(result.fontFaces.filter(x=>/Manrope|Inter/.test(x.family)).every(x=>x.display==='swap'));
   const headingFonts=await fonts(ctx,p,'.hc-title-word'),interfaceFonts=await fonts(ctx,p,'.hc-description');
   assert(headingFonts.some(f=>f.familyName.includes('Manrope')&&f.isCustomFont&&f.glyphCount));assert(interfaceFonts.some(f=>f.familyName.includes('Inter')&&f.isCustomFont&&f.glyphCount));
   assert.match(await p.locator('h1').ariaSnapshot(),/heading .*\[level=1\]/);
   if(page==='estoque'){
    const count=result.words.length;await p.locator('#atualizar').click();await p.waitForLoadState('networkidle');
    if(await p.locator('#bolsas button').count()){await p.locator('#bolsas button').first().click();await p.locator('#painelDetalhes').waitFor();await p.waitForFunction(()=>document.querySelector('#detalhes .hc-timeline li'));}
    // P0 hover/focus colour transitions remain; record/title animations do not replay.
    assert.equal(await p.evaluate(()=>window.__textTest.words.length),count);assert.equal(await p.evaluate(()=>document.getAnimations().filter(a=>!(a instanceof CSSTransition)&&!a.effect.getKeyframes().some(f=>'translate' in f)).length),0);
    assert.equal(await p.locator('h1').innerHTML(),result.source);
   }
   report.pages.push({page,size:result.size,titleFont:headingFonts,interfaceFont:interfaceFonts,totalMotion:560,accessibleName:result.name});
  }
  await p.goto(base+'/index.html',{waitUntil:'networkidle'});await p.keyboard.press('Tab');assert.equal(await p.locator(':focus').getAttribute('class'),'hc-skip-link');await p.keyboard.press('Enter');assert.equal(await p.locator(':focus').getAttribute('id'),'conteudo');await p.keyboard.press('Tab');assert.equal(await p.locator(':focus').evaluate(n=>getComputedStyle(n).outlineWidth),'2px');
  await ctx.close();report.checks.push('Manrope and Inter actually rendered; heading name read once and selectable; timing 560/380+100ms; no data-refresh replay; keyboard.');

  const reduced=await context({reducedMotion:'reduce'}),rp=await reduced.newPage();watch(rp);await rp.goto(base+'/estoque.html',{waitUntil:'networkidle'});
  assert.equal(await rp.evaluate(()=>window.__textTest.words.length),0);assert.equal(await rp.evaluate(()=>document.getAnimations().length),0);
  assert.equal(await rp.locator('h1').evaluate(n=>getComputedStyle(n).filter),'none');
  await rp.emulateMedia({reducedMotion:'no-preference'});await rp.waitForTimeout(100);assert.equal(await rp.evaluate(()=>window.__textTest.words.length),0);assert.equal(await rp.evaluate(()=>document.getAnimations().length),0);await reduced.close();
  const mid=await context(),midp=await mid.newPage();watch(midp);await midp.goto(base+'/atendimento.html',{waitUntil:'domcontentloaded'});await midp.emulateMedia({reducedMotion:'reduce'});await midp.waitForFunction(()=>document.querySelector('.hc-page-header').dataset.textEntry==='complete');assert.equal(await midp.evaluate(()=>document.getAnimations().length),0);await mid.close();
  report.checks.push('Reduced motion at startup and during entry; text remains static when preference changes back.');

  const fallback=await context({viewport:{width:390,height:844}});await fallback.route('**/fonts/*.woff2',route=>route.abort());const fp=await fallback.newPage();watch(fp);await fp.goto(base+'/estoque.html',{waitUntil:'networkidle'});await fp.waitForFunction(()=>document.querySelector('.hc-page-header').dataset.textEntry==='complete');
  assert(!(await fonts(fallback,fp,'.hc-title-word')).some(f=>/Manrope|Inter/.test(f.familyName)&&f.isCustomFont));assert(await fp.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));assert(await fp.locator('h1').innerText());await fp.screenshot({path:path.join(output,'fontes-indisponiveis.png'),fullPage:true});await fallback.close();
  report.checks.push('Font request failures keep text readable with system fallbacks and no horizontal overflow.');

  const noEffect=await context();await noEffect.route('**/js/text-reveal.js',route=>route.abort());const np=await noEffect.newPage();await np.goto(base+'/index.html',{waitUntil:'networkidle'});await np.waitForTimeout(600);
  assert.equal(await np.locator('.hc-title-word').count(),0);assert.equal(await np.locator('h1').evaluate(n=>getComputedStyle(n).opacity),'1');assert.equal(await np.locator('h1').evaluate(n=>getComputedStyle(n).filter),'none');assert.equal(await np.locator('.hemo-dock__item:visible').count(),5);assert.equal(await np.locator('.hc-veil').getAttribute('data-renderer'),'webgl');await noEffect.close();
  const noJS=await browser.newContext({javaScriptEnabled:false,viewport:{width:390,height:844}}),nj=await noJS.newPage();await nj.goto(base+'/estoque.html',{waitUntil:'networkidle'});await nj.waitForTimeout(600);assert.equal(await nj.locator('h1').innerText(),'Estoque e validade');assert.equal(await nj.locator('h1').evaluate(n=>getComputedStyle(n).opacity),'1');await noJS.close();
  report.checks.push('CSS-only entry without animation JS; no blocking of P0/P1; original title available with JavaScript disabled.');

  const late=await context();await late.route('**/js/text-reveal.js',async route=>{await new Promise(resolve=>setTimeout(resolve,1100));await route.continue();});const lp=await late.newPage();watch(lp);await lp.goto(base+'/requisicoes.html',{waitUntil:'networkidle'});assert.equal(await lp.evaluate(()=>window.__textTest.words.length),0);assert.equal(await lp.locator('h1').evaluate(n=>getComputedStyle(n).opacity),'1');assert.equal(await lp.locator('h1').evaluate(n=>getComputedStyle(n).filter),'none');await late.close();
  report.checks.push('Late animation script never hides or replays an already visible heading.');

  function luminance(hex){const rgb=hex.match(/\w\w/g).map(x=>parseInt(x,16)/255).map(x=>x<=.04045?x/12.92:((x+.055)/1.055)**2.4);return .2126*rgb[0]+.7152*rgb[1]+.0722*rgb[2];}
  report.contrast=[['F5EEF2','11131C'],['B9AFBB','11131C'],['A49AAB','11131C'],['C56B87','080A10']].map(([fg,bg])=>{const a=luminance(fg),b=luminance(bg),ratio=(Math.max(a,b)+.05)/(Math.min(a,b)+.05);assert(ratio>=4.5);return{fg,bg,ratio};});
  assert.deepEqual(report.errors,[]);report.status='APROVADO';
 }finally{await browser.close();await fs.writeFile(path.join(output,'resultado-tipografia.json'),JSON.stringify(report,null,2));}
 console.log(JSON.stringify({status:report.status,pages:report.pages.length,checks:report.checks,contrast:report.contrast,errors:report.errors},null,2));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
