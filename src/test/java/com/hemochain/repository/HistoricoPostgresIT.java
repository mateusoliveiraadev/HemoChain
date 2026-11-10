package com.hemochain.repository;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import com.hemochain.entity.*;
import com.hemochain.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Executado por mvn -Ppostgres-it verify contra PostgreSQL real configurado no ambiente. */
@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles("postgres-test")
class HistoricoPostgresIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired BolsaService bolsas;
    @Autowired BolsaRepository repository;
    @Autowired RastreabilidadeService historico;
    @Autowired HospitalService hospitais;
    @Autowired RequisicaoService requisicoes;
    @Autowired RequisicaoRepository requisicaoRepository;
    @Autowired AtendimentoHospitalarService atendimento;

    private BolsaHemocomponente cadastrar() {
        var b = new BolsaHemocomponente(); b.setCodigo("PG-"+UUID.randomUUID());
        b.setTipoComponente(TipoComponente.CONCENTRADO_HEMACIAS); b.setTipoSanguineo(TipoSanguineo.O_NEGATIVO);
        b.setVolumeMl(450); b.setDataColeta(LocalDate.now());
        return bolsas.cadastrar(b);
    }
    private Long requisicao() {
        var h = hospitais.cadastrar(new Hospital("Hospital PG sintetico", "SINTETICO", "0000"));
        return requisicoes.cadastrar(new RequisicaoHospitalar(h.getId(), TipoComponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.O_NEGATIVO, 1, UrgenciaRequisicao.ROTINA, LocalDate.now().plusDays(1))).getId();
    }

    @Test void bddSqlDiretoNaoAlteraNemExcluiERegistroOriginalPermanece() {
        var b = cadastrar(); var original = historico.historico(b.getId()).get(0);
        assertThrows(DataAccessException.class, () -> jdbc.update("update evento_bolsa set origem='FORJADO' where id=?", original.getId()));
        assertThrows(DataAccessException.class, () -> jdbc.update("delete from evento_bolsa where id=?", original.getId()));
        assertThrows(DataAccessException.class, () -> jdbc.execute("truncate evento_bolsa"));
        var preservado = historico.historico(b.getId()).get(0);
        assertEquals(original.getOrigem(), preservado.getOrigem());
        assertEquals(original.getDetalhes(), preservado.getDetalhes());
        assertEquals(original.getMomento(), preservado.getMomento());
        assertThrows(DataAccessException.class, () -> repository.deleteById(b.getId()));
    }

    @Test void duasRequisicoesConcorrentesNaoEntregamAMesmaBolsa() throws Exception {
        var b = cadastrar(); Long r1 = requisicao(), r2 = requisicao();
        var barreira = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (Long id : List.of(r1,r2)) resultados.add(pool.submit(() -> {
                barreira.await();
                try { atendimento.registrarEntrega(id,b.getId()); return true; }
                catch (org.springframework.web.server.ResponseStatusException erro) {
                    assertEquals(409, erro.getStatusCode().value()); return false;
                }
            }));
            barreira.countDown();
            int sucessos = 0; for (var f : resultados) if (f.get(20,TimeUnit.SECONDS)) sucessos++;
            assertEquals(1,sucessos);
            assertEquals(2,historico.historico(b.getId()).size());
        } finally { pool.shutdownNow(); }
    }

    @Test void confirmacoesConcorrentesRegistramUmUnicoRecebimento() throws Exception {
        var b = cadastrar(); Long r = requisicao();
        var entrega = atendimento.registrarEntrega(r,b.getId());
        Long hospitalId = requisicaoRepository.findById(r).orElseThrow().getHospital().getId();
        var barreira = new CountDownLatch(1); ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> confirmar = () -> {barreira.await();try {
                atendimento.receber(r,entrega.getId(),hospitalId,CondicaoRecebimento.INTEGRA,"");return true;
            } catch (org.springframework.web.server.ResponseStatusException erro) {
                assertEquals(409,erro.getStatusCode().value());return false;
            }};
            var a = pool.submit(confirmar);var c = pool.submit(confirmar);barreira.countDown();
            assertNotEquals(a.get(20,TimeUnit.SECONDS),c.get(20,TimeUnit.SECONDS));
            assertEquals(3,historico.historico(b.getId()).size());
        } finally {pool.shutdownNow();}
    }

    @Test void migraCheckLegadoEmSchemaIsoladoSemApagarBolsa() throws Exception {
        String schema = "e04_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("create schema " + schema);
        String baseUrl = System.getenv("HEMOCHAIN_TEST_DB_URL");
        var ds = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                System.getenv("HEMOCHAIN_TEST_DB_USER"),System.getenv("HEMOCHAIN_TEST_DB_PASSWORD"));
        var isolado = new JdbcTemplate(ds);
        try {
            isolado.execute("create table bolsa_hemocomponente(id bigint primary key, status varchar(40) check (status in ('DISPONIVEL','DESCARTADO')))");
            isolado.execute("create table evento_bolsa(id bigint primary key)");
            isolado.update("insert into bolsa_hemocomponente values (1,'DISPONIVEL')");
            var migracao = new com.hemochain.config.ProtecaoHistoricoPostgres(ds);
            migracao.afterPropertiesSet();migracao.afterPropertiesSet();
            isolado.update("update bolsa_hemocomponente set status='ENTREGUE' where id=1");
            assertEquals("ENTREGUE",isolado.queryForObject("select status from bolsa_hemocomponente where id=1",String.class));
            assertThrows(DataAccessException.class, () -> isolado.update("update bolsa_hemocomponente set status='INVALIDO' where id=1"));
        } finally {
            // Schema exclusivo criado por este teste; nenhuma tabela da aplicacao e removida.
            jdbc.execute("drop schema " + schema + " cascade");
        }
    }
}
