package co.edu.puj.rpg.engine;

import co.edu.puj.rpg.modelo.Item;

import java.util.Map;
import java.util.TreeMap;

/**
 * Catalogo de la tienda de items. El TreeMap mantiene las entradas
 * ordenadas automaticamente por el nombre del articulo, ideal para
 * presentar el listado de precios en consola.
 */
public class Tienda {

    private final Map<String, Double> catalogo = new TreeMap<>();

    /** Incorpora un item al catalogo con su valor comercial ya escalado. */
    public void agregarItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("El item no puede ser nulo");
        }
        this.catalogo.put(item.getNombre(), item.getValorComercial());
    }

    /** Verificacion de clave antes de consultar el precio. */
    public boolean existeItem(String nombre) {
        return this.catalogo.containsKey(nombre);
    }

    /**
     * @throws IllegalArgumentException si el item no figura en el catalogo
     *         (nunca se retorna un precio nulo o inventado)
     */
    public double consultarPrecio(String nombre) {
        if (!this.catalogo.containsKey(nombre)) {
            throw new IllegalArgumentException("Item no catalogado: " + nombre);
        }
        return this.catalogo.get(nombre);
    }

    public int getCantidadItems() {
        return this.catalogo.size();
    }

    /**
     * Listado del catalogo recorriendo entrySet; al ser TreeMap, las claves
     * salen ordenadas alfabeticamente de forma automatica.
     */
    public String generarCatalogo() {
        StringBuilder sb = new StringBuilder();
        if (this.catalogo.isEmpty()) {
            sb.append("(catalogo vacio)");
        } else {
            for (Map.Entry<String, Double> entrada : this.catalogo.entrySet()) {
                sb.append(String.format("%-18s $%.2f%n", entrada.getKey(), entrada.getValue()));
            }
        }
        return sb.toString();
    }
}
