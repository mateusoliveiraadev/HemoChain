// Renderiza uma captura identificada do tracker local, a partir do Markdown real.
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs=require('node:fs/promises');
const path=require('node:path');
const escapar=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
(async()=>{
    const markdown=await fs.readFile('entrega-04/bug-tracker.md','utf8');
    const linhas=markdown.split(/\r?\n/).filter(l=>l.startsWith('|'));
    const colunas=l=>l.split('|').slice(1,-1).map(t=>t.trim().replace(/`/g,''));
    const cabecalho=colunas(linhas[0]);const dados=linhas.slice(2).map(colunas);
    const browser=await chromium.launch({channel:process.env.PLAYWRIGHT_CHANNEL || 'msedge',headless:true});
    try {
        const p=await browser.newPage({viewport:{width:1600,height:1050},deviceScaleFactor:1});
        await p.setContent(`<!DOCTYPE html><html lang="pt-br"><meta charset="utf-8"><style>body{font:18px 'Segoe UI',sans-serif;margin:0;padding:40px;background:#f5f5f5;color:#292929}main{background:white;border-top:8px solid #b71c1c;padding:30px;border-radius:10px}h1{color:#8e1717;margin-top:0}p{line-height:1.5}table{width:100%;border-collapse:collapse;font-size:16px}th{background:#8e1717;color:white}td,th{text-align:left;vertical-align:top;padding:17px 12px;border:1px solid #ddd;line-height:1.5}tr:nth-child(even){background:#faf5f5}.aviso{padding:15px;background:#fff4db;border-left:5px solid #d38200}.rodape{font-size:15px;color:#555}</style><main><h1>HemoChain | Issue/Bug Tracker — Entrega 04</h1><p class="aviso">TRACKER LOCAL — captura renderizada do arquivo entrega-04/bug-tracker.md. Não é uma captura do GitHub Issues. Publicação externa pendente de autorização.</p><table><thead><tr>${cabecalho.map(c=>`<th>${escapar(c)}</th>`).join('')}</tr></thead><tbody>${dados.map(row=>`<tr>${row.map(c=>`<td>${escapar(c)}</td>`).join('')}</tr>`).join('')}</tbody></table><p class="rodape">Defeitos e validações registrados na branch feature/entrega-04-rastreabilidade-recebimento. Acesso continua didático por contexto declarado; não equivale a autenticação de produção.</p></main></html>`);
        await p.screenshot({path:path.resolve('entrega-04/evidencias/bug-tracker.png'),fullPage:true});
    } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
