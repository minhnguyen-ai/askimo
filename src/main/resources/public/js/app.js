// Loader for HTML partials
async function loadText(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`${url} -> ${res.status}`);
    return res.text();
}
function compileAlpineTree(el) {
    if (window.Alpine?.initTree) Alpine.initTree(el);
    else setTimeout(() => compileAlpineTree(el), 0);
}

// Settings helpers
export function readSettings() {
    try { return JSON.parse(localStorage.getItem("askimo.settings") || "{}"); }
    catch { return {}; }
}
export function saveSettings(obj) {
    localStorage.setItem("askimo.settings", JSON.stringify(obj));
    document.dispatchEvent(new Event("askimo:settings-updated"));
}
export function apiBase() {
    const s = readSettings();
    const host = s.host || location.hostname || "127.0.0.1";
    const port = s.port || (location.port ? Number(location.port) : 8080);
    const proto = location.protocol?.startsWith("https") ? "https" : "http";
    return `${proto}://${host}${port ? ":"+port : ""}`;
}
export function apiHeaders() {
    const s = readSettings();
    const h = { "Content-Type": "application/json" };
    if (s.token) h["X-Askimo-Token"] = s.token;
    return h;
}

// Alpine root
window.shell = function () {
    return {
        page: "chat",
        sidebarOpen: false,

        async go(p) {
            this.page = p;
            const html = await loadText(`/views/${p}.html`);
            this.$refs.outlet.innerHTML = html;
            compileAlpineTree(this.$refs.outlet);
            history.replaceState({}, "", `#${p}`);
            document.dispatchEvent(new CustomEvent("askimo:view-mounted", { detail: { page: p } }));
        },

        async init() {
            const p = location.hash?.slice(1) || "chat";
            await this.go(p);
        }
    };
};
