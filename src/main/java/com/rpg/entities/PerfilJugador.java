package com.rpg.entities;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

// TALLER ARCHIVOS (seccion 5): Serializable (victorias, derrotas y logros son parte de la partida).
public class PerfilJugador implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int victorias;
    private int derrotas;
    private final Set<String> logros = new TreeSet<>();

    public void registrarVictoria() {
        victorias++;
    }

    public void registrarDerrota() {
        derrotas++;
    }

    /** @return true si el logro era nuevo; false si ya estaba desbloqueado o es invalido */
    public boolean desbloquearLogro(String logro) {
        // Validacion homogenea con el resto del modelo (Pocion, Arma, Gremio, Escuadron):
        // TreeSet.add(null) lanzaria NPE sin control, y un logro en blanco no es logro.
        if (logro == null || logro.isBlank()) {
            return false;
        }
        return logros.add(logro);
    }

    public int getVictorias() {
        return victorias;
    }

    public int getDerrotas() {
        return derrotas;
    }

    /** Los logros salen ordenados alfabeticamente porque se guardan en un TreeSet. */
    public Set<String> getLogros() {
        return Collections.unmodifiableSet(logros);
    }

    /**
     * TALLER ARCHIVOS (seccion 5): restaura el perfil desde una partida cargada del .dat.
     * @return true si el perfil cargado era distinto del actual
     */
    public boolean restaurar(int victoriasGuardadas, int derrotasGuardadas, Collection<String> logrosGuardados) {
        boolean cambio = this.victorias != victoriasGuardadas || this.derrotas != derrotasGuardadas
                || !logros.equals(new TreeSet<>(logrosGuardados));
        this.victorias = victoriasGuardadas;
        this.derrotas = derrotasGuardadas;
        logros.clear();
        logros.addAll(logrosGuardados);
        return cambio;
    }
}
