package com.hemochain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hemochain.dto.IndicadoresDemandaResponse;
import com.hemochain.dto.RequisicaoProjecao;
import com.hemochain.repository.RequisicaoRepository;

/**
 * Servico de indicadores de demanda por tipo sanguineo (US06 - Painel de
 * indicadores de estoque e demanda, recorte de demanda historica).
 *
 * Contem duas implementacoes do mesmo calculo - sequencial e com threads
 * de plataforma via {@link ExecutorService} - para permitir a comparacao
 * de desempenho pedida no trabalho. As duas leem a mesma lista de dados
 * (ja carregada em memoria) e devem produzir exatamente o mesmo
 * resultado numerico, ja que soma e contagem sao operacoes associativas
 * e cada requisicao e processada de forma independente das demais.
 */
@Service
public class RequisicaoIndicadoresService {

    /** Acima disso, o custo de criar/coordenar threads deixa de compensar para este volume tipico de teste. */
    private static final int THREADS_MAXIMAS = 64;

    private final RequisicaoRepository repository;

    public RequisicaoIndicadoresService(RequisicaoRepository repository) {
        this.repository = repository;
    }

    /**
     * Carrega o historico (fase de I/O, fora da medicao) e calcula os
     * indicadores de demanda por tipo sanguineo (fase de CPU, e essa que
     * e cronometrada).
     *
     * @param threads numero de threads a usar; 1 executa a versao sequencial.
     * @param desde   filtro opcional de periodo (US06 - "consulta de historico por periodo"); nulo = todo o historico.
     */
    public IndicadoresDemandaResponse calcular(int threads, LocalDateTime desde) {
        if (threads < 1 || threads > THREADS_MAXIMAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "threads deve estar entre 1 e " + THREADS_MAXIMAS + ".");
        }

        List<RequisicaoProjecao> dados = repository.buscarProjecaoParaIndicadores(desde);

        long inicioNanos = System.nanoTime();
        DemandaAccumulator resultado = threads == 1
                ? calcularSequencial(dados)
                : calcularParalelo(dados, threads);
        long duracaoNanos = System.nanoTime() - inicioNanos;

        return new IndicadoresDemandaResponse(
                resultado.finalizar(),
                dados.size(),
                threads,
                duracaoNanos,
                duracaoNanos / 1_000_000.0,
                desde);
    }

    /** Versao sequencial: uma unica passagem, um unico acumulador, em uma thread. */
    DemandaAccumulator calcularSequencial(List<RequisicaoProjecao> dados) {
        DemandaAccumulator acumulador = new DemandaAccumulator();
        for (RequisicaoProjecao requisicao : dados) {
            acumulador.acumular(requisicao.tipoSanguineo(), requisicao.quantidade());
        }
        return acumulador;
    }

    /**
     * Versao paralela: particiona {@code dados} em ate {@code threads} fatias
     * contiguas de tamanho aproximadamente N/threads, processa cada fatia em
     * uma thread com seu proprio acumulador local (sem lock, sem escrita
     * compartilhada) e reduz os acumuladores parciais ao final (merge).
     */
    DemandaAccumulator calcularParalelo(List<RequisicaoProjecao> dados, int threads) {
        int n = dados.size();
        if (n == 0) {
            return new DemandaAccumulator();
        }

        int efetivas = Math.min(threads, n);
        int tamanhoFatia = (int) Math.ceil(n / (double) efetivas);

        List<Callable<DemandaAccumulator>> tarefas = new ArrayList<>(efetivas);
        for (int inicioFatia = 0; inicioFatia < n; inicioFatia += tamanhoFatia) {
            int fimFatia = Math.min(inicioFatia + tamanhoFatia, n);
            int de = inicioFatia;
            int ate = fimFatia;
            tarefas.add(() -> {
                DemandaAccumulator parcial = new DemandaAccumulator();
                for (int i = de; i < ate; i++) {
                    RequisicaoProjecao requisicao = dados.get(i);
                    parcial.acumular(requisicao.tipoSanguineo(), requisicao.quantidade());
                }
                return parcial;
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(efetivas);
        try {
            List<Future<DemandaAccumulator>> futuros = executor.invokeAll(tarefas);
            DemandaAccumulator total = new DemandaAccumulator();
            for (Future<DemandaAccumulator> futuro : futuros) {
                total.mesclar(futuro.get());
            }
            return total;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento paralelo interrompido.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Falha ao processar fatia em paralelo.", e.getCause());
        } finally {
            executor.shutdown();
        }
    }
}
