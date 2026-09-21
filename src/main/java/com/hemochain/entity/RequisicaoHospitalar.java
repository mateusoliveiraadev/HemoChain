package com.hemochain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
public class RequisicaoHospitalar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @ManyToOne
    private Hospital hospital;

    // So para receber o id do hospital escolhido no formulario (nao e uma coluna).
    // O Hospital completo e resolvido e atribuido pelo RequisicaoService.
    @NotNull
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long hospitalId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoComponente tipoComponente;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoSanguineo tipoSanguineo;

    @Min(1)
    private int quantidade;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UrgenciaRequisicao urgencia;

    @NotNull
    private LocalDate prazoLimite;

    private LocalDateTime dataCriacao;

    public RequisicaoHospitalar() {
        this.dataCriacao = LocalDateTime.now();
    }

    public RequisicaoHospitalar(Long hospitalId, TipoComponente tipoComponente, TipoSanguineo tipoSanguineo,
            int quantidade, UrgenciaRequisicao urgencia, LocalDate prazoLimite) {
        this.hospitalId = hospitalId;
        this.tipoComponente = tipoComponente;
        this.tipoSanguineo = tipoSanguineo;
        this.quantidade = quantidade;
        this.urgencia = urgencia;
        this.prazoLimite = prazoLimite;
        this.dataCriacao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public TipoComponente getTipoComponente() {
        return tipoComponente;
    }

    public void setTipoComponente(TipoComponente tipoComponente) {
        this.tipoComponente = tipoComponente;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public void setTipoSanguineo(TipoSanguineo tipoSanguineo) {
        this.tipoSanguineo = tipoSanguineo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public UrgenciaRequisicao getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(UrgenciaRequisicao urgencia) {
        this.urgencia = urgencia;
    }

    public LocalDate getPrazoLimite() {
        return prazoLimite;
    }

    public void setPrazoLimite(LocalDate prazoLimite) {
        this.prazoLimite = prazoLimite;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
