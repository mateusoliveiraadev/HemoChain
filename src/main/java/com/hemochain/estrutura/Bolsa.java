package com.hemochain.estrutura;

import com.hemochain.entity.TipoComponente;
import com.hemochain.entity.TipoSanguineo;

/**
 * Uma bolsa de sangue no estoque do hemocentro.
 *
 * Classe simples (POO), sem anotacoes de persistencia: e o
 * equivalente direto do struct Bolsa em C (bolsa.h). Reutiliza os
 * enums TipoSanguineo/TipoComponente que ja existem em
 * com.hemochain.entity, para nao duplicar o vocabulario de dominio
 * que o restante da aplicacao Spring Boot ja usa.
 *
 * Restricao da Unidade 1: so armazena o dado. Nenhuma logica de
 * compatibilidade ABO/Rh, FEFO ou roteirizacao e aplicada aqui.
 */
public class Bolsa {

    private final int id;
    private final TipoSanguineo tipoSanguineo;
    private final TipoComponente tipoComponente;
    private final int quantidadeMl;
    private final String dataColeta;

    public Bolsa(int id, TipoSanguineo tipoSanguineo, TipoComponente tipoComponente, int quantidadeMl,
            String dataColeta) {
        this.id = id;
        this.tipoSanguineo = tipoSanguineo;
        this.tipoComponente = tipoComponente;
        this.quantidadeMl = quantidadeMl;
        this.dataColeta = dataColeta;
    }

    public int getId() {
        return id;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public TipoComponente getTipoComponente() {
        return tipoComponente;
    }

    public int getQuantidadeMl() {
        return quantidadeMl;
    }

    public String getDataColeta() {
        return dataColeta;
    }

    @Override
    public String toString() {
        return "bolsa #" + id + " | tipo=" + tipoSanguineo + " | componente=" + tipoComponente
                + " | " + quantidadeMl + "ml | coleta=" + dataColeta;
    }
}
