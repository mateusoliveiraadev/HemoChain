package com.hemochain.repository;

import static com.hemochain.entity.TipoSanguineo.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import com.hemochain.entity.*;

// Teste de INTEGRACAO com o banco: a consulta de sugestao (FEFO) roda de verdade em SQL.
@DataJpaTest
class BolsaCompativelRepositoryTest {
    @Autowired private BolsaRepository bolsas;
    private final LocalDate hoje = LocalDate.now();

    private BolsaHemocomponente bolsa(String codigo, TipoComponente componente, TipoSanguineo tipo, int diasParaVencer) {
        BolsaHemocomponente b = new BolsaHemocomponente();
        b.setCodigo(codigo);
        b.setTipoComponente(componente);
        b.setTipoSanguineo(tipo);
        b.setVolumeMl(450);
        b.setDataColeta(hoje.minusDays(1));
        b.setValidade(hoje.plusDays(diasParaVencer));
        b.setStatus(StatusBolsa.DISPONIVEL);
        return bolsas.save(b);
    }

    @Test
    void retornaSomenteHemaciasDisponiveisCompativeisOrdenadasPorValidade() {
        bolsa("A-NEG-10D", TipoComponente.CONCENTRADO_HEMACIAS, A_NEGATIVO, 10);
        bolsa("O-NEG-3D", TipoComponente.CONCENTRADO_HEMACIAS, O_NEGATIVO, 3);
        bolsa("B-POS-1D", TipoComponente.CONCENTRADO_HEMACIAS, B_POSITIVO, 1);
        bolsa("O-NEG-PLAQ", TipoComponente.PLAQUETAS, O_NEGATIVO, 2);
        BolsaHemocomponente descartada = bolsa("O-NEG-DESC", TipoComponente.CONCENTRADO_HEMACIAS, O_NEGATIVO, 1);
        descartada.descartar(LocalDateTime.now());
        bolsas.saveAndFlush(descartada);

        List<BolsaHemocomponente> sugeridas = bolsas
                .findByStatusAndTipoComponenteAndTipoSanguineoInOrderByValidadeAscIdAsc(
                        StatusBolsa.DISPONIVEL, TipoComponente.CONCENTRADO_HEMACIAS,
                        List.of(O_NEGATIVO, A_NEGATIVO));

        assertEquals(List.of("O-NEG-3D", "A-NEG-10D"),
                sugeridas.stream().map(BolsaHemocomponente::getCodigo).toList());
    }
}
