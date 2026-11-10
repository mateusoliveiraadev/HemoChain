package com.hemochain.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.hemochain.entity.*;
import com.hemochain.repository.*;
import com.hemochain.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** BDD HTTP com commits reais: nao usa @Transactional no teste. */
@SpringBootTest(properties = {"recebimento.verificacao-ms=3600000", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
class AtendimentoHospitalarAceitacaoTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired HospitalService hospitais;
    @Autowired RequisicaoService requisicoes;
    @Autowired RequisicaoRepository requisicaoRepository;
    @Autowired BolsaRepository bolsas;
    @Autowired RastreabilidadeService rastreabilidade;
    @SpyBean Clock relogio;
    private Hospital hospital;
    private Instant inicio;

    @BeforeEach void preparar() {
        inicio = Instant.now();
        doReturn(inicio).when(relogio).instant();
        hospital = hospitais.cadastrar(new Hospital("Hospital sintetico E04", "SINTETICO", "0000"));
    }

    private JsonNode executar(MockHttpServletRequestBuilder pedido, int status) throws Exception {
        String corpo = mvc.perform(pedido).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(status)).andReturn().getResponse().getContentAsString();
        return corpo.isBlank() ? json.nullNode() : json.readTree(corpo);
    }
    private MockHttpServletRequestBuilder gestor(MockHttpServletRequestBuilder pedido) {
        return pedido.header("X-Perfil", "GESTOR_HEMOCENTRO");
    }
    private MockHttpServletRequestBuilder hospital(MockHttpServletRequestBuilder pedido) {
        return pedido.header("X-Perfil", "HOSPITAL_SOLICITANTE").header("X-Hospital-Id", hospital.getId());
    }
    private MockHttpServletRequestBuilder corpo(MockHttpServletRequestBuilder pedido, Object dados) throws Exception {
        return pedido.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(dados));
    }
    private Map<String, Object> dadosBolsa(String tipo, int diasColeta) {
        return Map.of("codigo", "E04-" + UUID.randomUUID(), "tipoComponente", "CONCENTRADO_HEMACIAS",
                "tipoSanguineo", tipo, "volumeMl", 450, "dataColeta", LocalDate.now().minusDays(diasColeta).toString());
    }
    private Long bolsa(String tipo, int diasColeta) throws Exception {
        return executar(corpo(gestor(post("/bolsas")), dadosBolsa(tipo, diasColeta)), 201).get("id").asLong();
    }
    private Long requisicao(int quantidade) {
        return requisicoes.cadastrar(new RequisicaoHospitalar(hospital.getId(), TipoComponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.A_NEGATIVO, quantidade, UrgenciaRequisicao.ROTINA, LocalDate.now().plusDays(1))).getId();
    }
    private String url(Long r, Long e) { return "/requisicoes/" + r + "/entregas/" + e; }
    private JsonNode entrega(Long r, Long b, int status) throws Exception {
        return executar(corpo(gestor(post("/requisicoes/" + r + "/entregas")),
                Map.of("bolsaId", b, "entregaRealizada", true)), status);
    }
    private JsonNode receber(Long r, Long e, String condicao, int status) throws Exception {
        return executar(corpo(hospital(post(url(r, e) + "/recebimento")),
                Map.of("condicao", condicao, "observacao", condicao.equals("NAO_CONFORME") ? "Embalagem avariada" : "")), status);
    }
    private StatusRequisicao status(Long r) { return requisicaoRepository.findById(r).orElseThrow().getStatus(); }

    @Test void bddRecebimentoIntegroEUtilizacaoComHistoricoCronologico() throws Exception {
        Long b = bolsa("O_NEGATIVO", 0), r = requisicao(1);
        Long e = entrega(r, b, 201).get("id").asLong();
        assertEquals(StatusRequisicao.ENTREGUE, status(r));
        receber(r, e, "INTEGRA", 200);
        assertEquals(StatusRequisicao.RECEBIDA, status(r));
        assertEquals(StatusBolsa.RECEBIDA, bolsas.findById(b).orElseThrow().getStatus());
        executar(hospital(post(url(r, e) + "/utilizacao")), 200);
        assertEquals(StatusRequisicao.UTILIZADA, status(r));
        JsonNode eventos = executar(gestor(get("/bolsas/" + b + "/historico")), 200);
        assertEquals(List.of("CADASTRADA", "ENTREGA_REGISTRADA", "RECEBIDA", "UTILIZADA"),
                eventos.findValuesAsText("tipo"));
        for (int i = 0; i < eventos.size(); i++) {
            assertFalse(eventos.get(i).get("origem").asText().isBlank());
            assertEquals(b, eventos.get(i).get("bolsaId").asLong());
            if (i > 0) assertFalse(Instant.parse(eventos.get(i).get("momento").asText())
                    .isBefore(Instant.parse(eventos.get(i - 1).get("momento").asText())));
        }
    }

    @Test void bddPendenciaAposPrazoAlertaUmaVezEPreservaAposConfirmacao() throws Exception {
        Long b = bolsa("O_NEGATIVO", 0), r = requisicao(1);
        Long e = entrega(r, b, 201).get("id").asLong();
        doReturn(inicio.plus(Duration.ofHours(24))).when(relogio).instant();
        executar(gestor(get("/requisicoes/pendencias")), 200);
        assertEquals(StatusRequisicao.ENTREGUE, status(r));
        doReturn(inicio.plus(Duration.ofHours(24)).plusSeconds(1)).when(relogio).instant();
        JsonNode pendentes = executar(gestor(get("/requisicoes/pendencias")), 200);
        assertTrue(pendentes.findValuesAsText("id").contains(e.toString()));
        executar(gestor(get("/requisicoes/pendencias")), 200);
        assertEquals(StatusRequisicao.RECEBIMENTO_PENDENTE, status(r));
        assertEquals(1, rastreabilidade.historico(b).stream().filter(ev -> ev.getTipo() == TipoEventoBolsa.CONFIRMACAO_ATRASADA).count());
        receber(r, e, "INTEGRA", 200);
        assertEquals(StatusRequisicao.RECEBIDA, status(r));
        JsonNode depois = executar(gestor(get("/requisicoes/pendencias")), 200);
        assertFalse(java.util.stream.StreamSupport.stream(depois.spliterator(), false)
                .anyMatch(item -> item.get("id").asLong() == e));
        assertEquals(4, rastreabilidade.historico(b).size());
    }

    @Test void impedeDuplicidadeDeRecebimentoEDeUso() throws Exception {
        Long b = bolsa("O_NEGATIVO", 0), r = requisicao(1), e = entrega(r, b, 201).get("id").asLong();
        receber(r, e, "INTEGRA", 200); receber(r, e, "INTEGRA", 409);
        executar(hospital(post(url(r, e) + "/utilizacao")), 200);
        executar(hospital(post(url(r, e) + "/utilizacao")), 409);
        assertEquals(4, rastreabilidade.historico(b).size());
    }

    @Test void impedeUsoAntesDeReceberENaoConformidadeBloqueiaUso() throws Exception {
        Long b = bolsa("O_NEGATIVO", 0), r = requisicao(1), e = entrega(r, b, 201).get("id").asLong();
        executar(hospital(post(url(r, e) + "/utilizacao")), 409);
        receber(r, e, "NAO_CONFORME", 200);
        executar(hospital(post(url(r, e) + "/utilizacao")), 409);
        assertEquals(StatusBolsa.BLOQUEADA, bolsas.findById(b).orElseThrow().getStatus());
        assertEquals(StatusRequisicao.NAO_CONFORME, status(r));
    }

    @Test void hospitalDiferenteESemContextoNaoLeNemConfirmaNemUsa() throws Exception {
        Long b = bolsa("O_NEGATIVO", 0), r = requisicao(1), e = entrega(r, b, 201).get("id").asLong();
        String base = "/requisicoes/" + r + "/entregas";
        executar(get(base).header("X-Perfil", "HOSPITAL_SOLICITANTE"), 403);
        executar(get(base).header("X-Perfil", "HOSPITAL_SOLICITANTE").header("X-Hospital-Id", hospital.getId() + 999), 403);
        executar(corpo(post(url(r, e) + "/recebimento").header("X-Perfil", "HOSPITAL_SOLICITANTE")
                .header("X-Hospital-Id", hospital.getId() + 999), Map.of("condicao", "INTEGRA")), 403);
        executar(post(url(r, e) + "/utilizacao").header("X-Perfil", "HOSPITAL_SOLICITANTE")
                .header("X-Hospital-Id", hospital.getId() + 999), 403);
        assertEquals(StatusRequisicao.ENTREGUE, status(r));
        assertEquals(2, rastreabilidade.historico(b).size());
    }

    @Test void listaHospitalarFiltraHospitalEImpedeCadastroComOutroContexto() throws Exception {
        Long r = requisicao(1);
        JsonNode lista = executar(hospital(get("/requisicoes")), 200);
        assertTrue(lista.findValuesAsText("id").contains(r.toString()));
        for (JsonNode item : lista) assertEquals(hospital.getId(), item.get("hospital").get("id").asLong());
        executar(get("/requisicoes").header("X-Perfil", "HOSPITAL_SOLICITANTE"), 403);
        executar(corpo(hospital(post("/requisicoes")), Map.of("hospitalId", hospital.getId()+999,
                "tipoComponente", "CONCENTRADO_HEMACIAS", "tipoSanguineo", "A_NEGATIVO", "quantidade", 1,
                "urgencia", "ROTINA", "prazoLimite", LocalDate.now().plusDays(1).toString())), 403);
    }

    @Test void perfisNegadosEOperadorConsultaSomentePendencias() throws Exception {
        Long b = bolsa("O_NEGATIVO", 0), r = requisicao(1), e = entrega(r, b, 201).get("id").asLong();
        executar(corpo(gestor(post(url(r, e) + "/recebimento")), Map.of("condicao", "INTEGRA")), 403);
        executar(gestor(post(url(r, e) + "/utilizacao")), 403);
        executar(hospital(get("/bolsas/" + b + "/historico")), 403);
        executar(hospital(get("/requisicoes/pendencias")), 403);
        executar(get("/requisicoes/pendencias").header("X-Perfil", "OPERADOR_LOGISTICA"), 200);
        executar(get("/bolsas/" + b + "/historico"), 403);
    }

    @Test void naoAssociaIncompativelVencidaNemBolsaJaEntregue() throws Exception {
        Long r = requisicao(2), b = bolsa("B_POSITIVO", 0);
        entrega(r, b, 409);
        assertEquals(StatusBolsa.DISPONIVEL, bolsas.findById(b).orElseThrow().getStatus());
        entrega(r, bolsa("O_NEGATIVO", 35), 409);
        Long valida = bolsa("O_NEGATIVO", 0);
        entrega(r, valida, 201);
        entrega(requisicao(1), valida, 409);
        assertEquals(2, rastreabilidade.historico(valida).size());
    }

    @Test void quantidadeParcialNaoFinalizaRequisicaoNemPermiteExcesso() throws Exception {
        Long r = requisicao(2), e1 = entrega(r, bolsa("O_NEGATIVO", 0), 201).get("id").asLong();
        assertEquals(StatusRequisicao.PARCIALMENTE_ENTREGUE, status(r));
        receber(r, e1, "INTEGRA", 200);
        assertEquals(StatusRequisicao.PARCIALMENTE_RECEBIDA, status(r));
        Long e2 = entrega(r, bolsa("O_NEGATIVO", 0), 201).get("id").asLong();
        entrega(r, bolsa("O_NEGATIVO", 0), 409);
        receber(r, e2, "INTEGRA", 200);
        assertEquals(StatusRequisicao.RECEBIDA, status(r));
        executar(hospital(post(url(r, e1) + "/utilizacao")), 200);
        assertEquals(StatusRequisicao.PARCIALMENTE_UTILIZADA, status(r));
        executar(hospital(post(url(r, e2) + "/utilizacao")), 200);
        assertEquals(StatusRequisicao.UTILIZADA, status(r));
    }

    @Test void bloqueiaEdicaoDaBolsaEntregueEProtegeHistoricoViaApi() throws Exception {
        Long b = bolsa("O_NEGATIVO", 0), r = requisicao(1); entrega(r, b, 201);
        executar(corpo(gestor(put("/bolsas/" + b)), dadosBolsa("O_NEGATIVO", 0)), 409);
        executar(corpo(gestor(put("/bolsas/" + b + "/historico")), Map.of("tipo", "FORJADO")), 405);
        executar(gestor(delete("/bolsas/" + b + "/historico")), 405);
        assertEquals(2, rastreabilidade.historico(b).size());
    }

    @Test void cadastroEdicaoEDescarteGeramEventosComRetrato() throws Exception {
        Long b = bolsa("O_NEGATIVO", 0);
        executar(corpo(gestor(put("/bolsas/" + b)), dadosBolsa("O_NEGATIVO", 35)), 200);
        var eventos = rastreabilidade.historico(b);
        assertEquals(List.of(TipoEventoBolsa.CADASTRADA, TipoEventoBolsa.ATUALIZADA, TipoEventoBolsa.DESCARTADA),
                eventos.stream().map(EventoBolsa::getTipo).toList());
        assertTrue(eventos.get(1).getDetalhes().contains("Antes:"));
        assertEquals("SISTEMA:VALIDADE", eventos.get(2).getOrigem());
        executar(gestor(get("/bolsas/" + b + "/historico")), 200);
        assertEquals(3, rastreabilidade.historico(b).size());
    }

    @Test void legadoSemEventosNaoRecebeHistoricoFabricado() {
        BolsaHemocomponente b = new BolsaHemocomponente();
        b.setCodigo("LEGADO-"+UUID.randomUUID()); b.setTipoComponente(TipoComponente.CONCENTRADO_HEMACIAS);
        b.setTipoSanguineo(TipoSanguineo.O_NEGATIVO); b.setVolumeMl(450); b.setDataColeta(LocalDate.now());
        b.setValidade(LocalDate.now().plusDays(35)); b.setStatus(StatusBolsa.DISPONIVEL);
        bolsas.saveAndFlush(b);
        assertTrue(rastreabilidade.historico(b.getId()).isEmpty());
    }

    @Test void validaAtestacaoCondicaoObservacaoERequisicaoDaEntrega() throws Exception {
        Long b = bolsa("O_NEGATIVO", 0), r = requisicao(1);
        executar(corpo(gestor(post("/requisicoes/" + r + "/entregas")), Map.of("bolsaId", b)), 400);
        Long e = entrega(r, b, 201).get("id").asLong();
        executar(corpo(hospital(post(url(r, e) + "/recebimento")), Map.of()), 400);
        executar(corpo(hospital(post(url(r, e) + "/recebimento")), Map.of("condicao", "NAO_CONFORME")), 400);
        receber(requisicao(1), e, "INTEGRA", 404);
        assertEquals(2, rastreabilidade.historico(b).size());
    }

    @Test void vencimentoAposEntregaImpedeRecebimentoIntegroEUtilizacao() throws Exception {
        Long b = bolsa("O_NEGATIVO", 34), r = requisicao(1), e = entrega(r, b, 201).get("id").asLong();
        doReturn(inicio.plus(Duration.ofDays(2))).when(relogio).instant();
        receber(r, e, "INTEGRA", 409);
        receber(r, e, "NAO_CONFORME", 200);
        executar(hospital(post(url(r, e) + "/utilizacao")), 409);
        Long b2 = bolsa("O_NEGATIVO", 34), r2 = requisicao(1);
        doReturn(inicio).when(relogio).instant();
        Long e2 = entrega(r2, b2, 201).get("id").asLong(); receber(r2, e2, "INTEGRA", 200);
        doReturn(inicio.plus(Duration.ofDays(2))).when(relogio).instant();
        executar(hospital(post(url(r2, e2) + "/utilizacao")), 409);
    }
}
