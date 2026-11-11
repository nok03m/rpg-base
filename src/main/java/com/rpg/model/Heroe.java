package com.rpg.model;

/**
 * POJO que representa una fila de la tabla heroes.
 * No sabe nada de JDBC ni de la consola: solo guarda datos y se valida a si mismo.
 */
public class Heroe {

    public static final String TIPO_GUERRERO = "GUERRERO";
    public static final String TIPO_MAGO = "MAGO";

    private int id;
    private String nombre;
    private String tipo;
    private int vidaMaxima;
    private int nivel;

    public Heroe() {
    }

    public Heroe(int id, String nombre, String tipo, int vidaMaxima, int nivel) {
        setId(id);
        setNombre(nombre);
        setTipo(tipo);
        setVidaMaxima(vidaMaxima);
        setNivel(nivel);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("El id no puede ser negativo.");
        }
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio.");
        }
        if (nombre.length() > 50) {
            throw new IllegalArgumentException("El nombre no puede superar 50 caracteres.");
        }
        this.nombre = nombre.trim();
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo no puede ser nulo.");
        }
        String normalizado = tipo.trim().toUpperCase();
        if (!normalizado.equals(TIPO_GUERRERO) && !normalizado.equals(TIPO_MAGO)) {
            throw new IllegalArgumentException("El tipo solo puede ser GUERRERO o MAGO.");
        }
        this.tipo = normalizado;
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser mayor a cero.");
        }
        this.vidaMaxima = vidaMaxima;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        if (nivel < 1) {
            throw new IllegalArgumentException("El nivel debe ser mayor o igual a 1.");
        }
        this.nivel = nivel;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - vida %d, nivel %d",
                id, nombre, tipo, vidaMaxima, nivel);
    }
}
