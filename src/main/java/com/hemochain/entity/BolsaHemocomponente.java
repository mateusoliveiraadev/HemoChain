package com.hemochain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
public class BolsaHemocomponente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoComponente tipoComponente;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSanguineo tipoSanguineo;

    @NotNull
    @Min(1)
    @Max(10000)
    @Column(nullable = false)
    private Integer volumeMl;

    @NotNull
    @PastOrPresent
    @Column(nullable = false)
    private LocalDate dataColeta;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate validade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private StatusBolsa status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime dataDescarte;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String motivoDescarte;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String responsavelDescarte;

    public BolsaHemocomponente() {
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public TipoComponente getTipoComponente() { return tipoComponente; }
    public void setTipoComponente(TipoComponente tipoComponente) { this.tipoComponente = tipoComponente; }
    public TipoSanguineo getTipoSanguineo() { return tipoSanguineo; }
    public void setTipoSanguineo(TipoSanguineo tipoSanguineo) { this.tipoSanguineo = tipoSanguineo; }
    public Integer getVolumeMl() { return volumeMl; }
    public void setVolumeMl(Integer volumeMl) { this.volumeMl = volumeMl; }
    public LocalDate getDataColeta() { return dataColeta; }
    public void setDataColeta(LocalDate dataColeta) { this.dataColeta = dataColeta; }
    public LocalDate getValidade() { return validade; }
    public void setValidade(LocalDate validade) { this.validade = validade; }
    public StatusBolsa getStatus() { return status; }
    public void setStatus(StatusBolsa status) { this.status = status; }
    public LocalDateTime getDataDescarte() { return dataDescarte; }
    public String getMotivoDescarte() { return motivoDescarte; }
    public String getResponsavelDescarte() { return responsavelDescarte; }

    // Nao ha exclusao fisica nem edicao do historico pela API.
    public void descartar(LocalDateTime momento) {
        if (status != StatusBolsa.DESCARTADO) {
            status = StatusBolsa.DESCARTADO;
            // Mesma precisao do TIMESTAMP(6) do H2, inclusive na resposta inicial.
            dataDescarte = momento.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            motivoDescarte = "Validade atingida ou ultrapassada";
            responsavelDescarte = "SISTEMA";
        }
    }
}
