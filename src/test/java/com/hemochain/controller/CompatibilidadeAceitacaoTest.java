package com.hemochain.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// Teste de ACEITACAO (BDD). Cenarios em Gherkin: docs/bdd/compatibilidade.feature
@SpringBootTest
@AutoConfigureMockMvc
class CompatibilidadeAceitacaoTest {
    private static final String GESTOR = "GESTOR_HEMOCENTRO";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private void cadastrarHemacias(String codigo, String tipo, LocalDate coleta) throws Exception {
        String corpo = json.writeValueAsString(Map.of("codigo", codigo,
                "tipoComponente", "CONCENTRADO_HEMACIAS", "tipoSanguineo", tipo,
                "volumeMl", 300, "dataColeta", coleta.toString()));
        mvc.perform(post("/bolsas").header("X-Perfil", GESTOR)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
    }

    // Considera apenas as bolsas criadas por este teste (prefixo ACEITE-).
    private List<String> sugeridas(String receptor) throws Exception {
        String resposta = mvc.perform(get("/bolsas/compativeis").param("receptor", receptor)
                .header("X-Perfil", GESTOR))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> codigos = new ArrayList<>();
        for (JsonNode bolsa : json.readTree(resposta)) {
            String codigo = bolsa.get("codigo").asText();
            if (codigo.startsWith("ACEITE-")) {
                codigos.add(codigo);
            }
        }
        return codigos;
    }

    @Test
    @Transactional
    void cenario1_sugereSomenteBolsasCompativeisComAMaisProximaDoVencimentoPrimeiro() throws Exception {
        // Dado hemacias O- (vence em 5 dias), A- (vence em 35 dias), A+ e B-
        cadastrarHemacias("ACEITE-A-NEG", "A_NEGATIVO", LocalDate.now());
        cadastrarHemacias("ACEITE-O-NEG", "O_NEGATIVO", LocalDate.now().minusDays(30));
        cadastrarHemacias("ACEITE-A-POS", "A_POSITIVO", LocalDate.now());
        cadastrarHemacias("ACEITE-B-NEG", "B_NEGATIVO", LocalDate.now());
        // Quando o gestor consulta bolsas compativeis para um receptor A-
        // Entao ve O- primeiro (FEFO) e depois A-; nao ve A+ nem B-
        assertEquals(List.of("ACEITE-O-NEG", "ACEITE-A-NEG"), sugeridas("A_NEGATIVO"));
    }

    @Test
    @Transactional
    void cenario2_bolsaVencidaNaoESugerida() throws Exception {
        // Dado uma bolsa O- coletada ha 40 dias (validade didatica de 35 dias: vencida) e uma O- valida
        cadastrarHemacias("ACEITE-VENCIDA", "O_NEGATIVO", LocalDate.now().minusDays(40));
        cadastrarHemacias("ACEITE-VALIDA", "O_NEGATIVO", LocalDate.now());
        // Quando o gestor consulta para um receptor O+
        // Entao somente a bolsa valida aparece
        assertEquals(List.of("ACEITE-VALIDA"), sugeridas("O_POSITIVO"));
    }

    @Test
    void cenario3_somenteOGestorConsultaCompatibilidade() throws Exception {
        mvc.perform(get("/bolsas/compativeis").param("receptor", "A_NEGATIVO"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/bolsas/compativeis").param("receptor", "A_NEGATIVO")
                .header("X-Perfil", "HOSPITAL_SOLICITANTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cenario4_receptorInvalidoOuAusenteRetornaErroDeCliente() throws Exception {
        mvc.perform(get("/bolsas/compativeis").param("receptor", "XPTO")
                .header("X-Perfil", GESTOR)).andExpect(status().isBadRequest());
        mvc.perform(get("/bolsas/compativeis").header("X-Perfil", GESTOR))
                .andExpect(status().isBadRequest());
    }
}
