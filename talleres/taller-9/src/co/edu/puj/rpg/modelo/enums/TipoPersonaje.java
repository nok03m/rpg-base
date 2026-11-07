package co.edu.puj.rpg.modelo.enums;

/**
 * Arquetipos de heroes disponibles en el juego. No es un mero identificador:
 * cada constante encapsula bonificadores inmutables que afectan las
 * estadisticas base del personaje, eliminando numeros y cadenas magicas.
 */
public enum TipoPersonaje {

    GUERRERO(3.0, 5.0),
    MAGO(7.0, 0.0),
    ARQUERO(5.0, 2.0);

    private final double bonificadorAtaqueBase;
    private final double bonificadorDefensaBase;

    TipoPersonaje(double bonificadorAtaqueBase, double bonificadorDefensaBase) {
        this.bonificadorAtaqueBase = bonificadorAtaqueBase;
        this.bonificadorDefensaBase = bonificadorDefensaBase;
    }

    public double getBonificadorAtaqueBase() {
        return bonificadorAtaqueBase;
    }

    public double getBonificadorDefensaBase() {
        return bonificadorDefensaBase;
    }
}
