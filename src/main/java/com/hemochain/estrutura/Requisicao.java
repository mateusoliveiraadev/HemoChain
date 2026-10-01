package com.hemochain.estrutura;

import com.hemochain.entity.TipoComponente;
import com.hemochain.entity.TipoSanguineo;
import com.hemochain.entity.UrgenciaRequisicao;

/**
 * Dado de uma requisicao hospitalar, equivalente ao struct Requisicao
 * em C (fila.h) - mesmos campos, mesmo hospitalId como simples
 * referencia numerica (em vez do relacionamento @ManyToOne que a
 * entidade JPA RequisicaoHospitalar usa para persistencia).
 *
 * Existe separada de com.hemochain.entity.RequisicaoHospitalar de
 * proposito: aquela e a entidade persistida pelo Spring Data JPA (tem
 * anotacoes e um relacionamento com Hospital); esta e o dado puro que
 * circula pela fila, sem nenhuma dependencia de framework. Quando o
 * time decidir como a fila conversa com a camada de persistencia,
 * essa conversao fica isolada num unico ponto, em vez de espalhada
 * pela estrutura de dados.
 */
public class Requisicao {

    private final int id;
    private final int hospitalId;
    private final TipoComponente tipoComponente;
    private final TipoSanguineo tipoSanguineo;
    private final int quantidade;
    private final UrgenciaRequisicao urgencia;

    public Requisicao(int id, int hospitalId, TipoComponente tipoComponente, TipoSanguineo tipoSanguineo,
            int quantidade, UrgenciaRequisicao urgencia) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.tipoComponente = tipoComponente;
        this.tipoSanguineo = tipoSanguineo;
        this.quantidade = quantidade;
        this.urgencia = urgencia;
    }

    public int getId() {
        return id;
    }

    public int getHospitalId() {
        return hospitalId;
    }

    public TipoComponente getTipoComponente() {
        return tipoComponente;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public UrgenciaRequisicao getUrgencia() {
        return urgencia;
    }

    @Override
    public String toString() {
        return "requisicao #" + id + " | hospital=" + hospitalId + " | tipo=" + tipoSanguineo
                + " | componente=" + tipoComponente + " | qtd=" + quantidade + " | urgencia=" + urgencia;
    }
}
