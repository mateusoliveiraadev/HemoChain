package com.hemochain.entity;

import java.time.Instant;
import jakarta.persistence.*;

/** Atestacao manual de entrega ocorrida, sem implementar transporte ou reserva. */
@Entity
@Table(indexes = @Index(name = "idx_entrega_pendencia", columnList = "recebidaEm,prazoConfirmacao"))
public class EntregaHospitalar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(nullable = false)
    private RequisicaoHospitalar requisicao;
    @OneToOne(optional = false) @JoinColumn(nullable = false, unique = true)
    private BolsaHemocomponente bolsa;
    @Column(nullable = false) private Instant entregueEm;
    @Column(nullable = false) private Instant prazoConfirmacao;
    @Column(nullable = false) private String origemEntrega;
    private Instant recebidaEm;
    private String origemRecebimento;
    @Enumerated(EnumType.STRING) private CondicaoRecebimento condicao;
    @Column(length = 1000) private String observacao;
    private Instant utilizadaEm;
    private String origemUtilizacao;
    private Instant alertaEm;

    protected EntregaHospitalar() { }
    public EntregaHospitalar(RequisicaoHospitalar requisicao, BolsaHemocomponente bolsa,
            Instant agora, Instant prazoConfirmacao) {
        this.requisicao = requisicao;
        this.bolsa = bolsa;
        this.entregueEm = agora;
        this.prazoConfirmacao = prazoConfirmacao;
        this.origemEntrega = "PERFIL:GESTOR_HEMOCENTRO";
    }
    public void receber(CondicaoRecebimento condicao, String observacao, Instant agora, String origem) {
        this.condicao = condicao;
        this.observacao = observacao;
        this.recebidaEm = agora;
        this.origemRecebimento = origem;
    }
    public void utilizar(Instant agora, String origem) { utilizadaEm = agora; origemUtilizacao = origem; }
    public void alertar(Instant agora) { if (alertaEm == null) alertaEm = agora; }
    public Long getId() { return id; }
    public RequisicaoHospitalar getRequisicao() { return requisicao; }
    public BolsaHemocomponente getBolsa() { return bolsa; }
    public Instant getEntregueEm() { return entregueEm; }
    public Instant getPrazoConfirmacao() { return prazoConfirmacao; }
    public String getOrigemEntrega() { return origemEntrega; }
    public Instant getRecebidaEm() { return recebidaEm; }
    public String getOrigemRecebimento() { return origemRecebimento; }
    public CondicaoRecebimento getCondicao() { return condicao; }
    public String getObservacao() { return observacao; }
    public Instant getUtilizadaEm() { return utilizadaEm; }
    public String getOrigemUtilizacao() { return origemUtilizacao; }
    public Instant getAlertaEm() { return alertaEm; }
}
