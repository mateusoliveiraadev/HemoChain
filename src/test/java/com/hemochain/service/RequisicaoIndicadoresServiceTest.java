package com.hemochain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.hemochain.dto.EstatisticaTipoSanguineo;
import com.hemochain.dto.RequisicaoProjecao;
import com.hemochain.entity.TipoSanguineo;
import com.hemochain.repository.RequisicaoRepository;

/**
 * Testa a corretude do calculo em si (sequencial x paralelo), sem subir o
 * contexto do Spring nem depender do banco - RequisicaoIndicadoresService
 * so precisa do repository para o metodo publico {@code calcular}; os
 * metodos {@code calcularSequencial}/{@code calcularParalelo} usados aqui
 * operam direto sobre a lista ja carregada.
 */
class RequisicaoIndicadoresServiceTest {

    private final RequisicaoIndicadoresService service = new RequisicaoIndicadoresService(
            (RequisicaoRepository) null);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 7, 8, 16})
    void sequencialEParaleloProduzemExatamenteAMesmaEstatistica(int threads) {
        List<RequisicaoProjecao> dados = gerarDadosAleatorios(50_000, 42L);

        List<EstatisticaTipoSanguineo> esperado = service.calcularSequencial(dados).finalizar();
        List<EstatisticaTipoSanguineo> obtido = service.calcularParalelo(dados, threads).finalizar();

        assertEquals(esperado.size(), obtido.size());
        for (int i = 0; i < esperado.size(); i++) {
            EstatisticaTipoSanguineo e = esperado.get(i);
            EstatisticaTipoSanguineo o = obtido.get(i);
            assertEquals(e.tipoSanguineo(), o.tipoSanguineo());
            assertEquals(e.totalRequisicoes(), o.totalRequisicoes());
            assertEquals(e.somaQuantidade(), o.somaQuantidade());
            assertEquals(e.media(), o.media(), 1e-9);
            assertEquals(e.variancia(), o.variancia(), 1e-6);
            assertEquals(e.desvioPadrao(), o.desvioPadrao(), 1e-6);
        }
    }

    @Test
    void totalDeRequisicoesBateComOTamanhoDaEntrada() {
        List<RequisicaoProjecao> dados = gerarDadosAleatorios(10_000, 7L);

        long totalSequencial = service.calcularSequencial(dados).finalizar().stream()
                .mapToLong(EstatisticaTipoSanguineo::totalRequisicoes).sum();
        long totalParalelo = service.calcularParalelo(dados, 4).finalizar().stream()
                .mapToLong(EstatisticaTipoSanguineo::totalRequisicoes).sum();

        assertEquals(dados.size(), totalSequencial);
        assertEquals(dados.size(), totalParalelo);
    }

    @Test
    void listaVaziaNaoQuebraENaoGeraDemanda() {
        List<EstatisticaTipoSanguineo> resultado = service.calcularParalelo(List.of(), 4).finalizar();
        assertTrue(resultado.stream().allMatch(e -> e.totalRequisicoes() == 0));
    }

    private List<RequisicaoProjecao> gerarDadosAleatorios(int quantidade, long seed) {
        Random random = new Random(seed);
        TipoSanguineo[] tipos = TipoSanguineo.values();
        List<RequisicaoProjecao> dados = new ArrayList<>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            TipoSanguineo tipo = tipos[random.nextInt(tipos.length)];
            int qtd = 1 + random.nextInt(10);
            dados.add(new RequisicaoProjecao(tipo, qtd));
        }
        return dados;
    }
}
