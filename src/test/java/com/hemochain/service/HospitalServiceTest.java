package com.hemochain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hemochain.entity.Hospital;

@SpringBootTest
@Transactional
class HospitalServiceTest {

    @Autowired
    private HospitalService hospitalService;

    @Test
    void cadastrarHospitalComSucesso() {
        Hospital salvo = hospitalService.cadastrar(new Hospital("Hospital Central", "12.345.678/0001-00", "8130001111"));

        assertNotNull(salvo.getId());
        assertEquals("Hospital Central", salvo.getNome());
    }

    @Test
    void listarTodosRetornaHospitaisCadastrados() {
        hospitalService.cadastrar(new Hospital("Hospital A", "111", "111"));
        hospitalService.cadastrar(new Hospital("Hospital B", "222", "222"));

        assertTrue(hospitalService.listarTodos().size() >= 2);
    }
}
