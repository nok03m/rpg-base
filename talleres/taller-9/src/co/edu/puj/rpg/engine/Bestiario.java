package co.edu.puj.rpg.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Contador de enemigos derrotados por el jugador (bestiario). El mapa
 * interno relaciona el nombre del enemigo con la cantidad total de veces
 * que ha sido derrotado, ofreciendo lecturas y escrituras O(1).
 */
public class Bestiario {

    private final Map<String, Integer> enemigosDerrotados = new HashMap<>();

    /**
     * Registra una baja en combate incrementando su contador en 1. La
     * lectura utiliza getOrDefault para evitar NullPointerException en
     * enemigos no registrados previamente.
     *
     * @return true si es la primera victoria sobre ese tipo de enemigo
     */
    public boolean registrarVictoria(String nombreEnemigo) {
        if (nombreEnemigo == null || nombreEnemigo.isBlank()) {
            throw new IllegalArgumentException("El nombre del enemigo no puede estar vacio");
        }
        // Verificacion de clave previa a la actualizacion.
        boolean primeraVictoria = !this.enemigosDerrotados.containsKey(nombreEnemigo);
        this.enemigosDerrotados.put(nombreEnemigo,
                this.enemigosDerrotados.getOrDefault(nombreEnemigo, 0) + 1);
        return primeraVictoria;
    }

    /** Lectura segura del contador de bajas de un enemigo (0 si no existe). */
    public int consultarBajas(String nombreEnemigo) {
        return this.enemigosDerrotados.getOrDefault(nombreEnemigo, 0);
    }

    public boolean estaRegistrado(String nombreEnemigo) {
        return this.enemigosDerrotados.containsKey(nombreEnemigo);
    }

    public int getCantidadTiposDerrotados() {
        return this.enemigosDerrotados.size();
    }

    /**
     * Reporte del bestiario recorriendo las entradas del mapa con entrySet.
     */
    public String generarReporte() {
        StringBuilder sb = new StringBuilder();
        if (this.enemigosDerrotados.isEmpty()) {
            sb.append("(aun no hay victorias registradas)");
        } else {
            for (Map.Entry<String, Integer> entrada : this.enemigosDerrotados.entrySet()) {
                sb.append(String.format("%-18s %d derrota(s)%n",
                        entrada.getKey(), entrada.getValue()));
            }
        }
        return sb.toString();
    }
}
