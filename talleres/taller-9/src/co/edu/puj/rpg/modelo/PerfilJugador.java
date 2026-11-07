package co.edu.puj.rpg.modelo;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Perfil del jugador: registro global de logros desbloqueados durante sus
 * batallas. El TreeSet garantiza la unicidad de cada logro y los presenta
 * automaticamente ordenados alfabeticamente para su despliegue.
 */
public class PerfilJugador {

    private final Set<String> logros = new TreeSet<>();

    /**
     * Agrega un nuevo logro al registro ordenado.
     * @return true si es un logro nuevo; false si ya habia sido desbloqueado
     */
    public boolean registrarLogro(String logro) {
        if (logro == null || logro.isBlank()) {
            throw new IllegalArgumentException("El logro no puede estar vacio");
        }
        return this.logros.add(logro);
    }

    /**
     * @return el conjunto de logros; la iteracion sigue estrictamente el
     *         orden alfabetico natural garantizado por el TreeSet
     */
    public Set<String> listarLogrosOrdenados() {
        return Collections.unmodifiableSet(this.logros);
    }

    public int getCantidadLogros() {
        return this.logros.size();
    }
}
