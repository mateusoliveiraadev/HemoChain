// P6: Fade Content concept. Traditional navigation, no artificial loading.
// https://www.reactbits.dev/animations/fade-content
// P2 owns heading/description motion. Only independent surfaces fade here.
const main=document.querySelector('body.hemo-internal #conteudo');
if(main){
    const reduce=matchMedia('(prefers-reduced-motion: reduce)'),seen=new WeakSet(),active=new Map();
    function finish(panel){const animation=active.get(panel);if(animation){active.delete(panel);animation.cancel();}panel.removeAttribute('data-hemo-entering');}
    function protect(target){active.forEach((a,panel)=>{if(panel.contains(target))finish(panel);});}
    function scan(){
        main.querySelectorAll('.hc-panel,.container,.hc-module-card').forEach(panel=>{
            if(seen.has(panel)||!panel.getClientRects().length)return;seen.add(panel);
            if(reduce.matches||panel.querySelector('#alertas,#pendencias,[data-tone="error"],.hemo-field-error')||panel.contains(document.activeElement)||typeof panel.animate!=='function')return;
            panel.getAnimations({subtree:true}).filter(a=>a.id==='hemo-list-enter').forEach(a=>a.cancel());
            panel.setAttribute('data-hemo-entering','');
            const animation=panel.animate([{opacity:.85},{opacity:1}],{duration:panel.matches('.hc-module-card')?350:300,easing:'cubic-bezier(.22,1,.36,1)'});
            animation.id='hemo-content-enter';active.set(panel,animation);
            animation.finished.then(()=>finish(panel),()=>{if(active.get(panel)===animation){active.delete(panel);panel.removeAttribute('data-hemo-entering');}});
        });
    }
    ['focusin','pointerdown','invalid'].forEach(type=>main.addEventListener(type,event=>protect(event.target),true));
    const stop=()=>{[...active.keys()].forEach(finish);};reduce.addEventListener('change',()=>{if(reduce.matches)stop();});window.addEventListener('pagehide',stop);
    let pending=false;new MutationObserver(records=>{
        records.forEach(record=>{const node=record.target;if(node.nodeType===1&&(node.matches('[data-tone="error"]')||node.closest('.hemo-field-error')))protect(node);});
        if(!pending){pending=true;queueMicrotask(()=>{pending=false;scan();});}
    }).observe(main,{childList:true,subtree:true,attributes:true,attributeFilter:['hidden','data-tone']});scan();
}
