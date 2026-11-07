package com.rpg.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Agregacion: disolver el escuadron no destruye a los personajes. */
public class Escuadron implements Iterable<Personaje> {

    private final String nombre;
    private final List<Personaje> miembros = new ArrayList<>();

    public Escuadron(String nombre) {
        this.nombre = nombre;
    }

    public boolean agregar(Personaje personaje) {
        if (personaje == null || miembros.contains(personaje)) {
            return false;
        }
        return miembros.add(personaje);
    }

    public boolean remover(Personaje personaje) {
        return miembros.remove(personaje);
    }

    /** Acceso a una posicion concreta de la lista dinamica. */
    public Personaje obtener(int indice) {
        return miembros.get(indice);
    }

    public int tamanio() {
        return miembros.size();
    }

    public boolean estaVacio() {
        return miembros.isEmpty();
    }

    /** Elimina de la coleccion a las unidades caidas mediante removeIf. */
    public void removerCaidos() {
        miembros.removeIf(personaje -> !personaje.estaVivo());
    }

    /** Suma de la vida de todos los integrantes del escuadron. */
    public int totalVida() {
        int total = 0;
        for (Personaje personaje : miembros) {
            total += personaje.getVida();
        }
        return total;
    }

    @Override
    public Iterator<Personaje> iterator() {
        return miembros.iterator();
    }

    public int contarVivos() {
        int vivos = 0;
        for (Personaje personaje : miembros) {
            if (personaje.estaVivo()) {
                vivos++;
            }
        }
        return vivos;
    }

    public void disolver() {
        miembros.clear();
    }

    public String getNombre() {
        return nombre;
    }

    public List<Personaje> getMiembros() {
        return Collections.unmodifiableList(miembros);
    }
}
