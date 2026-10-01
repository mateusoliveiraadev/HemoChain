#ifndef BOLSA_H
#define BOLSA_H

/*
 * Tipos de dominio do sistema "Rota Vital".
 *
 * Importante (restricao da Unidade 1): aqui so guardamos o dado.
 * Nenhuma logica de compatibilidade ABO/Rh, FEFO ou roteirizacao
 * e aplicada sobre estes tipos nesta entrega - isso fica reservado
 * para a Unidade 2.
 */

typedef enum {
    O_NEGATIVO,
    O_POSITIVO,
    A_NEGATIVO,
    A_POSITIVO,
    B_NEGATIVO,
    B_POSITIVO,
    AB_NEGATIVO,
    AB_POSITIVO
} TipoSanguineo;

typedef enum {
    CONCENTRADO_HEMACIAS,
    PLASMA,
    PLAQUETAS,
    CRIOPRECIPITADO
} TipoComponente;

typedef enum {
    ROTINA,
    URGENTE,
    EMERGENCIAL
} UrgenciaRequisicao;

/* Uma bolsa de sangue no estoque do hemocentro. */
typedef struct {
    int id;
    TipoSanguineo tipoSanguineo;
    TipoComponente tipoComponente;
    int quantidadeMl;
    char dataColeta[11]; /* formato "AAAA-MM-DD", apenas armazenado */
} Bolsa;

#endif
