package com.hemochain.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.hemochain.entity.Hospital;
import com.hemochain.repository.HospitalRepository;
import com.hemochain.service.HospitalService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AcessoPorPerfilTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Test
    void operadorLogisticaNaoAcessaHospitais() throws Exception {
        mockMvc.perform(get("/hospitais").header("X-Perfil", "OPERADOR_LOGISTICA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void semPerfilInformadoNaoAcessaRequisicoes() throws Exception {
        mockMvc.perform(get("/requisicoes"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hospitalSolicitanteAcessaRequisicoes() throws Exception {
        mockMvc.perform(get("/requisicoes").header("X-Perfil", "HOSPITAL_SOLICITANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void gestorPodeVisualizarMasNaoCriarRequisicao() throws Exception {
        Hospital hospital = hospitalService.cadastrar(new Hospital("Hospital Teste", "1", "1"));

        mockMvc.perform(get("/requisicoes").header("X-Perfil", "GESTOR_HEMOCENTRO"))
                .andExpect(status().isOk());

        // Corpo valido de proposito: queremos provar que e o perfil (nao a validacao dos campos)
        // quem barra a escrita para o Gestor de Hemocentro.
        String corpoValido = "{\"hospitalId\": " + hospital.getId() + ", \"tipoComponente\": \"PLASMA\","
                + " \"tipoSanguineo\": \"A_POSITIVO\", \"quantidade\": 1, \"urgencia\": \"ROTINA\","
                + " \"prazoLimite\": \"2030-01-01\"}";

        mockMvc.perform(post("/requisicoes")
                        .header("X-Perfil", "GESTOR_HEMOCENTRO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido))
                .andExpect(status().isForbidden());
    }

    @Test
    void enviarIdManualNoCadastroDeHospitalNaoSobrescreveExistente() throws Exception {
        Hospital original = hospitalService.cadastrar(new Hospital("Hospital Original", "111", "111"));

        mockMvc.perform(post("/hospitais")
                        .header("X-Perfil", "HOSPITAL_SOLICITANTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": " + original.getId()
                                + ", \"nome\": \"Hospital Falsificado\", \"cnpj\": \"x\", \"telefone\": \"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Hospital Falsificado"));

        Hospital aindaOriginal = hospitalRepository.findById(original.getId()).orElseThrow();
        assertEquals("Hospital Original", aindaOriginal.getNome());
        assertTrue(hospitalRepository.count() >= 2);
    }
}
