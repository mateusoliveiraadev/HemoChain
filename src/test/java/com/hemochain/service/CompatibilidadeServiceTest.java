package com.hemochain.service;

import static com.hemochain.entity.TipoSanguineo.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.hemochain.entity.TipoSanguineo;

// Regra didatica de compatibilidade ABO/Rh para concentrado de hemacias.
// Nao e protocolo clinico oficial. Tarefa T1 da US03 (ver README, story map).
class CompatibilidadeServiceTest {
    private final CompatibilidadeService service = new CompatibilidadeService();

    @Test
    void oNegativoDoaParaTodosOsTipos() {
        for (TipoSanguineo receptor : values()) {
            assertTrue(service.podeDoarHemacias(O_NEGATIVO, receptor), "O- deve doar para " + receptor);
        }
    }

    @Test
    void abPositivoRecebeDeTodosOsTipos() {
        for (TipoSanguineo doador : values()) {
            assertTrue(service.podeDoarHemacias(doador, AB_POSITIVO), doador + " deve doar para AB+");
        }
    }

    @Test
    void doadorRhPositivoNaoDoaParaReceptorRhNegativo() {
        assertFalse(service.podeDoarHemacias(A_POSITIVO, A_NEGATIVO));
        assertFalse(service.podeDoarHemacias(O_POSITIVO, O_NEGATIVO));
        assertFalse(service.podeDoarHemacias(AB_POSITIVO, AB_NEGATIVO));
        assertTrue(service.podeDoarHemacias(A_NEGATIVO, A_POSITIVO));
    }

    @Test
    void grupoAboIncompativelEBloqueado() {
        assertFalse(service.podeDoarHemacias(A_NEGATIVO, B_NEGATIVO));
        assertFalse(service.podeDoarHemacias(B_NEGATIVO, A_NEGATIVO));
        assertFalse(service.podeDoarHemacias(A_NEGATIVO, O_NEGATIVO));
        assertFalse(service.podeDoarHemacias(AB_NEGATIVO, A_NEGATIVO));
        assertFalse(service.podeDoarHemacias(AB_NEGATIVO, O_NEGATIVO));
    }

    @Test
    void tabelaCompletaDeDoadoresPorReceptor() {
        Map<TipoSanguineo, Set<TipoSanguineo>> esperado = Map.of(
                O_NEGATIVO, Set.of(O_NEGATIVO),
                O_POSITIVO, Set.of(O_NEGATIVO, O_POSITIVO),
                A_NEGATIVO, Set.of(O_NEGATIVO, A_NEGATIVO),
                A_POSITIVO, Set.of(O_NEGATIVO, O_POSITIVO, A_NEGATIVO, A_POSITIVO),
                B_NEGATIVO, Set.of(O_NEGATIVO, B_NEGATIVO),
                B_POSITIVO, Set.of(O_NEGATIVO, O_POSITIVO, B_NEGATIVO, B_POSITIVO),
                AB_NEGATIVO, Set.of(O_NEGATIVO, A_NEGATIVO, B_NEGATIVO, AB_NEGATIVO),
                AB_POSITIVO, Set.of(values()));
        for (TipoSanguineo receptor : values()) {
            assertEquals(esperado.get(receptor), new HashSet<>(service.doadoresCompativeis(receptor)),
                    "doadores de " + receptor);
        }
    }

    @Test
    void receptorNuloEhRejeitado() {
        assertThrows(IllegalArgumentException.class, () -> service.doadoresCompativeis(null));
    }
}
