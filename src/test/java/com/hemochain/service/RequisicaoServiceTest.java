package com.hemochain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hemochain.entity.Hospital;
import com.hemochain.entity.RequisicaoHospitalar;
import com.hemochain.entity.TipoComponente;
import com.hemochain.entity.TipoSanguineo;
import com.hemochain.entity.UrgenciaRequisicao;

@SpringBootTest
@Transactional
class RequisicaoServiceTest {

    @Autowired
    private RequisicaoService requisicaoService;

    @Autowired
    private HospitalService hospitalService;

    @Test
    void cadastrarRequisicaoVinculadaAoHospitalComSucesso() {
        Hospital hospital = hospitalService.cadastrar(new Hospital("Hospital Central", "111", "111"));

        RequisicaoHospitalar requisicao = new RequisicaoHospitalar(
                hospital.getId(), TipoComponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_NEGATIVO,
                2, UrgenciaRequisicao.EMERGENCIAL, LocalDate.now().plusDays(1));

        RequisicaoHospitalar salva = requisicaoService.cadastrar(requisicao);

        assertNotNull(salva.getId());
        assertEquals(hospital.getId(), salva.getHospital().getId());
        assertEquals("Hospital Central", salva.getHospital().getNome());
    }

    @Test
    void cadastrarComHospitalInexistenteDeveFalhar() {
        RequisicaoHospitalar requisicao = new RequisicaoHospitalar(
                9999L, TipoComponente.PLASMA, TipoSanguineo.A_POSITIVO,
                1, UrgenciaRequisicao.ROTINA, LocalDate.now().plusDays(3));

        assertThrows(ResponseStatusException.class, () -> requisicaoService.cadastrar(requisicao));
    }

    @Test
    void cadastrarSemHospitalIdDeveFalhar() {
        RequisicaoHospitalar requisicao = new RequisicaoHospitalar(
                null, TipoComponente.PLASMA, TipoSanguineo.A_POSITIVO,
                1, UrgenciaRequisicao.ROTINA, LocalDate.now().plusDays(3));

        assertThrows(ResponseStatusException.class, () -> requisicaoService.cadastrar(requisicao));
    }

    @Test
    void listarTodasRetornaRequisicoesCadastradas() {
        Hospital hospital = hospitalService.cadastrar(new Hospital("Hospital Sul", "222", "222"));
        requisicaoService.cadastrar(new RequisicaoHospitalar(
                hospital.getId(), TipoComponente.PLAQUETAS, TipoSanguineo.B_POSITIVO,
                3, UrgenciaRequisicao.URGENTE, LocalDate.now().plusDays(2)));

        assertTrue(requisicaoService.listarTodas().size() >= 1);
    }
}
