import { apiBase, apiHeaders, readSettings } from "../app.js";

window.chatPage = function () {
    return {
        messages: [],
        prompt: "",

        async send() {
            const q = this.prompt.trim();
            if (!q) return;
            this.messages.push({ role: "user", text: q });
            this.prompt = "";
            this.scroll();

            try {
                const res = await fetch(`${apiBase()}/api/chat`, {
                    method: "POST",
                    headers: apiHeaders(),
                    body: JSON.stringify({ prompt: q })
                });
                const json = await res.json();
                this.messages.push({ role: "assistant", text: json.message ?? String(json) });
            } catch (e) {
                this.messages.push({ role: "assistant", text: "[Error] " + (e?.message || e) });
            } finally {
                this.scroll();
            }
        },

        scroll() {
            this.$nextTick(() => {
                const box = this.$refs.chatBox;
                if (readSettings().autoScroll !== false) {
                    box.scrollTop = box.scrollHeight;
                }
            });
        },

        init() {
            const draft = JSON.parse(localStorage.getItem("askimo.chatDraft") || "null");
            if (draft?.prompt) { this.prompt = draft.prompt; localStorage.removeItem("askimo.chatDraft"); }
            document.addEventListener("askimo:settings-updated", () => {});
        }
    };
};
