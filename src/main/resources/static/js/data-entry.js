// P5: React Bits Animated List concept, restricted to genuine inserted records.
// https://www.reactbits.dev/components/animated-list
const main=document.querySelector('body.hemo-internal #conteudo');
if(main){
    const reduce=matchMedia('(prefers-reduced-motion: reduce)'),seen=new Map(),active=new Set();
    function scan(){
        let budget=12;
        main.querySelectorAll('tbody,#entregas,#historico,#detalhes').forEach(container=>{
            const keys=seen.get(container.id)||new Set();seen.set(container.id,keys);
            const records=container.matches('tbody')?[...container.rows].filter(r=>r.cells.length>1):container.id==='entregas'?[...container.querySelectorAll(':scope > .entrega')]:[...container.querySelectorAll('.hc-timeline li')];
            records.forEach(record=>{
                const key=record.matches('tr')?(record.cells[0].querySelector('a')?.textContent||record.cells[0].textContent):record.matches('.entrega')?record.querySelector('h3')?.firstChild?.textContent.split(' — ')[0]:record.querySelector('time')?.dateTime+'|'+record.querySelector('.hc-event-origin')?.textContent;
                if(!key||keys.has(key))return;keys.add(key);
                if(reduce.matches||budget--<=0||record.matches('.prioritaria,.emergencial,.descartada')||record.closest('[data-hemo-entering]')||typeof record.animate!=='function')return;
                const animation=record.animate([{opacity:.7},{opacity:1}],{duration:220,easing:'ease-out'});animation.id='hemo-list-enter';active.add(animation);
                animation.finished.then(()=>animation.cancel(),()=>{}).finally(()=>active.delete(animation));
            });
        });
    }
    const stop=()=>{active.forEach(a=>a.cancel());active.clear();};
    reduce.addEventListener('change',()=>{if(reduce.matches)stop();});window.addEventListener('pagehide',stop);
    let pending=false;new MutationObserver(()=>{if(!pending){pending=true;queueMicrotask(()=>{pending=false;scan();});}}).observe(main,{childList:true,subtree:true});scan();
}
