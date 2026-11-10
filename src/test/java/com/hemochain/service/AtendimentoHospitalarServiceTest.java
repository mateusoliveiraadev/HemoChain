package com.hemochain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import com.hemochain.entity.*;
import com.hemochain.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class AtendimentoHospitalarServiceTest {
    private final RequisicaoRepository requisicoes = mock(RequisicaoRepository.class);
    private final BolsaRepository bolsas = mock(BolsaRepository.class);
    private final EntregaHospitalarRepository entregas = mock(EntregaHospitalarRepository.class);
    private final RastreabilidadeService historico = mock(RastreabilidadeService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);
    private AtendimentoHospitalarService service(Duration prazo) {
        return new AtendimentoHospitalarService(requisicoes, bolsas, entregas,
                new CompatibilidadeService(), historico, clock, prazo);
    }
    @Test void prazoZeroOuNegativoImpedeInicializacao() {
        assertThrows(IllegalArgumentException.class, () -> service(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> service(Duration.ofSeconds(-1)));
    }
    @Test void prazoConfiguradoECalculadoNoServidorEAssociacaoEExplicita() {
        var r = new RequisicaoHospitalar(1L, TipoComponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.A_NEGATIVO, 1, UrgenciaRequisicao.ROTINA, LocalDate.now());
        var h = new Hospital("Sintetico", "S", "S"); ReflectionTestUtils.setField(h, "id", 1L);r.setHospital(h);
        ReflectionTestUtils.setField(r, "id", 2L);
        var b = new BolsaHemocomponente();ReflectionTestUtils.setField(b, "id", 3L);
        b.setTipoComponente(TipoComponente.CONCENTRADO_HEMACIAS);b.setTipoSanguineo(TipoSanguineo.O_NEGATIVO);
        b.setStatus(StatusBolsa.DISPONIVEL);b.setValidade(LocalDate.of(2026,11,12));
        when(requisicoes.buscarParaAtualizacao(2L)).thenReturn(Optional.of(r));
        when(bolsas.buscarParaAtualizacao(3L)).thenReturn(Optional.of(b));
        when(entregas.save(any())).thenAnswer(i -> i.getArgument(0));
        var e = service(Duration.ofMinutes(90)).registrarEntrega(2L,3L);
        assertEquals(clock.instant().plusSeconds(5400), e.getPrazoConfirmacao());
        assertEquals(StatusBolsa.ENTREGUE, b.getStatus());
        verify(historico).registrar(eq(b), eq(TipoEventoBolsa.ENTREGA_REGISTRADA), anyString(), anyString());
    }
    @Test void hospitalSemCorrespondenciaERecusado() {
        var h = new Hospital("Sintetico", "S", "S");ReflectionTestUtils.setField(h,"id",1L);
        var r = new RequisicaoHospitalar();r.setHospital(h);
        assertThrows(ResponseStatusException.class, () -> AtendimentoHospitalarService.exigirHospital(r,2L));
        assertThrows(ResponseStatusException.class, () -> AtendimentoHospitalarService.exigirHospital(r,null));
    }
}
