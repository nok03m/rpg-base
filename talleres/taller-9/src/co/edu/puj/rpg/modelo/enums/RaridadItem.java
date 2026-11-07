package co.edu.puj.rpg.modelo.enums;

/**
 * Clasificacion jerarquica de los items del equipamiento e inventario.
 * El multiplicador escala el valor comercial y el impacto en estadisticas.
 */
public enum RaridadItem {

    COMUN(1.0),
    RARO(1.5),
    LEGENDARIO(2.5);

    private final double multiplicadorStat;

    RaridadItem(double multiplicadorStat) {
        this.multiplicadorStat = multiplicadorStat;
    }

    public double getMultiplicadorStat() {
        return multiplicadorStat;
    }
}
