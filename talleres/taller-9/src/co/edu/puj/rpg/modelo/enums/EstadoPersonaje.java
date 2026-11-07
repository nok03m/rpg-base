package co.edu.puj.rpg.modelo.enums;

/**
 * Ciclo de vida y alteraciones de estado de los heroes en la arena.
 * Gobierna la toma de decisiones del bucle de combate: un personaje
 * ATURDIDO pierde su turno de ataque y un personaje ENVENENADO sufre
 * dano por turno durante la fase de actualizacion.
 */
public enum EstadoPersonaje {
    VIVO,
    DERROTADO,
    ENVENENADO,
    ATURDIDO
}
