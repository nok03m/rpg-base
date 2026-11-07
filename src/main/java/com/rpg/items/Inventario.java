package com.rpg.items;

import com.rpg.exceptions.CapacidadExcedidaException;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Contenedor generico con capacidad limitada: Inventario&lt;Pocion&gt; solo acepta pociones. */
// TALLER ARCHIVOS (seccion 5): Serializable. La generica guarda elementos T: al serializar el
// Inventario, cada elemento Pocion (tambien Serializable) se guarda dentro. Si mañana T fuera
// una clase que no lo sea, ObjectOutputStream lanzaria NotSerializableException en el guardar.
public class Inventario<T> implements Iterable<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<T> elementos = new ArrayList<>();
    private final int capacidad;

    public Inventario(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser positiva");
        }
        this.capacidad = capacidad;
    }

    public void agregar(T elemento) throws CapacidadExcedidaException {
        // Rechazar null AQUI: un elemento nulo dentro de la coleccion explota mucho
        // despues (al usarlo o al serializar), lejos del punto donde se introdujo.
        if (elemento == null) {
            throw new IllegalArgumentException("No se puede agregar un elemento nulo al inventario");
        }
        if (elementos.size() >= capacidad) {
            throw new CapacidadExcedidaException(capacidad);
        }
        elementos.add(elemento);
    }

    public T obtener(int indice) {
        return elementos.get(indice);
    }

    public T quitar(int indice) {
        return elementos.remove(indice);
    }

    public int tamanio() {
        return elementos.size();
    }

    public boolean estaVacio() {
        return elementos.isEmpty();
    }

    /** Vista de solo lectura de los elementos guardados. */
    public List<T> getItems() {
        return Collections.unmodifiableList(elementos);
    }

    @Override
    public Iterator<T> iterator() {
        return elementos.iterator();
    }
}
