package co.edu.puj.rpg.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Escuadron tactico de combatientes. Modela una Agregacion (0..*) con
 * Personaje: los personajes se crean de forma externa, se reciben como
 * parametro y conservan su ciclo de vida propio. Si el escuadron se
 * elimina, sus integrantes siguen activos e intactos en memoria.
 */
public class Escuadron {

    private String nombre;
    private List<Personaje> miembros; // Agregacion (0..*)

    public Escuadron(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del escuadron no puede estar vacio");
        }
        this.nombre = nombre;
        this.miembros = new ArrayList<>(); // Inicializacion de la coleccion
    }

    /**
     * Recluta un personaje externo sin alterar su instanciacion ni su
     * estado: la agregacion solo agrupa referencias ya existentes.
     */
    public void reclutarPersonaje(Personaje p) {
        if (p == null) {
            System.out.println("No se puede reclutar un personaje inexistente.");
            return;
        }
        if (this.miembros.contains(p)) {
            System.out.println(p.getNombre() + " ya combate en el escuadron " + this.nombre + ".");
            return;
        }
        this.miembros.add(p);
        System.out.printf("%s se incorpora al escuadron %s.%n", p.getNombre(), this.nombre);
    }

    /**
     * Expulsa a un personaje del escuadron. El personaje mantiene su
     * estado, vida y objetos intactos en memoria (ciclo de vida independiente).
     */
    public void expulsarPersonaje(Personaje p) {
        if (p == null) {
            return;
        }
        if (this.miembros.remove(p)) {
            System.out.printf("%s sale del escuadron %s y conserva todo su estado.%n",
                    p.getNombre(), this.nombre);
        } else {
            System.out.println(p.getNombre() + " no combate en el escuadron " + this.nombre + ".");
        }
    }

    public String getNombre() {
        return nombre;
    }

    /** Copia defensiva: evita que terceros muten la lista interna. */
    public List<Personaje> getMiembros() {
        return new ArrayList<>(this.miembros);
    }

    public int getCantidadMiembros() {
        return this.miembros.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Escuadron ").append(nombre).append(" (").append(miembros.size()).append(" miembros): ");
        if (miembros.isEmpty()) {
            sb.append("sin combatientes");
        } else {
            for (int i = 0; i < miembros.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(miembros.get(i).getNombre());
            }
        }
        return sb.toString();
    }
}
