package com.hemochain.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemochain.entity.*;
import com.hemochain.repository.*;
import com.hemochain.service.ValidadeService;

@SpringBootTest
@AutoConfigureMockMvc
class EstoqueControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private BolsaRepository bolsas;
    @Autowired private AlertaValidadeRepository alertas;
    @Autowired private ValidadeService validade;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    private String corpo(String codigo, String tipo, LocalDate coleta) throws Exception {
        return json.writeValueAsString(Map.of("codigo", codigo, "tipoComponente", tipo,
                "tipoSanguineo", "O_NEGATIVO", "volumeMl", 450, "dataColeta", coleta.toString()));
    }

    private BolsaHemocomponente cadastrar(String codigo, LocalDate coleta) throws Exception {
        String resposta = mvc.perform(post("/bolsas").header("X-Perfil", "GESTOR_HEMOCENTRO")
                .contentType(MediaType.APPLICATION_JSON).content(corpo(codigo, "PLAQUETAS", coleta)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = json.readTree(resposta).get("id").asLong();
        return bolsas.findById(id).orElseThrow();
    }

    @Test
    @Transactional
    void cadastroCalculaValidadeENormalizaCodigo() throws Exception {
        BolsaHemocomponente bolsa = cadastrar("  teste-cadastro  ", LocalDate.now());
        assertEquals("TESTE-CADASTRO", bolsa.getCodigo());
        assertEquals(LocalDate.now().plusDays(5), bolsa.getValidade());
        assertEquals(StatusBolsa.DISPONIVEL, bolsa.getStatus());
    }

    @Test
    @Transactional
    void duplicidadeBloqueadaMesmoComEspacosEMinusculas() throws Exception {
        cadastrar("DUPLICADA", LocalDate.now());
        mvc.perform(post("/bolsas").header("X-Perfil", "GESTOR_HEMOCENTRO")
                .contentType(MediaType.APPLICATION_JSON).content(corpo(" duplicada ", "PLAQUETAS", LocalDate.now())))
                .andExpect(status().isConflict());
    }

    @Test
    void camposObrigatoriosEColetaFuturaSaoRejeitados() throws Exception {
        mvc.perform(post("/bolsas").header("X-Perfil", "GESTOR_HEMOCENTRO")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("tipoSanguineo")));
        mvc.perform(post("/bolsas").header("X-Perfil", "GESTOR_HEMOCENTRO")
                .contentType(MediaType.APPLICATION_JSON).content(corpo("FUTURA", "PLASMA", LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void alertaDoisDiasNaoDuplicaEDescarteMantemHistorico() throws Exception {
        BolsaHemocomponente bolsa = cadastrar("VENCE-DOIS", LocalDate.now().minusDays(3));
        validade.verificar(LocalDate.now());
        validade.verificar(LocalDate.now());
        assertTrue(alertas.existsByBolsaIdAndLimiarDias(bolsa.getId(), 2));
        assertEquals(2, alertas.findAll().stream().filter(a -> a.getBolsa().getId().equals(bolsa.getId())).count());
        validade.verificar(LocalDate.now().plusDays(2));
        BolsaHemocomponente descartada = bolsas.findById(bolsa.getId()).orElseThrow();
        assertEquals(StatusBolsa.DESCARTADO, descartada.getStatus());
        assertNotNull(descartada.getDataDescarte());
        assertEquals("SISTEMA", descartada.getResponsavelDescarte());
        var momento = descartada.getDataDescarte();
        validade.verificar(LocalDate.now().plusDays(3));
        assertEquals(momento, descartada.getDataDescarte());
        mvc.perform(get("/bolsas?status=DISPONIVEL").header("X-Perfil", "GESTOR_HEMOCENTRO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.codigo == 'VENCE-DOIS')]").isEmpty());
    }

    @Test
    @Transactional
    void vencidaNoCadastroDescartaEImpedeEdicao() throws Exception {
        BolsaHemocomponente bolsa = cadastrar("JA-VENCIDA", LocalDate.now().minusDays(5));
        assertEquals(StatusBolsa.DESCARTADO, bolsa.getStatus());
        mvc.perform(put("/bolsas/" + bolsa.getId()).header("X-Perfil", "GESTOR_HEMOCENTRO")
                .contentType(MediaType.APPLICATION_JSON).content(corpo("JA-VENCIDA", "PLAQUETAS", LocalDate.now())))
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    void horarioDoDescarteNaoMudaAoRecarregarDoH2() throws Exception {
        BolsaHemocomponente bolsa = cadastrar("PRECISAO-H2", LocalDate.now().minusDays(6));
        var momento = bolsa.getDataDescarte();
        Long id = bolsa.getId();
        entityManager.flush();
        entityManager.clear();
        assertEquals(momento, bolsas.findById(id).orElseThrow().getDataDescarte());
    }

    @Test
    @Transactional
    void editarRecalculaValidadeERemoveAlertasAntigos() throws Exception {
        BolsaHemocomponente bolsa = cadastrar("EDITAVEL", LocalDate.now().minusDays(3));
        mvc.perform(put("/bolsas/" + bolsa.getId()).header("X-Perfil", "GESTOR_HEMOCENTRO")
                .contentType(MediaType.APPLICATION_JSON).content(corpo("EDITADA", "PLASMA", LocalDate.now())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.validade").value(LocalDate.now().plusDays(365).toString()));
        assertFalse(alertas.existsByBolsaIdAndLimiarDias(bolsa.getId(), 2));
    }

    @Test
    void alertasPodemSerConsultadosSemTransacaoDoTeste() throws Exception {
        // Reproduz a chamada HTTP real, sem @Transactional escondendo falhas do service.
        mvc.perform(get("/bolsas/alertas").header("X-Perfil", "GESTOR_HEMOCENTRO"))
                .andExpect(status().isOk());
    }

    @Test
    void fluxoHttpCompletoSemTransacaoExterna() throws Exception {
        String codigo = "HTTP-" + System.nanoTime();
        BolsaHemocomponente bolsa = cadastrar(codigo, LocalDate.now().minusDays(3));
        mvc.perform(get("/bolsas/alertas").header("X-Perfil", "GESTOR_HEMOCENTRO"))
                .andExpect(status().isOk());
        mvc.perform(put("/bolsas/" + bolsa.getId()).header("X-Perfil", "GESTOR_HEMOCENTRO")
                .contentType(MediaType.APPLICATION_JSON).content(corpo(codigo, "PLAQUETAS", LocalDate.now().minusDays(2))))
                .andExpect(status().isOk());
        mvc.perform(get("/bolsas/" + bolsa.getId()).header("X-Perfil", "GESTOR_HEMOCENTRO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value(codigo));
        assertTrue(alertas.existsByBolsaIdAndLimiarDias(bolsa.getId(), 5));
        assertFalse(alertas.existsByBolsaIdAndLimiarDias(bolsa.getId(), 2));
    }

    @Test
    @Transactional
    void camposControladosPeloServidorNaoPodemSerForjados() throws Exception {
        String dados = corpo("SERVIDOR", "PLAQUETAS", LocalDate.now());
        dados = dados.substring(0, dados.length() - 1)
                + ",\"id\":999,\"validade\":\"2099-01-01\",\"status\":\"DESCARTADO\",\"motivoDescarte\":\"forjado\"}";
        mvc.perform(post("/bolsas").header("X-Perfil", "GESTOR_HEMOCENTRO")
                .contentType(MediaType.APPLICATION_JSON).content(dados))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DISPONIVEL"))
                .andExpect(jsonPath("$.validade").value(LocalDate.now().plusDays(5).toString()))
                .andExpect(jsonPath("$.motivoDescarte").isEmpty());
    }

    @Test
    void perfisSemAcessoEIdentificadorInexistente() throws Exception {
        mvc.perform(get("/bolsas")).andExpect(status().isForbidden());
        mvc.perform(get("/bolsas").header("X-Perfil", "HOSPITAL_SOLICITANTE")).andExpect(status().isForbidden());
        mvc.perform(get("/bolsas/alertas").header("X-Perfil", "OPERADOR_LOGISTICA")).andExpect(status().isForbidden());
        mvc.perform(get("/bolsas/999999").header("X-Perfil", "GESTOR_HEMOCENTRO")).andExpect(status().isNotFound());
    }
}
