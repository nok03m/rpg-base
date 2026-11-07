package com.rpg.core;

/**
 * Callback que recibe el aviso de que el tiempo del turno se agoto.
 * Se implementa con una lambda desde {@code MotorJuego.pedirAccion}.
 */
@FunctionalInterface
public interface TurnoCallback {

    /** Invocado por el hilo del temporizador cuando llega a cero. */
    void onTiempoAgotado();
}
