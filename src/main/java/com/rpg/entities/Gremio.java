package com.rpg.entities;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Agregacion: el gremio guarda referencias a personajes que existen por su cuenta. */
// TALLER ARCHIVOS (seccion 5): Serializable. La lista guarda Personaje (tambien Serializable) y
// el ciclo Gremio <-> Personaje se serializa sin problema: Java asigna a cada objeto un id una
// sola vez y al cargar reconstruye la misma red de referencias.
public class Gremio implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final List<Personaje> miembros = new ArrayList<>();

    public Gremio(String nombre) {
        // Validacion homogenea con el resto del modelo (Pocion, Arma, EstadoEfecto):
        // un gremio sin nombre romperia los reportes y el menu de guardado.
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del gremio no puede estar vacio");
        }
        this.nombre = nombre;
    }

    public void agregarMiembro(Personaje personaje) {
        if (personaje == null || miembros.contains(personaje)) {
            return;
        }
        miembros.add(personaje);
        if (personaje.getGremio() != this) {
            personaje.unirseAGremio(this);
        }
    }

    public void removerMiembro(Personaje personaje) {
        if (!miembros.remove(personaje)) {
            return;
        }
        if (personaje.getGremio() == this) {
            personaje.abandonarGremio();
        }
    }

    public String getNombre() {
        return nombre;
    }

    public List<Personaje> getMiembros() {
        return Collections.unmodifiableList(miembros);
    }
}
