package co.edu.puj.rpg.modelo.enemigo;

import co.edu.puj.rpg.modelo.Personaje;

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

    // ---- API publica de comportamiento

    public void atacar(Personaje objetivo) {
        objetivo.recibirDano(this.puntosAtaque);
    }

    public void recibirDano(int cantidadDano) {
        if (cantidadDano < 0) {
            return;
        }
        setPuntosVida(this.puntosVida - cantidadDano);
    }

    // ---- setter validado

    public void setPuntosVida(int vida) {
        if (vida <= 0) {
            this.puntosVida = 0;
            this.estaVivo = false;
        } else if (vida > this.vidaMaxima) {
            this.puntosVida = this.vidaMaxima;
            this.estaVivo = true;
        } else {
            this.puntosVida = vida;
            this.estaVivo = true;
        }
    }

    // ---- getters

    public String getNombre() {
        return this.nombre;
    }

    public int getPuntosVida() {
        return this.puntosVida;
    }

    public int getVidaMaxima() {
        return this.vidaMaxima;
    }

    public int getPuntosAtaque() {
        return this.puntosAtaque;
    }

    public int getNivel() {
        return this.nivel;
    }

    public double getOro() {
        return this.oro;
    }

    public boolean isEstaVivo() {
        return this.estaVivo;
    }
}
