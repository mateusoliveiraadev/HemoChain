// Native adaptation of React Bits Dock: left/dark, 44 -> 56px, reach 110px.
// https://www.reactbits.dev/components/dock
// Shared concepts: cosine proximity, damped spring, labels and active indicators.
// No React/dependencies, bounce, auto-hide, route interception or access changes.
export function initDock(root) {
    if (!root) return;
    const items = [...root.querySelectorAll('.hemo-dock__item')].filter(item => !item.hidden);
    const faces = items.map(item => item.querySelector('.hemo-dock__face'));
    const reduce = matchMedia('(prefers-reduced-motion: reduce)');
    const fine = matchMedia('(hover: hover) and (pointer: fine)');
    const horizontal = matchMedia('(max-width: 760px), (max-height: 480px)');
    const base = 44, peak = 56, reach = 110, omega = 26;
    const motion = items.map(() => ({ size: base, velocity: 0 }));
    let frame = 0, previousTime = 0, pointer = null, focused = -1, centers = [];
    const enabled = () => !reduce.matches && fine.matches && !horizontal.matches && !document.hidden;
    function measure() {
        centers = items.map(item => { const r = item.getBoundingClientRect(); return r.top + r.height / 2; });
    }
    function stop() {
        cancelAnimationFrame(frame); frame = 0; previousTime = 0; pointer = null; focused = -1;
        motion.forEach((state,index) => { state.size = base; state.velocity = 0; faces[index].style.removeProperty('--dock-scale'); });
    }
    function animate(time) {
        frame = 0;
        if (!enabled()) { stop(); return; }
        const dt = Math.min((time - (previousTime || time - 16)) / 1000, .032);
        previousTime = time;
        const origin = focused >= 0 ? centers[focused] : pointer;
        let moving = false;
        motion.forEach((state,index) => {
            const distance = origin === null ? reach : Math.abs(origin - centers[index]);
            const influence = distance < reach ? Math.cos(distance / reach * Math.PI / 2) ** 2 : 0;
            const target = base + (peak - base) * influence;
            // Analytic critically damped spring: gentle response, bounded hit slots.
            const offset = state.size - target, coefficient = state.velocity + omega * offset;
            const decay = Math.exp(-omega * dt);
            state.size = Math.min(peak, Math.max(base, target + (offset + coefficient * dt) * decay));
            state.velocity = (state.velocity - omega * coefficient * dt) * decay;
            if (Math.abs(state.size - target) < .01 && Math.abs(state.velocity) < .1) {
                state.size = target; state.velocity = 0;
            } else moving = true;
            faces[index].style.setProperty('--dock-scale', (state.size / base).toFixed(4));
        });
        if (moving) frame = requestAnimationFrame(animate); else previousTime = 0;
    }
    function wake() {
        if (!enabled()) { stop(); return; }
        if (!frame) frame = requestAnimationFrame(animate);
    }
    root.addEventListener('pointerenter', () => { measure(); root.removeAttribute('data-label-dismissed'); });
    root.addEventListener('pointermove', event => {
        if (event.pointerType === 'touch' || !enabled()) return;
        pointer = event.clientY; focused = -1; wake();
    }, { passive: true });
    root.addEventListener('pointerleave', () => {
        pointer = null;
        if (!root.contains(document.activeElement)) root.removeAttribute('data-label-dismissed');
        wake();
    });
    items.forEach(item => item.addEventListener('pointerenter', () => root.removeAttribute('data-label-dismissed')));
    root.addEventListener('focusin', event => {
        root.removeAttribute('data-label-dismissed'); measure();
        focused = event.target.matches(':focus-visible') ? items.indexOf(event.target) : -1;
        wake();
    });
    root.addEventListener('focusout', () => {
        queueMicrotask(() => {
            focused = items.findIndex(item => item === document.activeElement && item.matches(':focus-visible'));
            if (!root.contains(document.activeElement)) root.removeAttribute('data-label-dismissed');
            wake();
        });
    });
    root.addEventListener('keydown', event => {
        if (event.key === 'Escape') { root.setAttribute('data-label-dismissed',''); pointer = null; focused = -1; wake(); return; }
        const index = items.indexOf(document.activeElement);
        if (index < 0) return;
        const backward = horizontal.matches ? 'ArrowLeft' : 'ArrowUp';
        const forward = horizontal.matches ? 'ArrowRight' : 'ArrowDown';
        let next;
        if (event.key === backward) next = (index - 1 + items.length) % items.length;
        else if (event.key === forward) next = (index + 1) % items.length;
        else if (event.key === 'Home') next = 0;
        else if (event.key === 'End') next = items.length - 1;
        else return;
        event.preventDefault(); items[next].focus({ preventScroll: true });
    });
    const reset = () => { stop(); measure(); };
    [reduce,fine,horizontal].forEach(query => query.addEventListener('change',reset));
    window.addEventListener('resize',reset,{ passive: true });
    window.addEventListener('pagehide',stop);
    window.addEventListener('pageshow',reset);
    document.addEventListener('visibilitychange',stop);
    measure();
}
