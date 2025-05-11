package org.fabiano.tfg.engine.model;


public enum JerarquiaCarta {
    // Cartas más altas
    AS_ESPADA(1, Palo.ESPADA, 14),
    AS_BASTO(1, Palo.BASTO, 13),
    SIETE_ESPADA(7, Palo.ESPADA, 12),
    SIETE_ORO(7, Palo.ORO, 11),

    // Cartas del medio
    TRES_CUALQUIERA(3, null, 10),
    DOS_CUALQUIERA(2, null, 9),
    AS_ORO(1, Palo.ORO, 8),
    AS_COPA(1, Palo.COPA, 8),
    REY_CUALQUIERA(12, null, 7),
    CABALLO_CUALQUIERA(11, null, 6),
    SOTA_CUALQUIERA(10, null, 5),

    // Cartas más bajas
    SIETE_COPA(7, Palo.COPA, 4),
    SIETE_BASTO(7, Palo.BASTO, 4),
    SEIS_CUALQUIERA(6, null, 3),
    CINCO_CUALQUIERA(5, null, 2),
    CUATRO_CUALQUIERA(4, null, 1);

    private final int numero;
    private final Palo palo;
    private final int valorJerarquico;

    JerarquiaCarta(int numero, Palo palo, int valorJerarquico) {
        this.numero = numero;
        this.palo = palo;
        this.valorJerarquico = valorJerarquico;
    }

    public int getNumero() {
        return numero;
    }

    public Palo getPalo() {
        return palo;
    }

    public int getValorJerarquico() {
        return valorJerarquico;
    }
}