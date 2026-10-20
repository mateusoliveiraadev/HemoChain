package com.hemochain.entity;

public enum TipoSanguineo {
    A_POSITIVO("A", true),
    A_NEGATIVO("A", false),
    B_POSITIVO("B", true),
    B_NEGATIVO("B", false),
    AB_POSITIVO("AB", true),
    AB_NEGATIVO("AB", false),
    O_POSITIVO("O", true),
    O_NEGATIVO("O", false);

    private final String abo;
    private final boolean rhPositivo;

    TipoSanguineo(String abo, boolean rhPositivo) {
        this.abo = abo;
        this.rhPositivo = rhPositivo;
    }

    public String getAbo() {
        return abo;
    }

    public boolean isRhPositivo() {
        return rhPositivo;
    }
}
