// Native, bounded adaptation of React Bits Magnet.
// https://www.reactbits.dev/animations/magnet
// Separate translate channel preserves transform, Dock and text animations.
if (!window.hemoMagnetInitialized) {
    window.hemoMagnetInitialized = true;
    const fine = matchMedia('(hover: hover) and (pointer: fine)');
    const reduce = matchMedia('(prefers-reduced-motion: reduce)');
    const selector = 'button,a.hc-module-card[href],a.hc-button[href],[role="button"],#btnAlternar';
    const states = new Map();
    let pointer = null, frame = 0, last = 0, pressed = false;
    const enabled = () => fine.matches && !reduce.matches && !document.hidden;
    const usable = el => !el.closest('.hemo-navigation,[inert]') && !el.matches(':disabled,[aria-disabled="true"]') && el.getClientRects().length;
    function clear() {
        cancelAnimationFrame(frame); frame = 0; last = 0; pointer = null;
        states.forEach(s => s.animation.cancel()); states.clear();
    }
    function tick(time) {
        frame = 0;
        if (!enabled()) { clear(); return; }
        if (pressed) return;
        let winner = null, distance = Infinity, targetX = 0, targetY = 0;
        if (pointer) document.querySelectorAll(selector).forEach(el => {
            if (!usable(el)) return;
            const r = el.getBoundingClientRect(), s = states.get(el);
            const x = r.left - (s?.x || 0), y = r.top - (s?.y || 0);
            const dx = pointer.x - (x + r.width / 2), dy = pointer.y - (y + r.height / 2);
            const outside = Math.hypot(Math.max(0,Math.abs(dx)-r.width/2),Math.max(0,Math.abs(dy)-r.height/2));
            const score = outside * 1000 + Math.hypot(dx,dy);
            if (outside > 18 || score >= distance) return;
            winner = el; distance = score;
            const critical = /descart|exclu|remov|confirm|recebimento|utiliza/i.test(el.textContent);
            const max = critical ? 2 : Math.min(5,r.height * .1);
            const strength = critical ? .04 : .1, falloff = 1 - outside / 18;
            const length = Math.hypot(dx,dy) * strength || 1;
            const factor = Math.min(1,max/length) * falloff;
            targetX = dx * strength * factor; targetY = dy * strength * factor;
        });
        if (winner && !states.has(winner) && typeof winner.animate === 'function') {
            const animation = winner.animate([{translate:'0px 0px'}],{duration:1,fill:'both'});
            animation.pause(); animation.currentTime=1; states.set(winner,{x:0,y:0,animation});
        }
        const dt = Math.min(32,time-(last || time-16)); last = time;
        const blend = 1-Math.exp(-dt/45); let moving = false;
        states.forEach((s,el) => {
            if (!el.isConnected || !usable(el)) { s.animation.cancel(); states.delete(el); return; }
            const x = el === winner ? targetX : 0, y = el === winner ? targetY : 0;
            s.x += (x-s.x)*blend; s.y += (y-s.y)*blend;
            if (Math.hypot(s.x-x,s.y-y)<.02) { s.x=x; s.y=y; } else moving=true;
            if (el !== winner && s.x===0 && s.y===0) {s.animation.cancel();states.delete(el);return;}
            s.animation.effect.setKeyframes([{translate:`${s.x.toFixed(3)}px ${s.y.toFixed(3)}px`}]);
        });
        if (moving) frame=requestAnimationFrame(tick); else last=0;
    }
    const wake = () => {if (!frame && !pressed) frame=requestAnimationFrame(tick);};
    document.addEventListener('pointermove',e=>{
        if(e.pointerType!=='mouse'||!enabled()) {clear();return;}
        pointer={x:e.clientX,y:e.clientY};wake();
    },{passive:true});
    document.documentElement.addEventListener('pointerleave',()=>{pointer=null;wake();});
    // Freeze the current hit position throughout pointerdown -> click.
    document.addEventListener('pointerdown',e=>{if(e.pointerType!=='mouse')clear();pressed=true;cancelAnimationFrame(frame);frame=0;},{passive:true});
    const release=()=>setTimeout(()=>{pressed=false;pointer=null;wake();},0);
    document.addEventListener('pointerup',release,{passive:true});
    document.addEventListener('pointercancel',()=>{pressed=false;clear();},{passive:true});
    document.addEventListener('keydown',clear);
    window.addEventListener('scroll',clear,{passive:true,capture:true});
    window.addEventListener('resize',clear,{passive:true});
    window.addEventListener('blur',()=>{pressed=false;clear();});
    window.addEventListener('pagehide',clear);
    document.addEventListener('visibilitychange',clear);
    [fine,reduce].forEach(q=>q.addEventListener('change',clear));
    new MutationObserver(()=>{states.forEach((s,el)=>{if(!el.isConnected||!usable(el)){s.animation.cancel();states.delete(el);}});})
        .observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['disabled','aria-disabled','hidden']});
}
