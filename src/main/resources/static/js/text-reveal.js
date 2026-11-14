// P2: native progressive enhancement of React Bits Blur Text / Fade Content.
// https://www.reactbits.dev/text-animations/blur-text
// https://www.reactbits.dev/animations/fade-content
// CSS provides the same bounded entry without JS. Word animations inherit its
// clock, so slow module loading never hides/restarts an already visible title.
export function initTextReveal(header) {
    if (!header || header.dataset.textEntry) return;
    const title = header.querySelector(':scope > h1.hc-title-reveal');
    const reduce = matchMedia('(prefers-reduced-motion: reduce)');
    const animations = new Set();
    header.dataset.textEntry = 'started';
    function complete() {
        header.dataset.textEntry = 'complete';
        animations.forEach(animation => animation.cancel()); animations.clear();
    }
    reduce.addEventListener('change', () => { if (reduce.matches) complete(); });
    window.addEventListener('pagehide', complete, { once: true });
    if (!title || reduce.matches || document.hidden || typeof title.animate !== 'function') { complete(); return; }
    const fallback = title.getAnimations().find(animation => animation.animationName === 'hc-title-reveal');
    // No running fallback means the entry already ended (or CSS is unavailable).
    if (!fallback || fallback.playState === 'finished') { complete(); return; }
    const elapsed = Math.max(0, Number(fallback.currentTime) || 0);
    if (elapsed >= 560) { complete(); return; }
    const original = title.textContent;
    const tokens = original.match(/\S+|\s+/gu) || [];
    const count = tokens.filter(token => /\S/u.test(token)).length;
    if (!count) { complete(); return; }
    const visual = document.createElement('span'); visual.setAttribute('aria-hidden','true');
    const words = [];
    tokens.forEach(token => {
        if (!/\S/u.test(token)) { visual.appendChild(document.createTextNode(token)); return; }
        const word = document.createElement('span'); word.className = 'hc-title-word'; word.textContent = token;
        visual.appendChild(word); words.push(word);
    });
    // One heading name; no duplicate hidden text. Visible content stays selectable.
    title.setAttribute('aria-label',original.trim());
    title.classList.add('hc-title--words'); title.replaceChildren(visual);
    const spread = Math.min(140, Math.max(0,count - 1) * 34);
    const duration = 560 - spread;
    words.forEach((word,index) => {
        const animation = word.animate([
            {opacity:.45,filter:'blur(5px)',transform:'translateY(4px)'},
            {opacity:1,filter:'blur(0)',transform:'translateY(0)'}
        ], {duration,delay:count > 1 ? index * spread / (count - 1) : 0,easing:'cubic-bezier(.22,1,.36,1)',fill:'backwards'});
        animation.currentTime = elapsed; animations.add(animation);
    });
    Promise.all([...animations].map(animation => animation.finished.catch(() => {}))).then(complete);
}

// Separate entry point: an unavailable animation asset cannot block P0/P1.
if (document.body?.classList.contains('hemo-internal')) initTextReveal(document.querySelector('.hc-page-header'));
