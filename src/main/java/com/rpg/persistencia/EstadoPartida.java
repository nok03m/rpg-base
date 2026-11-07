package com.rpg.persistencia;

import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * TALLER ARCHIVOS (seccion 5): recipiente del estado de la partida que viaja al .dat.
 * Implementa Serializable y agrupa todo lo que se quiere conservar entre sesiones:
 * el perfil del jugador (victorias, derrotas, logros), el gremio con sus miembros y
 * el contador de enemigos derrotados del bestiario.
 *
 * Se serializa como UN solo objeto con writeObject(): Java sigue las referencias
 * (Gremio -> Personaje -> Gremio...) y reconstruye la red completa al cargar.
 */
public class EstadoPartida implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final PerfilJugador perfil;
    private final Gremio gremio;
    private final Map<String, Integer> enemigosDerrotados;

    public EstadoPartida(PerfilJugador perfil, Gremio gremio, Map<String, Integer> enemigosDerrotados) {
        this.perfil = perfil;
        this.gremio = gremio;
        this.enemigosDerrotados = Map.copyOf(enemigosDerrotados);
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Map<String, Integer> getEnemigosDerrotados() {
        return enemigosDerrotados;
    }
}
