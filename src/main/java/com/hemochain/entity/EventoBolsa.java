package com.hemochain.entity;

import java.time.Instant;
import jakarta.persistence.*;

/** Registro append-only. Nao recebe dados de entrada da API. */
@Entity
@Table(indexes = @Index(name = "idx_evento_bolsa_ordem", columnList = "bolsa_id,momento,id"))
public class EventoBolsa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(nullable = false, updatable = false)
    private BolsaHemocomponente bolsa;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false)
    private TipoEventoBolsa tipo;
    @Column(nullable = false, updatable = false)
    private Instant momento;
    @Column(nullable = false, updatable = false, length = 160)
    private String origem;
    @Column(nullable = false, updatable = false, length = 4000)
    private String detalhes;

    protected EventoBolsa() { }

    public EventoBolsa(BolsaHemocomponente bolsa, TipoEventoBolsa tipo, String origem, String detalhes) {
        this.bolsa = bolsa;
        this.tipo = tipo;
        this.origem = origem;
        this.detalhes = detalhes;
        this.momento = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    @PreUpdate @PreRemove
    private void impedirAlteracao() {
        throw new IllegalStateException("Eventos historicos nao podem ser alterados ou excluidos.");
    }

    public Long getId() { return id; }
    public Long getBolsaId() { return bolsa.getId(); }
    public TipoEventoBolsa getTipo() { return tipo; }
    public Instant getMomento() { return momento; }
    public String getOrigem() { return origem; }
    public String getDetalhes() { return detalhes; }
}
