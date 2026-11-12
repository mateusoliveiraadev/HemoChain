// The academic application stores its signed-in user locally (no server session).
(() => {
    const key = 'hemochainUsuario';
    function current() {
        try { return JSON.parse(localStorage.getItem(key) || 'null'); }
        catch { localStorage.removeItem(key); return null; }
    }
    let returning = false;
    async function returnToLogin() {
        if (returning) return;
        returning = true;
        // Migrate browsers that already cached the pre-P9 HTML without Cache-Control.
        // Reload the canonical resource in HTTP cache before navigating to that same URL.
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 5000);
        try { await fetch('/login.html', { cache: 'reload', signal: controller.signal }); }
        catch { /* Navigation can retry. */ }
        finally { clearTimeout(timeout); }
        window.location.replace('/login.html');
    }
    function requireUser() {
        if (!current()) returnToLogin();
    }
    function logout() {
        localStorage.removeItem(key);
        return returnToLogin();
    }
    window.HemoSession = Object.freeze({ current, requireUser, logout });
    if (document.currentScript.hasAttribute('data-auth-required')) {
        requireUser();
        // Re-check pages restored from the browser's back/forward cache and other tabs.
        window.addEventListener('pageshow', requireUser);
        window.addEventListener('storage', event => {
            if (event.key === key || event.key === null) requireUser();
        });
    }
})();
