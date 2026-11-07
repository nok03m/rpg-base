package com.rpg.core;

/** Acciones que el jugador puede elegir en su turno. */
public enum Accion {
    ATACAR,
    ESPECIAL,
    POCION,
    ESTADO,
    HUIR,
    /** El temporizador vencio antes de que el jugador eligiera: pierde el turno. */
    TURNO_PERDIDO
}
