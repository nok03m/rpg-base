package com.rpg.entities;

/**
 * Taller 4.2 - Seccion 2: puente entre el dominio (Personaje) y la capa de
 * concurrencia (GestorEfectos). Cuando el dominio aplica o quita un efecto,
 * avisa al oyente para que ese efecto corra en su propio hilo (o se cancele
 * con interrupt()). El Personaje no conoce hilos: solo esta interfaz.
 */
public interface OyenteEfectos {

    /**
     * Un efecto se aplico sobre el personaje: nace un hilo que lo procesa.
     * @param objetivo personaje afectado
     * @param efecto   copia guardada en el mapa de efectos del personaje
     */
    void efectoAplicado(Personaje objetivo, EstadoEfecto efecto);

    /**
     * Un efecto se quito (antidoto, purificacion o fin natural): el hilo
     * correspondiente debe interrumpirse con interrupt().
     * @param objetivo      personaje afectado
     * @param nombreEfecto  nombre unico del efecto quitado
     */
    void efectoQuitado(Personaje objetivo, String nombreEfecto);
}
