package co.edu.puj.rpg.modelo.enemigo;

/**
 * Entidad rival del heroe, en un subpaquete propio para evidenciar las barreras
 * de acceso entre paquetes. Replica la estructura de atributos privados.
 */
public class Enemigo {

    private static final int NIVEL_INICIAL = 1;
    private static final int VIDA_BASE = 80;
    private static final int ATAQUE_BASE = 12;
    private static final double ORO_BASE = 25.0;

    private String nombre;
    private int puntosVida;
    private int vidaMaxima;
    private int puntosAtaque;
    private int nivel;
    private double oro;
    private boolean estaVivo;

    public Enemigo(String nombre, int vidaMaxima, int puntosAtaque, double oro) {
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

    public Enemigo(String nombre) {
        this(nombre, VIDA_BASE, ATAQUE_BASE, ORO_BASE);
    }
}
