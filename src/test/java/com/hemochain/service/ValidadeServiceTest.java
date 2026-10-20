package com.hemochain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.hemochain.entity.*;
import com.hemochain.repository.*;

class ValidadeServiceTest {
    private BolsaRepository bolsas;
    private AlertaValidadeRepository alertas;
    private ValidadeService service;
    private final LocalDate hoje = LocalDate.of(2026, 10, 20);

    @BeforeEach
    void preparar() {
        bolsas = mock(BolsaRepository.class);
        alertas = mock(AlertaValidadeRepository.class);
        service = new ValidadeService(bolsas, alertas);
        ReflectionTestUtils.setField(service, "diasHemacias", 35);
        ReflectionTestUtils.setField(service, "diasPlasma", 365);
        ReflectionTestUtils.setField(service, "diasPlaquetas", 5);
        ReflectionTestUtils.setField(service, "diasCrioprecipitado", 365);
        ReflectionTestUtils.setField(service, "limiares", new int[] {5, 2});
    }

    private BolsaHemocomponente bolsa(int dias) {
        BolsaHemocomponente b = new BolsaHemocomponente();
        ReflectionTestUtils.setField(b, "id", 1L);
        b.setValidade(hoje.plusDays(dias));
        b.setStatus(StatusBolsa.DISPONIVEL);
        return b;
    }

    @Test
    void calculaTabelaDidaticaConfiguravel() {
        assertEquals(hoje.plusDays(35), service.calcular(TipoComponente.CONCENTRADO_HEMACIAS, hoje));
        assertEquals(hoje.plusDays(365), service.calcular(TipoComponente.PLASMA, hoje));
        assertEquals(hoje.plusDays(5), service.calcular(TipoComponente.PLAQUETAS, hoje));
        assertEquals(hoje.plusDays(365), service.calcular(TipoComponente.CRIOPRECIPITADO, hoje));
        ReflectionTestUtils.setField(service, "diasPlaquetas", 7);
        assertEquals(hoje.plusDays(7), service.calcular(TipoComponente.PLAQUETAS, hoje));
    }

    @Test
    void seisDiasSemAlertaECincoDiasComAlerta() {
        service.verificarBolsa(bolsa(6), hoje);
        verifyNoInteractions(alertas);
        service.verificarBolsa(bolsa(5), hoje);
        verify(alertas, times(1)).save(any(AlertaValidade.class));
    }

    @Test
    void diaDoVencimentoEDiaAnteriorAoAtualDescartamSemAlerta() {
        BolsaHemocomponente b = bolsa(0);
        BolsaHemocomponente vencida = bolsa(-1);
        when(bolsas.buscarParaVerificacao(StatusBolsa.DISPONIVEL)).thenReturn(List.of(b, vencida));
        service.verificar(hoje);
        assertEquals(StatusBolsa.DESCARTADO, b.getStatus());
        assertEquals(StatusBolsa.DESCARTADO, vencida.getStatus());
        verifyNoInteractions(alertas);
    }

    @Test
    void configuracaoInvalidaFalhaExplicitamente() {
        ReflectionTestUtils.setField(service, "diasPlasma", 0);
        assertThrows(IllegalArgumentException.class, service::validarConfiguracao);
    }
}
