// Homologacao real da interface. Instale Playwright fora do codigo Java e use somente base sintetica.
// PLAYWRIGHT_MODULE pode apontar para a instalacao local; HEMOCHAIN_URL padrao abaixo.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const base = process.env.HEMOCHAIN_URL || 'http://localhost:18084';
const destino = path.resolve(process.env.HEMOCHAIN_UI_OUTPUT || 'entrega-04/evidencias');
const sufixo = Date.now();
const erros = [];
const resultados = [];

async function principal() {
    await fs.mkdir(destino, {recursive:true});
    const browser = await chromium.launch({channel:process.env.PLAYWRIGHT_CHANNEL || 'msedge',headless:true});
    async function pagina() {
        const context = await browser.newContext({viewport:{width:1440,height:1000},locale:'pt-BR',timezoneId:'America/Fortaleza'});
        const p = await context.newPage();
        p.on('pageerror', e => erros.push(e.message));
        return p;
    }
    async function conta(p, perfil) {
        await p.goto(base+'/login.html');
        await p.locator('#btnAlternar').click();
        await p.locator('#inputNome').fill('Demo E04 '+perfil);
        await p.locator('#inputEmail').fill(`${perfil.toLowerCase()}.${sufixo}@example.test`);
        await p.locator('#inputSenha').fill('DemoSintetica-E04-2026');
        await p.locator('#inputPerfil').selectOption(perfil);
        await p.locator('#btnAcao').click();
        await p.locator('#btnAcao').filter({hasText:'ENTRAR'}).waitFor();
        await p.locator('#btnAcao').click();
        await p.waitForURL('**/index.html');
    }
    async function screenshot(p,nome) { await p.screenshot({path:path.join(destino,nome),fullPage:true}); }
    async function bolsa(p,codigo, dias=0) {
        await p.goto(base+'/estoque.html');
        await p.locator('#codigo').fill(codigo);
        await p.locator('#tipoComponente').selectOption('CONCENTRADO_HEMACIAS');
        await p.locator('#tipoSanguineo').selectOption('O_NEGATIVO');
        await p.locator('#volumeMl').fill('450');
        const coleta=new Date();coleta.setDate(coleta.getDate()-dias);
        await p.locator('#dataColeta').fill(coleta.toLocaleDateString('en-CA',{timeZone:'America/Fortaleza'}));
        await p.locator('#salvar').click();
        await p.locator('#mensagem').filter({hasText:'Bolsa salva'}).waitFor();
        assert.match(await p.locator('#detalhes').innerText(),/CADASTRADA/);
    }
    try {
        const gestor = await pagina(), hospital = await pagina();
        await conta(gestor,'GESTOR_HEMOCENTRO'); await conta(hospital,'HOSPITAL_SOLICITANTE');
        const codigo='DEMO-E04-'+sufixo;
        await bolsa(gestor,codigo,1);
        await screenshot(gestor,'01-estoque-historico.png');
        resultados.push('Cadastro de bolsa e historico automatico visiveis no estoque.');
        await hospital.goto(base+'/requisicoes.html');
        await hospital.locator('#hospNome').fill('Hospital Demo E04 '+sufixo);
        await hospital.locator('#cadastrarHospital').click();
        await hospital.locator('#statusHospital').filter({hasText:'Hospital cadastrado!'}).waitFor();
        await hospital.locator('#hospitalContexto option').filter({hasText:'Hospital Demo E04 '+sufixo}).waitFor({state:'attached'});
        const hospitalId=await hospital.locator('#hospitalContexto option').filter({hasText:'Hospital Demo E04 '+sufixo}).getAttribute('value');
        await hospital.locator('#hospitalContexto').selectOption(hospitalId);
        await hospital.locator('#reqTipoSanguineo').selectOption('A_NEGATIVO');
        await hospital.locator('#reqPrazo').fill('2027-01-01');
        await hospital.locator('#registrarRequisicao').click();
        await hospital.locator('#statusRequisicao').filter({hasText:'Requisicao registrada!'}).waitFor();
        const link=hospital.locator('#listaRequisicoes a').first();await link.waitFor();
        const rId=(await link.innerText()).replace('#','');
        await screenshot(hospital,'02-requisicao-hospitalar.png');
        await gestor.goto(base+'/atendimento.html?requisicao='+rId);
        await gestor.locator('#bolsa option').filter({hasText:codigo}).waitFor({state:'attached'});
        const bId=await gestor.locator('#bolsa option').filter({hasText:codigo}).getAttribute('value');
        await gestor.locator('#bolsa').selectOption(bId);
        await screenshot(gestor,'03-sugestao-fefo.png');
        await gestor.locator('#entregaRealizada').check();
        await gestor.getByRole('button',{name:'Registrar entrega realizada'}).click();
        await gestor.locator('#mensagem').filter({hasText:'Entrega registrada.'}).waitFor();
        await screenshot(gestor,'04-entrega-atestada.png');
        await hospital.goto(base+'/atendimento.html?requisicao='+rId);
        await hospital.getByRole('button',{name:'Confirmar recebimento'}).waitFor();
        const observacao='Embalagem integra.\n\nObservacao adicional preservada integralmente.';
        await hospital.locator('#entregas textarea').fill(observacao);
        await hospital.getByRole('button',{name:'Confirmar recebimento'}).click();
        await hospital.locator('#mensagem').filter({hasText:'Recebimento confirmado'}).waitFor();
        assert.match(await hospital.locator('#resumo').innerText(),/RECEBIDA/);
        assert((await hospital.locator('#entregas dd').allTextContents()).includes(observacao));
        await screenshot(hospital,'05-recebimento-integro.png');
        await hospital.getByRole('button',{name:'Registrar utilização'}).click();
        await hospital.locator('#mensagem').filter({hasText:'Utilização registrada.'}).waitFor();
        assert.match(await hospital.locator('#resumo').innerText(),/UTILIZADA/);
        await screenshot(hospital,'06-utilizacao.png');
        await gestor.locator('#atualizar').click();
        await gestor.getByRole('button',{name:'Consultar histórico'}).click();
        await gestor.locator('#historico').filter({hasText:'UTILIZADA'}).waitFor();
        assert.equal(await gestor.locator('#historico .hc-timeline li').count(),4);
        assert((await gestor.locator('#historico .hc-event-detail').allTextContents()).some(texto=>texto.includes(observacao)));
        await screenshot(gestor,'07-historico-final.png');
        resultados.push('Requisicao, FEFO, entrega atestada, recebimento e utilizacao executados pela interface.');
        const trace=await gestor.request.get(base+`/bolsas/${bId}/historico`,{headers:{'X-Perfil':'GESTOR_HEMOCENTRO'}});
        const eventos=await trace.json();
        assert.deepEqual(eventos.map(e=>e.tipo),['CADASTRADA','ENTREGA_REGISTRADA','RECEBIDA','UTILIZADA']);
        await fs.writeFile(path.join(destino,'fluxo-ui.json'),JSON.stringify({data:new Date().toISOString(),base,requisicaoId:Number(rId),bolsaId:Number(bId),hospitalId:Number(hospitalId),codigo,eventos,resultados},null,2));
        // Segunda entrega real sintetica, deixada sem confirmacao para verificar o prazo pela UI.
        // Para executar rapidamente, use RECEBIMENTO_PRAZO=PT5S apenas no ambiente de homologacao.
        if(process.env.HEMOCHAIN_UI_PENDENCIA==='true') {
            const codigoP=codigo+'-P'; await bolsa(gestor,codigoP);
            await hospital.goto(base+'/requisicoes.html');await hospital.locator('#reqPrazo').fill('2027-01-01');
            await hospital.locator('#registrarRequisicao').click();
            await hospital.locator('#statusRequisicao').filter({hasText:'Requisicao registrada!'}).waitFor();
            const ultimo=hospital.locator('#listaRequisicoes a').first();await ultimo.waitFor();
            const pendenteId=(await ultimo.innerText()).replace('#','');
            await gestor.goto(base+'/atendimento.html?requisicao='+pendenteId);
            await gestor.locator('#bolsa option').filter({hasText:codigoP}).waitFor({state:'attached'});
            await gestor.locator('#bolsa').selectOption(await gestor.locator('#bolsa option').filter({hasText:codigoP}).getAttribute('value'));
            await gestor.locator('#entregaRealizada').check();await gestor.getByRole('button',{name:'Registrar entrega realizada'}).click();
            await gestor.locator('#mensagem').filter({hasText:'Entrega registrada.'}).waitFor();
            await gestor.waitForTimeout(6500);
            await gestor.locator('#verificar').click();
            await gestor.locator('#mensagem').filter({hasText:'Verificação concluída.'}).waitFor();
            assert.match(await gestor.locator('#resumo').innerText(),/RECEBIMENTO_PENDENTE/);
            assert.match(await gestor.locator('#pendencias').innerText(),new RegExp(codigoP));
            await screenshot(gestor,'08-alerta-pendente.png');
            resultados.push('Alerta apos prazo real de 5 segundos demonstrado pela interface.');
        }
        assert.deepEqual(erros,[],'Erros JavaScript na interface');
        await fs.writeFile(path.join(destino,'resultado-ui.json'),JSON.stringify({data:new Date().toISOString(),status:'APROVADO',resultados,errosJavaScript:erros},null,2));
        console.log(JSON.stringify({status:'APROVADO',resultados,errosJavaScript:erros},null,2));
    } finally {await browser.close();}
}
principal().catch(e=>{console.error(e);process.exitCode=1;});
