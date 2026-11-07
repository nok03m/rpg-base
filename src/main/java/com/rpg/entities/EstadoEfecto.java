package com.rpg.entities;

import com.rpg.entities.enums.TipoEfecto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Efecto periodico (DoT o HoT): cambia la vida un valor fijo en cada tick durante cierta duracion. */
// TALLER ARCHIVOS (seccion 5): Serializable; los efectos activos de Personaje y de Pocion lo referencian.
public class EstadoEfecto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final TipoEfecto tipo;
    private final int valorPorTick;
    // TALLER CONCURRENCIA: desde el taller de hilos, turnosRestantes se decrementa en el
    // hilo del efecto mientras otros hilos lo leen (por ejemplo al mostrar el estado).
    // volatile garantiza que todos los hilos vean siempre el valor actualizado.
    private volatile int turnosRestantes;

    public EstadoEfecto(String nombre, TipoEfecto tipo, int valorPorTick, int duracion) {
        // Validacion: un efecto sin nombre, sin tipo, con valor no positivo o sin duracion
        // no tiene sentido y fallaba mas tarde (NPE en esDanio, bucles sin fin...).
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del efecto no puede estar vacio");
        }
        if (valorPorTick <= 0) {
            throw new IllegalArgumentException("El valor por tick debe ser positivo");
        }
        if (duracion <= 0) {
            throw new IllegalArgumentException("La duracion del efecto debe ser positiva");
        }
        this.nombre = nombre;
        this.tipo = Objects.requireNonNull(tipo, "El tipo de efecto no puede ser nulo");
        this.valorPorTick = valorPorTick;
        this.turnosRestantes = duracion;
    }

    public EstadoEfecto copia() {
        return new EstadoEfecto(nombre, tipo, valorPorTick, turnosRestantes);
    }

    public void consumirTurno() {
        turnosRestantes--;
    }

    public boolean haTerminado() {
        return turnosRestantes <= 0;
    }

    public boolean esDanio() {
        return tipo == TipoEfecto.DANIO_CONTINUO;
    }

    public String getNombre() {
        return nombre;
    }

    public int getValorPorTick() {
        return valorPorTick;
    }

    public int getTurnosRestantes() {
        return turnosRestantes;
    }
}
