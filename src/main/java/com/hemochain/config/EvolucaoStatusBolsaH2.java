package com.hemochain.config;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import javax.sql.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/** Mantem a copia H2 local anterior compativel, preservando seus dados. */
@Component
@DependsOn("entityManagerFactory")
public class EvolucaoStatusBolsaH2 implements InitializingBean {
    private final DataSource dataSource;
    public EvolucaoStatusBolsaH2(DataSource dataSource) { this.dataSource = dataSource; }
    @Override public void afterPropertiesSet() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            if (!c.getMetaData().getDatabaseProductName().equals("H2")) return;
            try (Statement sql = c.createStatement()) {
                var nomes = new ArrayList<String>();
                try (var rs = sql.executeQuery("""
                    SELECT tc.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                    JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc
                    ON tc.CONSTRAINT_SCHEMA=cc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME=cc.CONSTRAINT_NAME
                    WHERE tc.TABLE_NAME='BOLSA_HEMOCOMPONENTE' AND cc.CHECK_CLAUSE LIKE '%"STATUS"%'
                    """)) { while (rs.next()) nomes.add(rs.getString(1)); }
                for (String nome : nomes) sql.execute("ALTER TABLE bolsa_hemocomponente DROP CONSTRAINT \""
                        + nome.replace("\"", "\"\"") + "\"");
                sql.execute("""
                    ALTER TABLE bolsa_hemocomponente ADD CONSTRAINT hc_status_bolsa
                    CHECK (status IN ('DISPONIVEL','DESCARTADO','ENTREGUE','RECEBIDA','BLOQUEADA','UTILIZADA'))
                    """);
            }
        }
    }
}
