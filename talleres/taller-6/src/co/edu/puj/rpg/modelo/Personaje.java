package co.edu.puj.rpg.modelo;

/**
 * Heroe del simulador de batalla. Todos sus atributos son private: el estado
 * interno solo puede leerse o modificarse a traves de la API publica de la clase.
 */
public class Personaje {

    private static final int NIVEL_INICIAL = 1;
    private static final int VIDA_BASE = 100;
    private static final int ATAQUE_BASE = 10;
    private static final double ORO_INICIAL = 0.0;

    private String nombre;
    private int puntosVida;
    private int vidaMaxima;
    private int puntosAtaque;
    private int nivel;
    private double oro;
    private boolean estaVivo;

    public Personaje(String nombre, int vidaMaxima, int puntosAtaque, double oro) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        if (vidaMaxima <= 0 || puntosAtaque < 0 || oro < 0.0) {
            throw new IllegalArgumentException("Estadisticas invalidas para " + nombre);
        }
        this.nombre = nombre;
        this.vidaMaxima = vidaMaxima;
        this.puntosAtaque = puntosAtaque;
        this.oro = oro;
        this.puntosVida = vidaMaxima;
        this.nivel = NIVEL_INICIAL;
        this.estaVivo = true;
    }

    public Personaje(String nombre) {
        this(nombre, VIDA_BASE, ATAQUE_BASE, ORO_INICIAL);
    }
}
