package co.edu.puj.rpg.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Gremio de heroes. Extremo 0..* de la asociacion bidireccional con
 * Personaje: mantiene la lista explicita de sus integrantes y cada
 * integrante mantiene su referencia al gremio. Toda alta o baja debe
 * sincronizarse en doble via para conservar la coherencia en memoria.
 */
public class Gremio {

    private String nombre;
    private List<Personaje> miembros;

    public Gremio(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del gremio no puede estar vacio");
        }
        this.nombre = nombre;
        this.miembros = new ArrayList<>();
    }

    /**
     * Registra un personaje como miembro. La sincronizacion cruzada invoca a
     * p.unirseAGremio(this) solo si el personaje aun no apunta a este gremio,
     * evitando la recursion infinita.
     */
    public void agregarMiembro(Personaje p) {
        if (p == null) {
            System.out.println("No se puede registrar un personaje inexistente.");
            return;
        }
        if (!this.miembros.contains(p)) {
            this.miembros.add(p);
            System.out.printf("%s queda registrado en el gremio %s.%n", p.getNombre(), this.nombre);
        }
        // Sincronizacion cruzada: si el personaje no apunta a este gremio, se le notifica.
        if (p.getGremio() != this) {
            p.unirseAGremio(this);
        }
    }

    /** Da de baja a un personaje del gremio sincronizando su referencia. */
    public void retirarMiembro(Personaje p) {
        if (p == null) {
            return;
        }
        if (this.miembros.remove(p)) {
            if (p.getGremio() == this) {
                p.abandonarGremio();
            }
        } else {
            System.out.println(p.getNombre() + " no figura en el gremio " + this.nombre + ".");
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
        sb.append("Gremio ").append(nombre).append(" (").append(miembros.size()).append(" miembros): ");
        if (miembros.isEmpty()) {
            sb.append("sin integrantes");
        } else {
            // Solo nombres simples: llamar a Personaje.toString() aqui
            // dispararia la recursion cruzada Personaje <-> Gremio.
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
