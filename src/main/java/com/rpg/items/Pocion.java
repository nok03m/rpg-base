package com.rpg.items;

import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.enums.RaridadItem;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

// TALLER ARCHIVOS (seccion 5): Serializable; guarda un EstadoEfecto (tambien Serializable).
public class Pocion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final int curacion;
    private final RaridadItem raridad;
    private final EstadoEfecto efecto;
    private final boolean purifica;

    public Pocion(String nombre, int curacion, RaridadItem raridad) {
        this(nombre, curacion, raridad, null, false);
    }

    /**
     * @param efecto   efecto que se aplica al usarla (por ejemplo un HoT), o null
     * @param purifica si es true, elimina los efectos de dano continuo (antidoto)
     */
    public Pocion(String nombre, int curacion, RaridadItem raridad, EstadoEfecto efecto, boolean purifica) {
        // Validacion en el constructor: un dato invalido debe fallar AQUI y no mas tarde
        // (por ejemplo un NPE en getCuracionReal lejos del punto del error).
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la pocion no puede estar vacio");
        }
        if (curacion < 0) {
            throw new IllegalArgumentException("La curacion de la pocion no puede ser negativa");
        }
        this.nombre = nombre;
        this.curacion = curacion;
        this.raridad = Objects.requireNonNull(raridad, "La rareza de la pocion no puede ser nula");
        this.efecto = efecto;
        this.purifica = purifica;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCuracionReal() {
        return (int) (curacion * raridad.getMultiplicadorStat());
    }

    public EstadoEfecto getEfecto() {
        return efecto;
    }

    public boolean isPurifica() {
        return purifica;
    }
}
