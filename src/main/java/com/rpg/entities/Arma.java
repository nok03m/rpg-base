package com.rpg.entities;

import com.rpg.entities.enums.RaridadItem;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

// TALLER ARCHIVOS (seccion 5): Serializable; la referencia desde Personaje.arma la necesita.
public class Arma implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final int danioBase;
    private final RaridadItem raridad;

    public Arma(String nombre, int danioBase, RaridadItem raridad) {
        // Validacion completa en el constructor: nombre y rareza nulos fallaban despues,
        // en getDanioEfectivo (NPE diferido lejos del punto del error).
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del arma no puede estar vacio");
        }
        if (danioBase < 0) {
            throw new IllegalArgumentException("El danio del arma no puede ser negativo");
        }
        this.nombre = nombre;
        this.danioBase = danioBase;
        this.raridad = Objects.requireNonNull(raridad, "La rareza del arma no puede ser nula");
    }

    public double getDanioEfectivo() {
        return danioBase * raridad.getMultiplicadorStat();
    }

    public String getNombre() {
        return nombre;
    }
}
