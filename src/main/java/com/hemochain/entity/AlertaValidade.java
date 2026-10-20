package com.hemochain.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"bolsa_id", "limiarDias"}))
public class AlertaValidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "bolsa_id", nullable = false)
    private BolsaHemocomponente bolsa;
    @Column(nullable = false)
    private int limiarDias;
    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    public AlertaValidade() {
    }

    public AlertaValidade(BolsaHemocomponente bolsa, int limiarDias, LocalDateTime dataCriacao) {
        this.bolsa = bolsa;
        this.limiarDias = limiarDias;
        this.dataCriacao = dataCriacao;
    }

    public Long getId() { return id; }
    public BolsaHemocomponente getBolsa() { return bolsa; }
    public int getLimiarDias() { return limiarDias; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
