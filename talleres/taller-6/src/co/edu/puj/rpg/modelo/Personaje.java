package co.edu.puj.rpg.modelo;

import co.edu.puj.rpg.modelo.enemigo.Enemigo;

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

    // ---- API publica de comportamiento

    public void atacar(Enemigo enemigo) {
        enemigo.recibirDano(this.puntosAtaque);
    }

    public void usarPocion(int cantidadRestaurada) {
        setPuntosVida(this.puntosVida + cantidadRestaurada);
    }

    public void recibirDano(int cantidadDano) {
        if (cantidadDano < 0) {
            return;
        }
        setPuntosVida(this.puntosVida - cantidadDano);
    }

    // ---- setters validados

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

    /** @return true si la operacion fue aceptada; false si el saldo seria insuficiente */
    public boolean setOro(double oro) {
        if (oro < 0.0) {
            return false;
        }
        this.oro = oro;
        return true;
    }

    protected void setNivel(int nivel) {
        if (nivel >= 1) {
            this.nivel = nivel;
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
