package co.edu.puj.rpg.modelo;

import co.edu.puj.rpg.modelo.enums.RaridadItem;

/**
 * Item del equipamiento e inventario, clasificado jerarquicamente por su
 * raridad. El multiplicador de la rareza escala su valor comercial.
 */
public class Item {

    private final String nombre;
    private final double precioBase;
    private final RaridadItem raridad;

    public Item(String nombre, double precioBase, RaridadItem raridad) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del item no puede estar vacio");
        }
        if (precioBase < 0.0) {
            throw new IllegalArgumentException("El precio base no puede ser negativo");
        }
        if (raridad == null) {
            throw new IllegalArgumentException("La rareza del item es obligatoria");
        }
        this.nombre = nombre;
        this.precioBase = precioBase;
        this.raridad = raridad;
    }

    /** Valor comercial escalado por el multiplicador de la rareza. */
    public double getValorComercial() {
        return this.precioBase * this.raridad.getMultiplicadorStat();
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecioBase() {
        return precioBase;
    }

    public RaridadItem getRaridad() {
        return raridad;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nombre).append(" [").append(raridad)
          .append(", valor: ").append(getValorComercial()).append(']');
        return sb.toString();
    }
}
