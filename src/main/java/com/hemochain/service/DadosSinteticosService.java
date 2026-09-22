package com.hemochain.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hemochain.dto.ResultadoGeracaoDados;
import com.hemochain.entity.Hospital;
import com.hemochain.entity.TipoComponente;
import com.hemochain.entity.TipoSanguineo;
import com.hemochain.entity.UrgenciaRequisicao;
import com.hemochain.repository.HospitalRepository;

/**
 * Gera massa sintetica de {@code RequisicaoHospitalar} para simular a
 * escala nacional pedida no trabalho (ex.: 100 mil e 1 milhao de
 * registros). Usa insert em lote via {@link JdbcTemplate}, ignorando o
 * ciclo de persistencia do Hibernate (que fica caro demais em 1 milhao
 * de saves individuais) - esta e uma rotina de preparacao de dados, nao
 * a operacao que o trabalho mede, entao seu proprio desempenho nao entra
 * na comparacao sequencial vs. threads.
 */
@Service
public class DadosSinteticosService {

    private static final int TAMANHO_LOTE = 2_000;
    private static final String SQL_INSERT = "insert into requisicao_hospitalar "
            + "(hospital_id, tipo_componente, tipo_sanguineo, quantidade, urgencia, prazo_limite, data_criacao) "
            + "values (?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final HospitalRepository hospitalRepository;

    public DadosSinteticosService(JdbcTemplate jdbcTemplate, HospitalRepository hospitalRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.hospitalRepository = hospitalRepository;
    }

    /**
     * @param quantidade      numero de requisicoes sinteticas a gerar (somadas as ja existentes).
     * @param hospitaisMinimo numero minimo de hospitais a manter cadastrados, para distribuir as requisicoes.
     */
    @Transactional
    public ResultadoGeracaoDados gerar(long quantidade, int hospitaisMinimo) {
        List<Long> hospitalIds = garantirHospitais(hospitaisMinimo);
        TipoComponente[] componentes = TipoComponente.values();
        TipoSanguineo[] tipos = TipoSanguineo.values();
        UrgenciaRequisicao[] urgencias = UrgenciaRequisicao.values();

        long inicio = System.nanoTime();
        long restante = quantidade;
        while (restante > 0) {
            int tamanhoDoLote = (int) Math.min(TAMANHO_LOTE, restante);
            List<Object[]> parametros = new ArrayList<>(tamanhoDoLote);
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (int i = 0; i < tamanhoDoLote; i++) {
                long hospitalId = hospitalIds.get(random.nextInt(hospitalIds.size()));
                TipoComponente componente = componentes[random.nextInt(componentes.length)];
                TipoSanguineo tipo = tipos[random.nextInt(tipos.length)];
                int quantidadeSolicitada = 1 + random.nextInt(10);
                UrgenciaRequisicao urgencia = urgencias[random.nextInt(urgencias.length)];
                LocalDate prazoLimite = LocalDate.now().plusDays(1 + random.nextInt(10));
                // Espalha a data de criacao pelo ultimo ano, para que o filtro "desde" do painel faca sentido.
                LocalDateTime dataCriacao = LocalDateTime.now()
                        .minusDays(random.nextInt(365))
                        .minusSeconds(random.nextInt(86_400));

                parametros.add(new Object[] {
                        hospitalId,
                        componente.name(),
                        tipo.name(),
                        quantidadeSolicitada,
                        urgencia.name(),
                        Date.valueOf(prazoLimite),
                        Timestamp.valueOf(dataCriacao)
                });
            }

            jdbcTemplate.batchUpdate(SQL_INSERT, parametros);
            restante -= tamanhoDoLote;
        }
        long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;

        return new ResultadoGeracaoDados(quantidade, hospitalIds.size(), duracaoMs);
    }

    private List<Long> garantirHospitais(int minimo) {
        long existentes = hospitalRepository.count();
        if (existentes < minimo) {
            Random random = new Random();
            List<Hospital> novos = new ArrayList<>();
            for (long i = existentes; i < minimo; i++) {
                String sufixo = i + "-" + random.nextInt(1_000_000);
                novos.add(new Hospital("Hospital Sintetico " + sufixo, "CNPJ-" + sufixo, "0000-0000"));
            }
            hospitalRepository.saveAll(novos);
        }
        return hospitalRepository.findAll().stream().map(Hospital::getId).toList();
    }
}
