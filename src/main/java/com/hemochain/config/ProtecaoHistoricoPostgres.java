package com.hemochain.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/** Protege inclusive SQL direto. Erro de instalacao impede a inicializacao. */
@Component
@DependsOn("entityManagerFactory")
@DependsOnDatabaseInitialization
public class ProtecaoHistoricoPostgres implements InitializingBean {
    private final DataSource dataSource;

    public ProtecaoHistoricoPostgres(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public void afterPropertiesSet() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")) return;
            connection.setAutoCommit(false);
            try (Statement sql = connection.createStatement()) {
                // ddl-auto=update nao substitui o CHECK de enums de instalacoes anteriores.
                sql.execute("""
                    DO $$ DECLARE c record; BEGIN
                      FOR c IN SELECT conname FROM pg_constraint
                        WHERE conrelid = 'bolsa_hemocomponente'::regclass AND contype = 'c'
                          AND pg_get_constraintdef(oid) LIKE '%status%'
                      LOOP EXECUTE format('ALTER TABLE bolsa_hemocomponente DROP CONSTRAINT %I', c.conname); END LOOP;
                      ALTER TABLE bolsa_hemocomponente ADD CONSTRAINT hc_status_bolsa
                        CHECK (status IN ('DISPONIVEL','DESCARTADO','ENTREGUE','RECEBIDA','BLOQUEADA','UTILIZADA'));
                    END $$
                    """);
                sql.execute("""
                    CREATE OR REPLACE FUNCTION hemochain_impedir_alteracao_evento() RETURNS trigger
                    LANGUAGE plpgsql AS $$ BEGIN
                      RAISE EXCEPTION 'Historico imutavel: alteracao, exclusao e truncate bloqueados';
                    END $$
                    """);
                sql.execute("""
                    CREATE OR REPLACE TRIGGER evento_bolsa_imutavel
                    BEFORE UPDATE OR DELETE OR TRUNCATE ON evento_bolsa
                    FOR EACH STATEMENT EXECUTE FUNCTION hemochain_impedir_alteracao_evento()
                    """);
                connection.commit();
            } catch (Exception erro) {
                connection.rollback();
                throw erro;
            }
        }
    }
}
