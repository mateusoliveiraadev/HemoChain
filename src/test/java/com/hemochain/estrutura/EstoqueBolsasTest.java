package com.hemochain.estrutura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.hemochain.entity.TipoComponente;
import com.hemochain.entity.TipoSanguineo;

class EstoqueBolsasTest {

    @Test
    void comecaVazio() {
        EstoqueBolsas estoque = new EstoqueBolsas();

        assertEquals(0, estoque.getTotalBolsas());
        assertNull(estoque.buscarBolsa(1));
    }

    @Test
    void inserirEBuscarBolsa() {
        EstoqueBolsas estoque = new EstoqueBolsas();
        estoque.inserirBolsa(new Bolsa(1, TipoSanguineo.O_NEGATIVO, TipoComponente.CONCENTRADO_HEMACIAS, 450, "2026-09-01"));
        estoque.inserirBolsa(new Bolsa(2, TipoSanguineo.A_POSITIVO, TipoComponente.PLASMA, 300, "2026-09-05"));

        assertEquals(2, estoque.getTotalBolsas());

        Bolsa encontrada = estoque.buscarBolsa(2);
        assertEquals(TipoSanguineo.A_POSITIVO, encontrada.getTipoSanguineo());
        assertEquals(300, encontrada.getQuantidadeMl());
    }

    @Test
    void removerBolsaExistenteEInexistente() {
        EstoqueBolsas estoque = new EstoqueBolsas();
        estoque.inserirBolsa(new Bolsa(1, TipoSanguineo.O_NEGATIVO, TipoComponente.CONCENTRADO_HEMACIAS, 450, "2026-09-01"));

        assertTrue(estoque.removerBolsa(1));
        assertEquals(0, estoque.getTotalBolsas());
        assertNull(estoque.buscarBolsa(1));

        assertFalse(estoque.removerBolsa(999)); // id que nunca existiu
    }

    @Test
    void listarRetornaTodasAsBolsasInseridas() {
        EstoqueBolsas estoque = new EstoqueBolsas();
        estoque.inserirBolsa(new Bolsa(1, TipoSanguineo.O_NEGATIVO, TipoComponente.CONCENTRADO_HEMACIAS, 450, "2026-09-01"));
        estoque.inserirBolsa(new Bolsa(2, TipoSanguineo.A_POSITIVO, TipoComponente.PLASMA, 300, "2026-09-05"));

        assertEquals(2, estoque.listar().size());
    }
}
