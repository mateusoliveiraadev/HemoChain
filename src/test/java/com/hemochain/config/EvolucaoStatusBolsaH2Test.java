package com.hemochain.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataAccessException;

class EvolucaoStatusBolsaH2Test {
    @Test void evoluiRestricaoAnteriorSemPerderDados() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:migracao-e04;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        jdbc.execute("create table bolsa_hemocomponente(id bigint primary key, status varchar(40) check (status in ('DISPONIVEL','DESCARTADO')))");
        jdbc.update("insert into bolsa_hemocomponente values (1,'DISPONIVEL')");
        new EvolucaoStatusBolsaH2(ds).afterPropertiesSet();
        jdbc.update("update bolsa_hemocomponente set status='ENTREGUE' where id=1");
        assertEquals("ENTREGUE",jdbc.queryForObject("select status from bolsa_hemocomponente where id=1",String.class));
        assertThrows(DataAccessException.class, () -> jdbc.update("update bolsa_hemocomponente set status='INVALIDO' where id=1"));
        new EvolucaoStatusBolsaH2(ds).afterPropertiesSet();
        assertEquals(1,jdbc.queryForObject("select count(*) from bolsa_hemocomponente",Integer.class));
    }
}
